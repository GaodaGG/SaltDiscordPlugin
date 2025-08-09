package com.gg.SaltDiscordPlugin.taskbarLyrics;

import okhttp3.Response;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * TaskbarLyrics API封装类
 * 移植自JavaScript的TaskbarLyricsAPI功能
 */
public class TaskbarLyricsAPI {
    private final TaskbarLyricsClient client;

//    public TaskbarLyricsAPI(TaskbarLyricsClient client) {
//        this.client = client;
//    }

    /**
     * 字体相关API
     */
    public static class FontAPI {
        private final TaskbarLyricsClient client;

        public FontAPI(TaskbarLyricsClient client) {
            this.client = client;
        }

        /**
         * 设置字体
         */
        public CompletableFuture<Response> font(Object params) {
            return client.fetch("/font/font", params);
        }

        /**
         * 设置字体颜色
         */
        public CompletableFuture<Response> color(Object params) {
            return client.fetch("/font/color", params);
        }

        /**
         * 设置字体样式
         */
        public CompletableFuture<Response> style(Object params) {
            return client.fetch("/font/style", params);
        }

        // 同步版本
        public Response fontSync(Object params) throws IOException {
            return client.fetchSync("/font/font", params);
        }

        public Response colorSync(Object params) throws IOException {
            return client.fetchSync("/font/color", params);
        }

        public Response styleSync(Object params) throws IOException {
            return client.fetchSync("/font/style", params);
        }
    }

    /**
     * 歌词相关API
     */
    public static class LyricsAPI {
        private final TaskbarLyricsClient client;

        public LyricsAPI(TaskbarLyricsClient client) {
            this.client = client;
        }

        /**
         * 设置歌词内容
         */
        public CompletableFuture<Response> lyrics(Object params) {
            return client.fetch("/lyrics/lyrics", params);
        }

        /**
         * 设置歌词对齐方式
         */
        public CompletableFuture<Response> align(Object params) {
            return client.fetch("/lyrics/align", params);
        }

        // 同步版本
        public Response lyricsSync(Object params) throws IOException {
            return client.fetchSync("/lyrics/lyrics", params);
        }

        public Response alignSync(Object params) throws IOException {
            return client.fetchSync("/lyrics/align", params);
        }
    }

    /**
     * 窗口相关API
     */
    public static class WindowAPI {
        private final TaskbarLyricsClient client;

        public WindowAPI(TaskbarLyricsClient client) {
            this.client = client;
        }

        /**
         * 设置窗口位置
         */
        public CompletableFuture<Response> position(Object params) {
            return client.fetch("/window/position", params);
        }

        /**
         * 设置窗口边距
         */
        public CompletableFuture<Response> margin(Object params) {
            return client.fetch("/window/margin", params);
        }

        /**
         * 设置屏幕
         */
        public CompletableFuture<Response> screen(Object params) {
            return client.fetch("/window/screen", params);
        }

        // 同步版本
        public Response positionSync(Object params) throws IOException {
            return client.fetchSync("/window/position", params);
        }

        public Response marginSync(Object params) throws IOException {
            return client.fetchSync("/window/margin", params);
        }

        public Response screenSync(Object params) throws IOException {
            return client.fetchSync("/window/screen", params);
        }
    }

    // API实例
    public final FontAPI font;
    public final LyricsAPI lyrics;
    public final WindowAPI window;

    public TaskbarLyricsAPI(TaskbarLyricsClient client) {
        this.client = client;
        this.font = new FontAPI(client);
        this.lyrics = new LyricsAPI(client);
        this.window = new WindowAPI(client);
    }

    /**
     * 关闭连接
     */
    public CompletableFuture<Response> close(Object params) {
        return client.fetch("/close", params);
    }

    /**
     * 关闭连接 (同步版本)
     */
    public Response closeSync(Object params) throws IOException {
        return client.fetchSync("/close", params);
    }
}
