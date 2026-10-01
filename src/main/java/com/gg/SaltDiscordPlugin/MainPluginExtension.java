package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.cover.CoverArtExtractor;
import com.gg.SaltDiscordPlugin.cover.CoverFetcher;
import com.gg.SaltDiscordPlugin.discord.DiscordRichPresence;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import com.xuncorp.spw.workshop.api.PluginPermission;
import com.xuncorp.spw.workshop.api.WorkshopApi;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    private static final Config config;
    private static CloudflareR2Service r2Service;

    static {
        config = Config.getInstance();
        initializeR2Service();
    }

    /**
     * 初始化 R2 服务
     */
    private static void initializeR2Service() {
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
                r2Service = null;
            }
        } else {
            r2Service = null;
        }
    }

    /**
     * 配置变更时重新初始化服务
     */
    public static void onConfigChanged() {
        System.out.println("配置已变更，重新初始化 R2 服务");
        if (r2Service != null) {
            r2Service.close();
        }
        initializeR2Service();
    }

    @Override
    public String onBeforeLoadLyrics(@NotNull MediaItem mediaItem) {
        DiscordRichPresence discordRichPresence = DiscordRichPresence.getInstance();
        if (!discordRichPresence.isDiscordRunning()) {
            return null;
        }

        discordRichPresence.setListeningActivity(mediaItem.getTitle(), mediaItem.getArtist(), mediaItem.getAlbum());

        long duration = resolveDurationMs(mediaItem);
        if (duration > 0) {
            discordRichPresence.setSongDuration(duration);
        } else {
            System.out.println("无法获取歌曲时长");
        }

        // 根据配置决定封面获取方式
        fetchCoverArt(mediaItem, discordRichPresence);

        return null;
    }

    @Override
    public void onIsPlayingChanged(boolean b) {
        DiscordRichPresence.getInstance().updatePlayingState(b);
    }


    @Override
    public void onPositionUpdated(long position) {
        // 更新Discord Rich Presence的播放进度
        DiscordRichPresence.getInstance().updatePlaybackPosition(position);
    }

    /**
     * 是否已获得曲库读取权限
     */
    private static boolean isLibraryReadGranted() {
        return WorkshopApi.manager().isPermissionGranted(PluginPermission.LIBRARY_READ);
    }

    /**
     * 获取歌曲时长（毫秒）
     * 通过 dev21 曲库 API 查询完整元数据，未授权或查询失败时返回 0
     */
    private long resolveDurationMs(MediaItem mediaItem) {
        if (!isLibraryReadGranted() || mediaItem.getId().isEmpty()) {
            return 0;
        }

        try {
            MediaItem track = WorkshopApi.library()
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
    private void fetchCoverArt(MediaItem mediaItem, DiscordRichPresence discordRichPresence) {
        // 在新线程中异步获取封面，避免阻塞主线程
        new Thread(() -> {
            String coverUrl = null;

            // 如果启用了 CFR2，优先从文件中提取封面并上传
            if (config.isUseCFR2() && r2Service != null) {
                coverUrl = extractAndUploadCover(mediaItem);
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
     */
    private String extractAndUploadCover(MediaItem mediaItem) {
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
