package com.gg.SaltDiscordPlugin.discord;

import com.gg.SaltDiscordPlugin.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.LogLevel;
import de.jcm.discordgamesdk.Result;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;
import de.jcm.discordgamesdk.impl.Command;
import de.jcm.discordgamesdk.impl.commands.SetActivity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Discord Rich Presence 单例类
 * 用于管理音乐播放器的 Discord 状态显示
 *
 * @author GaodaGG
 * @since 2025-08-05
 */
public class DiscordRichPresence {
    // 单例实例
    private static DiscordRichPresence instance;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null && instance.isInitialized()) {
                System.out.println("程序退出，正在清理 Discord Rich Presence...");
                instance.shutdown();
            }
        }));
    }

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    // Discord 相关
    private Core core;
    private Activity currentActivity;
    private Thread callbackThread;
    // 当前播放状态
    private volatile String currentSong = "";
    private volatile String currentArtist = "";
    private volatile String currentAlbum = "";
    private volatile boolean isPlaying = false;
    private volatile String currentCoverUrl = ""; // 当前封面URL
    // 播放进度相关
    private volatile long currentPosition = 0; // 当前播放位置（毫秒）
    private volatile long songDuration = 0; // 歌曲总时长（毫秒）
    private volatile Instant playStartTime = null; // 播放开始时间

    private DiscordRichPresence() {

    }

    /**
     * 获取单例实例
     *
     * @return DiscordRichPresence 单例实例
     */
    public static synchronized DiscordRichPresence getInstance() {
        if (instance == null) {
            instance = new DiscordRichPresence();
        }

        return instance;
    }

    /**
     * 初始化 Discord Rich Presence
     *
     * @param clientId Discord 应用程序 ID
     * @param callback 初始化回调
     */
    public synchronized void initialize(long clientId, InitializeCallback callback) {
        if (initialized.get()) {
            callback.onFailure("Discord 已初始化，无需重复初始化。");
            return;
        }

        CreateParams params = new CreateParams();
        params.setClientID(clientId);
        params.setFlags(CreateParams.Flags.NO_REQUIRE_DISCORD);

        core = new Core(params);
        core.setLogHook(LogLevel.ERROR, (level, message) ->
                System.out.printf("[%s] %s%n", level, message));

        // 启动回调线程
        startCallbackThread(callback);

        initialized.set(true);
        System.out.println("Discord Rich Presence 初始化成功 (Client ID: " + clientId + ")");
    }

    /**
     * 使用默认客户端 ID 初始化
     *
     * @param callback 初始化回调
     */
    public void initialize(InitializeCallback callback) {
        // 插件默认的 Discord 客户端 ID
        // https://discord.com/developers/applications 在这里新建应用后下方Copy Application ID
        initialize(1402153571725873203L, callback);
    }

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * 设置正在听音乐的状态
     *
     * @param songName 歌曲名称
     * @param artist   艺术家
     * @param album    专辑名称
     * @param playing  是否正在播放
     */
    public synchronized void setListeningActivity(String songName, String artist, String album, boolean playing) {
        if (!initialized.get() || core == null) {
            System.err.println("Discord Rich Presence 未初始化，请先调用 initialize()");
            return;
        }

        if (!isDiscordRunning()) {
            return;
        }

        // 检查是否需要更新
        if (songName.equals(currentSong) &&
                artist.equals(currentArtist) &&
                album.equals(currentAlbum) &&
                playing == isPlaying) {
            return; // 状态未改变，无需更新
        }

        try {
            // 清理之前的 Activity
            currentActivity = null;

            currentActivity = new Activity();

            // 设置活动类型为 LISTENING
            currentActivity.setType(ActivityType.LISTENING);
            currentActivity.setInstance(true);

            // 处理空值
            String safeSongName = songName.trim().isEmpty() ? "Unknown Song" : songName.trim();
            String safeArtist = (artist == null || artist.trim().isEmpty()) ? "Unknown Artist" : artist.trim();
            String safeAlbum = (album == null || album.trim().isEmpty()) ? "Unknown Album" : album.trim();

            // 设置歌曲信息
            currentActivity.setDetails(safeSongName);  // 第一行：歌曲名
            currentActivity.setState(safeArtist);  // 第二行：艺术家

            // 设置大图标和悬停文本
            // 如果有封面URL，使用封面；否则使用默认图标
            if (currentCoverUrl != null && !currentCoverUrl.trim().isEmpty()) {
                currentActivity.assets().setLargeImage(currentCoverUrl);
            } else {
                currentActivity.assets().setLargeImage("app_icon");
            }
            currentActivity.assets().setLargeText(safeAlbum);

            // 设置小图标表示播放/暂停状态
            if (playing) {
                currentActivity.assets().setSmallImage("play_icon");
                currentActivity.assets().setSmallText("正在播放");
            } else {
                currentActivity.assets().setSmallImage("pause_icon");
                currentActivity.assets().setSmallText("已暂停");
            }

            // 更新当前状态
            this.currentSong = safeSongName;
            this.currentArtist = safeArtist;
            this.currentAlbum = safeAlbum;
            this.isPlaying = playing;

            // 设置时间戳
            updateTimestamps();

            // 更新活动
            updateActivity(result -> {
                if (result == Result.OK) {
                    System.out.println("Discord 状态已更新: " + safeSongName + " - " + safeArtist +
                            " [" + (playing ? "播放中" : "已暂停") + "]");
                } else {
                    System.err.println("更新 Discord 状态失败: " + result);
                }
            });

        } catch (Exception e) {
            System.err.println("设置音乐活动失败: " + e.getMessage());
        }
    }

    /**
     * 设置正在听音乐的状态
     */
    public void setListeningActivity(String songName, String artist, String album) {
        setListeningActivity(songName, artist, album, true);
    }

    /**
     * 更新播放状态
     */
    public void updatePlayingState(boolean playing) {
        if (!currentSong.isEmpty()) {
            setListeningActivity(currentSong, currentArtist, currentAlbum, playing);
        }
    }


    public synchronized void updatePlaybackPosition(long position) {
        this.currentPosition = position;

        // 只有在播放状态下才更新时间戳
        if (isPlaying && currentActivity != null && isDiscordRunning()) {
            updateTimestamps();
        }
    }

    /**
     * 设置歌曲总时长
     *
     * @param duration 歌曲总时长（毫秒）
     */
    public void setSongDuration(long duration) {
        this.songDuration = duration;
    }

    /**
     * 设置封面URL
     *
     * @param coverUrl 封面图片URL
     */
    public void setCoverUrl(String coverUrl) {
        if (coverUrl != null && !coverUrl.equals(this.currentCoverUrl)) {
            this.currentCoverUrl = coverUrl;
            // 如果当前有活动，更新封面
            if (currentActivity != null && !currentSong.isEmpty()) {
                updateActivityWithCover();
            }
        }
    }

    /**
     * 更新当前活动的封面图片
     */
    private void updateActivityWithCover() {
        if (currentActivity == null) {
            return;
        }

        try {
            // 更新大图标
            if (currentCoverUrl != null && !currentCoverUrl.trim().isEmpty()) {
                currentActivity.assets().setLargeImage(currentCoverUrl);
            } else {
                currentActivity.assets().setLargeImage("app_icon");
            }

            // 更新活动
            updateActivity(result -> {
                if (result == Result.OK) {
                    System.out.println("Discord 封面已更新: " + currentCoverUrl);
                } else {
                    System.err.println("更新 Discord 封面失败: " + result);
                }
            });

        } catch (Exception e) {
            System.err.println("更新封面失败: " + e.getMessage());
        }
    }

    /**
     * 更新Discord活动的时间戳以反映当前播放进度
     */
    private void updateTimestamps() {
        if (currentActivity == null) {
            return;
        }

        try {
            Instant now = Instant.now();

            if (isPlaying) {
                // 计算播放开始时间：当前时间 - 已播放时间
                Instant startTime = now.minusMillis(currentPosition);
                currentActivity.timestamps().setStart(startTime);

                // 如果有歌曲总时长，设置结束时间
                if (songDuration > 0) {
                    Instant endTime = startTime.plusMillis(songDuration);
                    currentActivity.timestamps().setEnd(endTime);
                }

                playStartTime = startTime;
            } else {
                // 暂停时清除时间戳
                currentActivity.timestamps().setStart(null);
                currentActivity.timestamps().setEnd(null);
            }

            // 更新活动

            updateActivity(result -> {
                if (result != Result.OK) {
                    System.err.println("更新播放进度失败: " + result);
                }
            });

        } catch (Exception e) {
            System.err.println("更新时间戳失败: " + e.getMessage());
        }
    }

    /**
     * 通过反射调用私有方法更新活动
     */
    private void updateActivity(Consumer<Result> callback) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> coreClazz = core.getClass();

        Field corePrivateField = coreClazz.getDeclaredField("corePrivate");
        Field nonceField = coreClazz.getDeclaredField("nonce");
        Method sendCommand = coreClazz.getDeclaredMethod("sendCommand", Command.class, Consumer.class);

        corePrivateField.setAccessible(true);
        nonceField.setAccessible(true);
        sendCommand.setAccessible(true);

        long nonce = (long) nonceField.get(core);
        Core.CorePrivate corePrivate = (Core.CorePrivate) corePrivateField.get(core);

        SetActivity.Args args = new SetActivity.Args(corePrivate.pid, currentActivity);
        JsonObject asJsonObject = new Gson().toJsonTree(args).getAsJsonObject();
        int ordinal = DisplayType.valueOf(Config.getInstance().getDisplayType()).ordinal();

        asJsonObject.get("activity").getAsJsonObject().addProperty("status_display_type", ordinal); // 服了，就为了这玩意各种反射拿
        Command command = new Command();
        command.setCmd(Command.Type.SET_ACTIVITY);
        command.setArgs(asJsonObject);
        command.setNonce(Long.toString(++nonce));
        sendCommand.invoke(core, command, (Consumer<Command>) c -> callback.accept(corePrivate.checkError(c)));
    }

    /**
     * 启动回调线程处理 Discord 事件
     */
    private void startCallbackThread(InitializeCallback callback) {
        callbackThread = new Thread(() -> {
            int count = 0;
            while (initialized.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (core != null) {
                        core.runCallbacks();
                    }

                    count = 0; // 成功时重置计数
                    callback.onSuccess();
                } catch (Exception e) {
                    if (!e.getMessage().equals("NOT_RUNNING")) {
                        e.printStackTrace();
                        return;
                    }

                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    count++;
                    System.err.println("Discord 未启动 或连接失败，尝试重新连接... (" + count + "/10)");

                    if (count >= 10) {
                        System.err.println("Discord 回调错误次数过多，正在关闭...");
                        shutdown();
                        callback.onFailure("请确保 Discord 已启动。");
                        break;
                    }
                }
            }
        });

        callbackThread.setDaemon(true);
        callbackThread.setName("Discord-Callbacks-" + System.currentTimeMillis());
        callbackThread.start();
    }

    public boolean isDiscordRunning() {
        return core != null && core.isDiscordRunning();
    }

    /**
     * 关闭 Discord Rich Presence
     */
    public synchronized void shutdown() {
        if (!initialized.get()) {
            return;
        }

        initialized.set(false);

        // 停止回调线程
        if (callbackThread != null && callbackThread.isAlive()) {
            callbackThread.interrupt();
            try {
                callbackThread.join(1000); // 等待最多1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 清理资源
        if (currentActivity != null) {
            currentActivity = null;
        }

        if (core != null) {
            core.close();
            core = null;
        }

        // 重置状态
        currentSong = "";
        currentArtist = "";
        currentAlbum = "";
        currentCoverUrl = "";
        isPlaying = false;
        currentPosition = 0;
        songDuration = 0;
        playStartTime = null;

        // 清除单例引用
        instance = null;

        System.out.println("Discord Rich Presence 已关闭");
    }

    public interface InitializeCallback {
        void onSuccess();

        void onFailure(String errorMessage);
    }
}

