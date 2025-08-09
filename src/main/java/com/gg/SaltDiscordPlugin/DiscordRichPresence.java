package com.gg.SaltDiscordPlugin;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.GameSDKException;
import de.jcm.discordgamesdk.Result;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.activity.ActivityType;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discord Rich Presence 单例类
 * 用于管理音乐播放器的 Discord 状态显示
 *
 * @author GaodaGG
 * @since 2025-08-05
 */
public class DiscordRichPresence {

    private static final Object lock = new Object();
    // 单例实例
    private static volatile DiscordRichPresence instance;

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
    // 默认配置
    private long clientId;
    // 当前播放状态
    private volatile String currentSong = "";
    private volatile String currentArtist = "";
    private volatile String currentAlbum = "";
    private volatile boolean isPlaying = false;

    private DiscordRichPresence() {

    }

    /**
     * 获取单例实例
     *
     * @return DiscordRichPresence 单例实例
     */
    public static DiscordRichPresence getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DiscordRichPresence();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化 Discord Rich Presence
     *
     * @param clientId Discord 应用程序 ID
     * @return 是否初始化成功
     */
    public synchronized boolean initialize(long clientId) {
        if (initialized.get()) {
            System.out.println("Discord Rich Presence 已经初始化");
            return true;
        }

        this.clientId = clientId;

        CreateParams params = new CreateParams();
        params.setClientID(clientId);
        params.setFlags(CreateParams.Flags.NO_REQUIRE_DISCORD);

        core = new Core(params);
//        core.activityManager().registerSteam(3009140);

        // 启动回调线程
        startCallbackThread();

        initialized.set(true);
        System.out.println("Discord Rich Presence 初始化成功 (Client ID: " + clientId + ")");
        return true;
    }

    /**
     * 使用默认客户端 ID 初始化
     */
    public boolean initialize() {
        // 插件默认的 Discord 客户端 ID
        // https://discord.com/developers/applications 在这里新建应用后下方Copy Application ID
        return initialize(1402153571725873203L);
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

        // 检查是否需要更新
        if (songName.equals(currentSong) &&
                artist.equals(currentArtist) &&
                album.equals(currentAlbum) &&
                playing == isPlaying) {
            return; // 状态未改变，无需更新
        }

        try {
            // 清理之前的 Activity
            if (currentActivity != null) {
                currentActivity.close();
            }

            currentActivity = new Activity();

            // 设置活动类型为 LISTENING
            currentActivity.setType(ActivityType.LISTENING);
            currentActivity.setInstance(true);

            // 处理空值
            String safeSongName = (songName == null || songName.trim().isEmpty()) ? "Unknown Song" : songName.trim();
            String safeArtist = (artist == null || artist.trim().isEmpty()) ? "Unknown Artist" : artist.trim();
            String safeAlbum = (album == null || album.trim().isEmpty()) ? "Unknown Album" : album.trim();

            // 设置歌曲信息
            currentActivity.setDetails(safeSongName);  // 第一行：歌曲名
            currentActivity.setState(safeArtist);  // 第二行：艺术家

            // 设置大图标和悬停文本
            currentActivity.assets().setLargeImage("app_icon");
            currentActivity.assets().setLargeText(safeAlbum);

            // 设置小图标表示播放/暂停状态
            if (playing) {
                currentActivity.assets().setSmallImage("play_icon");
                currentActivity.assets().setSmallText("正在播放");
                // 设置开始时间以显示播放时长
                currentActivity.timestamps().setStart(Instant.now());
            } else {
                currentActivity.assets().setSmallImage("pause_icon");
                currentActivity.assets().setSmallText("已暂停");
                // 暂停时清除时间戳
//                currentActivity.timestamps().setEnd();
            }
            currentActivity.timestamps().setStart(Instant.now());

            // 更新活动
            core.activityManager().updateActivity(currentActivity, result -> {
                if (result == Result.OK) {
                    System.out.println("Discord 状态已更新: " + safeSongName + " - " + safeArtist +
                            " [" + (playing ? "播放中" : "已暂停") + "]");
                } else {
                    System.err.println("更新 Discord 状态失败: " + result);
                }
            });

            // 更新当前状态
            this.currentSong = safeSongName;
            this.currentArtist = safeArtist;
            this.currentAlbum = safeAlbum;
            this.isPlaying = playing;

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

    /**
     * 启动回调线程处理 Discord 事件
     */
    private void startCallbackThread() {
        callbackThread = new Thread(() -> {
            int count = 0;
            while (initialized.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    if (core != null) {
                        core.runCallbacks();
                    }

                    count = 0; // 成功时重置计数
                } catch (GameSDKException e) {
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ex) {

                    }
                    count++;
                    System.err.println("Discord 未启动 或连接失败，尝试重新连接... (" + count + "/10)");

                    if (count >= 10) {
                        System.err.println("Discord 回调错误次数过多，正在关闭...");
                        shutdown();
                        break;
                    }
                }
            }
        });

        callbackThread.setDaemon(true);
        callbackThread.setName("Discord-Callbacks-" + System.currentTimeMillis());
        callbackThread.start();
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
            currentActivity.close();
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
        isPlaying = false;

        System.out.println("Discord Rich Presence 已关闭");
    }
}

