package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.cover.CoverArtExtractor;
import com.gg.SaltDiscordPlugin.cover.CoverFetcher;
import com.gg.SaltDiscordPlugin.discord.DiscordRichPresence;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import com.xuncorp.spw.workshop.api.PluginPermission;
import com.xuncorp.spw.workshop.api.WorkshopApi;

public class Utils {
    private Utils() {
    }

    // volatile 保证多线程间的可见性；初始化/关闭通过 synchronized 方法串行化
    private static volatile CloudflareR2Service r2Service;

    /**
     * 初始化 R2 服务
     * 重复调用时会先关闭旧实例，可直接用于配置变更后的重建
     */
    public static synchronized void initializeR2Service(Config config) {
        closeR2ServiceInternal();

        if (config.isUseCFR2()) {
            String accessKey = config.getCFR2AccessKey();
            String secretKey = config.getCFR2SecretKey();
            String bucketName = config.getCFR2BucketName();
            String cfc2Endpoint = config.getCFR2Endpoint();
            String publicUrl = config.getCFR2PublicUrl();

            if (!accessKey.isEmpty() && !secretKey.isEmpty() &&
                    !bucketName.isEmpty() && !cfc2Endpoint.isEmpty() && !publicUrl.isEmpty()) {
                try {
                    r2Service = new CloudflareR2Service(accessKey, secretKey, bucketName, cfc2Endpoint, publicUrl);
                    System.out.println("Cloudflare R2 服务初始化成功");
                } catch (Exception e) {
                    System.err.println("初始化 Cloudflare R2 服务失败: " + e.getMessage());
                    r2Service = null;
                }
            } else {
                System.out.println("Cloudflare R2 配置不完整，将使用在线获取封面");
                System.out.println("请确保填写: AccessKey, SecretKey, BucketName, Endpoint, PublicUrl");
            }
        }
    }

    /**
     * 关闭 R2 服务
     */
    public static synchronized void closeR2Service() {
        closeR2ServiceInternal();
    }

    private static void closeR2ServiceInternal() {
        if (r2Service != null) {
            r2Service.close();
            r2Service = null;
        }
    }

    /**
     * 是否已获得曲库读取权限
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLibraryReadGranted() {
        return WorkshopApi.manager().isPermissionGranted(PluginPermission.LIBRARY_READ);
    }

    /**
     * 上传当前正在播放的歌曲信息到 Discord
     * 用于插件启动、Discord 重新初始化等需要立即同步状态的场景
     */
    public static void uploadCurrentSong() {
        if (!isLibraryReadGranted()) {
            System.out.println("未获得曲库读取权限，跳过上传当前歌曲信息");
            return;
        }

        PlaybackExtensionPoint.MediaItem mediaItem = WorkshopApi.playback().getCurrentMediaItem();
        if (mediaItem == null) {
            System.out.println("当前没有正在播放的歌曲");
            return;
        }

        uploadSongInfo(mediaItem);
    }

    /**
     * 上传指定歌曲的完整信息（歌曲信息、时长、封面）到 Discord
     */
    public static void uploadSongInfo(PlaybackExtensionPoint.MediaItem mediaItem) {
        DiscordRichPresence discordRichPresence = DiscordRichPresence.getInstance();
        if (!discordRichPresence.isDiscordRunning()) {
            return;
        }

        discordRichPresence.setListeningActivity(mediaItem.getTitle(), mediaItem.getArtist(), mediaItem.getAlbum());

        long duration = resolveDurationMs(mediaItem);
        if (duration > 0) {
            discordRichPresence.setSongDuration(duration);
        } else {
            System.out.println("无法获取歌曲时长");
        }

        // 根据配置决定封面获取方式
        fetchCoverArt(mediaItem, discordRichPresence, Config.getInstance());
    }

