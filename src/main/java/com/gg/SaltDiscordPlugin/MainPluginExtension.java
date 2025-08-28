package com.gg.SaltDiscordPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


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
        discordRichPresence.setListeningActivity(mediaItem.getTitle(), mediaItem.getArtist(), mediaItem.getAlbum());

        try {
            long duration = getDurationSeconds(mediaItem.getPath()) * 1000L;
            discordRichPresence.setSongDuration(duration);
        } catch (Exception e) {
            System.out.println("无法获取歌曲时长: " + e.getMessage());
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
     * 获取音频文件时长（秒）
     */
    private int getDurationSeconds(String audioPath) {
        try {
            File audioFile = new File(audioPath);
            AudioFile f = AudioFileIO.read(audioFile);
            AudioHeader header = f.getAudioHeader();
            return header.getTrackLength();
        } catch (Exception e) {
            System.err.println("读取音频文件失败: " + e.getMessage());
            return 0;
        }
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
                coverUrl = extractAndUploadCover(mediaItem.getPath());
            }

            // 如果从文件提取失败或未启用 CFR2，则使用在线获取
            if (coverUrl == null) {
                // 尝试网易云接口
                coverUrl = fetchCoverFromNetEase(mediaItem);

                // 如果网易云接口失败，尝试酷狗接口
                if (coverUrl == null) {
                    coverUrl = fetchCoverFromKugou(mediaItem);
                }
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
     * 从文件中提取封面并上传到 R2
     */
    private String extractAndUploadCover(String audioFilePath) {
        try {
            // 提取封面
            CoverArtExtractor.CoverArtData coverData = CoverArtExtractor.extractCoverArt(audioFilePath);
            if (coverData == null) {
                System.out.println("文件中没有封面图片，将尝试在线获取");
                return null;
            }

            // 上传到 R2
            String uploadedUrl = r2Service.uploadCoverImage(
                    coverData.getImageData(),
                    coverData.getFileName(),
                    coverData.getMimeType()
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

    /**
     * 使用网易云API在线获取封面
     */
    private String fetchCoverFromNetEase(MediaItem mediaItem) {
        try {
            // 构建搜索URL
            String searchQuery = mediaItem.getTitle() + "-" + mediaItem.getArtist();
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String searchUrl = "https://music.163.com/api/search/get?type=1&offset=0&limit=1&s=" + encodedQuery;

            // 执行搜索请求
            String searchResult = getUrlContent(searchUrl);
            JsonObject searchJson = JsonParser.parseString(searchResult).getAsJsonObject();

            if (!searchJson.has("result") || searchJson.get("result").isJsonNull()) {
                return null;
            }
            JsonObject result = searchJson.getAsJsonObject("result");

            if (!result.has("songs") || result.get("songs").isJsonNull()) {
                return null;
            }
            JsonArray songs = result.getAsJsonArray("songs");

            if (songs.isEmpty()) {
                return null;
            }

            int songId = songs.get(0).getAsJsonObject().get("id").getAsInt();

            // 获取歌曲详细信息
            String songInfoUrl = "https://api.injahow.cn/meting/?type=song&id=" + songId;
            String songInfoResult = getUrlContent(songInfoUrl);
            JsonArray songInfoArray = JsonParser.parseString(songInfoResult).getAsJsonArray();

            if (songInfoArray.isEmpty()) {
                return null;
            }

            JsonObject songInfo = songInfoArray.get(0).getAsJsonObject();
            String coverUrl = songInfo.get("pic").getAsString();
            System.out.println("网易云接口获取封面成功: " + coverUrl);
            return coverUrl;
        } catch (Exception e) {
            System.err.println("网易云接口获取封面失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 使用酷狗API在线获取封面
     */
    private String fetchCoverFromKugou(MediaItem mediaItem) {
        try {
            // 构建搜索URL
            String searchQuery = mediaItem.getTitle() + "-" + mediaItem.getArtist();
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String searchUrl = "http://ioscdn.kugou.com/api/v3/search/song?page=1&pagesize=1&version=7910&keyword=" + encodedQuery;

            // 执行搜索请求
            String searchResult = getUrlContent(searchUrl);
            JsonObject searchJson = JsonParser.parseString(searchResult).getAsJsonObject();

            if (!searchJson.has("data") || searchJson.get("data").isJsonNull()) {
                return null;
            }

            JsonObject data = searchJson.getAsJsonObject("data");
            if (!data.has("info") || data.get("info").isJsonNull()) {
                return null;
            }
            JsonArray info = data.getAsJsonArray("info");

            if (info.isEmpty()) {
                return null;
            }
            JsonObject songInfo = info.get(0).getAsJsonObject();
            String albumId = songInfo.get("album_id").getAsString();

            // 使用album_id获取专辑信息
            String albumUrl = "http://mobilecdn.kugou.com/api/v3/album/song?version=9108&plat=0&pagesize=100&area_code=1&page=1&with_res_tag=1&albumid=" + albumId;
            String albumResult = getUrlContent(albumUrl);
            JsonObject albumJson = JsonParser.parseString(albumResult).getAsJsonObject();

            if (!albumJson.has("data") || albumJson.get("data").isJsonNull()) {
                return null;
            }
            JsonObject albumData = albumJson.getAsJsonObject("data");

            if (!albumData.has("info") || albumData.get("info").isJsonNull()) {
                return null;
            }
            JsonArray albumInfo = albumData.getAsJsonArray("info");

            if (albumInfo.isEmpty()) {
                return null;
            }
            JsonObject album = albumInfo.get(0).getAsJsonObject();

            if (!album.has("union_cover") || album.get("union_cover").isJsonNull()) {
                return null;
            }

            String unionCover = album.get("union_cover").getAsString();

            // 处理union_cover字段
            String coverUrl = unionCover
                    .replace("\\", "")
                    .replace("{size}", "");

            System.out.println("酷狗接口获取封面成功: " + coverUrl);
            return coverUrl;
        } catch (Exception e) {
            System.err.println("酷狗接口获取封面失败: " + e.getMessage());
        }
        return null;
    }

    private String getUrlContent(String url) {
        StringBuilder content = new StringBuilder();
        try {
            URLConnection connection = new URI(url).toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
        } catch (MalformedURLException e) {
            System.err.println("无效的URL: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("读取URL内容失败: " + e.getMessage());
        }
        return content.toString();
    }
}
