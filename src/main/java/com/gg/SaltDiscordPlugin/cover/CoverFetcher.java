package com.gg.SaltDiscordPlugin.cover;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;

import java.io.BufferedReader;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CoverFetcher {

    /**
     * 使用网易云API在线获取封面
     */
    public static String fetchCoverFromNetEase(PlaybackExtensionPoint.MediaItem mediaItem) {
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
    public static String fetchCoverFromKugou(PlaybackExtensionPoint.MediaItem mediaItem) {
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
            // 删除头尾的 <!--KG_TAG_RES_START--> 和 <!--KG_TAG_RES_END-->
            albumResult = albumResult.replace("<!--KG_TAG_RES_START-->", "").replace("<!--KG_TAG_RES_END-->", "");
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
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 使用QQ音乐API在线获取封面
     */
    public static String fetchCoverFromQQ(PlaybackExtensionPoint.MediaItem mediaItem) {
        try {
            // 构建搜索URL
            String searchQuery = mediaItem.getTitle() + " " + mediaItem.getArtist();
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String searchUrl = "https://c.y.qq.com/soso/fcgi-bin/music_search_new_platform?format=json&p=1&n=1&w=" + encodedQuery;

            // 执行搜索请求
            String searchResult = getUrlContent(searchUrl);
            JsonObject searchJson = JsonParser.parseString(searchResult).getAsJsonObject();

            if (!searchJson.has("data") || searchJson.get("data").isJsonNull()) {
                return null;
            }

            JsonObject data = searchJson.getAsJsonObject("data");
            if (!data.has("song") || data.get("song").isJsonNull()) {
                return null;
            }

            JsonObject song = data.getAsJsonObject("song");
            if (!song.has("list") || song.get("list").isJsonNull()) {
                return null;
            }

            JsonArray list = song.getAsJsonArray("list");
            if (list.isEmpty()) {
                return null;
            }

            // 获取歌曲ID
            JsonObject firstSong = list.get(0).getAsJsonObject();
            String f = firstSong.get("f").getAsString();
            String[] fParts = f.split("\\|");
            if (fParts[0].isEmpty()) {
                return null;
            }
            String songId = fParts[0];

            // 获取歌曲详情
            String detailUrl = "https://c.y.qq.com/v8/fcg-bin/fcg_play_single_song.fcg?tpl=yqq_song_detail&format=json&songid=" + songId;
            String detailResult = getUrlContent(detailUrl);
            JsonObject detailJson = JsonParser.parseString(detailResult).getAsJsonObject();

            if (detailJson == null || detailJson.isJsonNull() || !detailJson.has("data") || detailJson.get("data").isJsonNull()) {
                return null;
            }

            JsonArray dataArray = detailJson.getAsJsonArray("data");
            if (dataArray.isEmpty()) {
                return null;
            }

            JsonObject songData = dataArray.get(0).getAsJsonObject();
            if (!songData.has("album") || songData.get("album").isJsonNull()) {
                return null;
            }

            JsonObject album = songData.getAsJsonObject("album");
            if (!album.has("mid") || album.get("mid").isJsonNull()) {
                return null;
            }

            String pmid = album.get("mid").getAsString();

            // 构建封面URL
            String coverUrl = "https://y.qq.com/music/photo_new/T002R300x300M000" + pmid + ".jpg?max_age=2592000";

            System.out.println("QQ音乐接口获取封面成功: " + coverUrl);
            return coverUrl;
        } catch (Exception e) {
            System.err.println("QQ音乐接口获取封面失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static String getUrlContent(String url) {
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