    /**
     * 获取歌曲时长（毫秒）
     * 通过 dev21 曲库 API 查询完整元数据，未授权或查询失败时返回 0
     */
    public static long resolveDurationMs(PlaybackExtensionPoint.MediaItem mediaItem) {
        if (!isLibraryReadGranted() || mediaItem.getId().isEmpty()) {
            return 0;
        }

        try {
            PlaybackExtensionPoint.MediaItem track = WorkshopApi.library()
                    .getTrackById(mediaItem.getId())
                    .toCompletableFuture()
                    .join();
            if (track != null) {
                return track.getDuration();
            }
        } catch (Exception e) {
            System.out.println("曲库查询歌曲时长失败: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 异步获取封面图片
     */
    public static void fetchCoverArt(PlaybackExtensionPoint.MediaItem mediaItem, DiscordRichPresence discordRichPresence, Config config) {
        // 在调用线程先取 r2Service 快照，避免异步线程执行期间配置变更导致实例被替换后仍按旧配置走 R2 分支
        CloudflareR2Service r2ServiceSnapshot = r2Service;

        // 在新线程中异步获取封面，避免阻塞主线程
        new Thread(() -> {
            String coverUrl = null;

            // 如果启用了 CFR2，优先从文件中提取封面并上传
            if (config.isUseCFR2() && r2ServiceSnapshot != null) {
                coverUrl = extractAndUploadCover(mediaItem, r2ServiceSnapshot);
            }

            // 如果从文件提取失败或未启用 CFR2，则使用在线获取
            if (coverUrl == null && !config.isDisableNetEase()) {
                // 尝试网易云接口
                coverUrl = CoverFetcher.fetchCoverFromNetEase(mediaItem);
            }

            if (coverUrl == null && !config.isDisableQQ()) {
                // 如果酷狗接口失败，尝试QQ音乐接口
                coverUrl = CoverFetcher.fetchCoverFromQQ(mediaItem);
            }

            if (coverUrl == null && !config.isDisableKugou()) {
                // 如果网易云接口失败，尝试酷狗接口
                coverUrl = CoverFetcher.fetchCoverFromKugou(mediaItem);
            }

            // 更新封面
            if (coverUrl != null) {
                discordRichPresence.setCoverUrl(coverUrl);
                System.out.println("封面获取成功: " + coverUrl);
            } else {
                discordRichPresence.setCoverUrl("app_icon");
                System.out.println("封面获取失败，将使用默认图标");
            }
        }).start();
    }

    /**
     * 提取歌曲内嵌封面并上传到 R2
     * 通过 dev21 曲库 API 读取封面字节，未授权或无内嵌封面时返回 null（走在线获取）
     *
     * @param r2Service 调用方持有的 R2 服务快照，上传期间若配置变更导致服务被关闭，上传会失败并回退在线获取
     */
    public static String extractAndUploadCover(PlaybackExtensionPoint.MediaItem mediaItem, CloudflareR2Service r2Service) {
        try {
            if (!isLibraryReadGranted() || mediaItem.getId().isEmpty()) {
                return null;
            }

            byte[] coverBytes = WorkshopApi.library()
                    .getCoverById(mediaItem.getId())
                    .toCompletableFuture()
                    .join();

            if (coverBytes == null) {
                System.out.println("文件中没有封面图片，将尝试在线获取");
                return null;
            }

            CoverArtExtractor.CoverArtData coverData =
                    CoverArtExtractor.processImageBytes(coverBytes, CoverArtExtractor.sniffMimeType(coverBytes));
            if (coverData == null) {
                System.out.println("封面图片处理失败，将尝试在线获取");
                return null;
            }

            // 上传到 R2
            String uploadedUrl = r2Service.uploadCoverImage(
                    coverData.imageData(),
                    coverData.fileName(),
                    coverData.mimeType()
            );

            if (uploadedUrl != null) {
                System.out.println("封面上传到 R2 成功: " + uploadedUrl);
                return uploadedUrl;
            } else {
                System.err.println("封面上传到 R2 失败");
                return null;
            }

        } catch (Exception e) {
            System.err.println("提取并上传封面失败: " + e.getMessage());
            return null;
        }
    }
}
