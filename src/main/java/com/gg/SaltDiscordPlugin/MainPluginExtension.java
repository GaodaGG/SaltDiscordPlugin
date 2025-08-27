package com.gg.SaltDiscordPlugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    private volatile String currentCoverUrl = "";

    @Override
    public String updateLyrics(@NotNull MediaItem mediaItem) {
        DiscordRichPresence discordRichPresence = DiscordRichPresence.getInstance();
        discordRichPresence.setListeningActivity(mediaItem.getTitle(), mediaItem.getArtist(), mediaItem.getAlbum());

        try {
            long duration = getDurationSeconds(mediaItem.getPath()) * 1000L;
            discordRichPresence.setSongDuration(duration);
        } catch (Exception e) {
            System.out.println("无法获取歌曲时长: " + e.getMessage());
        }

        // 异步获取封面
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
            try {
                // 构建搜索URL
                String searchQuery = mediaItem.getTitle() + "-" + mediaItem.getArtist();
                String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
                String searchUrl = "https://music.163.com/api/search/get?type=1&offset=0&limit=1&s=" + encodedQuery;

                // 执行搜索请求
                String searchResult = getUrlContent(searchUrl);
                JsonObject searchJson = JsonParser.parseString(searchResult).getAsJsonObject();

                if (searchJson.has("result") && !searchJson.get("result").isJsonNull()) {
                    JsonObject result = searchJson.getAsJsonObject("result");
                    if (result.has("songs") && !result.get("songs").isJsonNull()) {
                        JsonArray songs = result.getAsJsonArray("songs");

                        if (songs.size() > 0) {
                            int songId = songs.get(0).getAsJsonObject().get("id").getAsInt();

                            // 获取歌曲详细信息
                            String songInfoUrl = "https://api.injahow.cn/meting/?type=song&id=" + songId;
                            String songInfoResult = getUrlContent(songInfoUrl);
                            JsonArray songInfoArray = JsonParser.parseString(songInfoResult).getAsJsonArray();

                            if (songInfoArray.size() > 0) {
                                JsonObject songInfo = songInfoArray.get(0).getAsJsonObject();
                                String coverUrl = songInfo.get("pic").getAsString();
                                currentCoverUrl = coverUrl;

                                // 更新Discord Rich Presence的封面
                                discordRichPresence.setCoverUrl(coverUrl);
                                System.out.println("获取封面成功: coverUrl=" + coverUrl);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("获取封面失败: " + e.getMessage());
            }
        }).start();
    }

    private String getUrlContent(String url) {
        StringBuilder content = new StringBuilder();
        try {
            URLConnection connection = new URL(url).openConnection();
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
