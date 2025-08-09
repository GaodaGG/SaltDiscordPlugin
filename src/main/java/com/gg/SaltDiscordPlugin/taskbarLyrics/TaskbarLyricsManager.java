package com.gg.SaltDiscordPlugin.taskbarLyrics;

import okhttp3.Response;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * TaskbarLyrics管理类
 * 移植自JavaScript的base.js，整合所有TaskbarLyrics功能
 */
public class TaskbarLyricsManager {
    private final TaskbarLyricsClient client;
    private final TaskbarLyricsAPI api;
    private final Map<String, Object> defaultConfig;
    private final int taskbarLyricsPort;

    public TaskbarLyricsManager(int betterncmApiPort) {
        this.taskbarLyricsPort = betterncmApiPort - 2;
        this.client = new TaskbarLyricsClient(betterncmApiPort);
        this.api = new TaskbarLyricsAPI(client);
        this.defaultConfig = TaskbarLyricsConfig.getDefaultConfig();
    }

    public static void start(String taskbarLyricsPath) throws IOException {
        // 启动TaskbarLyrics客户端
        Runtime.getRuntime().exec(taskbarLyricsPath);
    }

    /**
     * 获取TaskbarLyrics端口
     */
    public int getTaskbarLyricsPort() {
        return taskbarLyricsPort;
    }

    /**
     * 获取API实例
     */
    public TaskbarLyricsAPI getAPI() {
        return api;
    }

    /**
     * 获取Windows枚举常量
     */
    public WindowsEnum getWindowsEnum() {
        return new WindowsEnum();
    }

    /**
     * 获取默认配置
     */
    public Map<String, Object> getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 获取指定名称的默认配置
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDefaultConfig(String name) {
        Object config = defaultConfig.get(name);
        if (config instanceof Map) {
            return (Map<String, Object>) config;
        }
        return null;
    }

    // === 便捷方法：字体相关 ===

    /**
     * 设置字体
     */
    public CompletableFuture<Response> setFont(Object params) {
        return api.font.font(params);
    }

    /**
     * 设置字体颜色
     */
    public CompletableFuture<Response> setFontColor(Object params) {
        return api.font.color(params);
    }

    /**
     * 设置字体样式
     */
    public CompletableFuture<Response> setFontStyle(Object params) {
        return api.font.style(params);
    }

    // === 便捷方法：歌词相关 ===

    /**
     * 设置歌词内容
     */
    public CompletableFuture<Response> setLyrics(Object params) {
        return api.lyrics.lyrics(params);
    }

    /**
     * 设置歌词对齐方式
     */
    public CompletableFuture<Response> setLyricsAlign(Object params) {
        return api.lyrics.align(params);
    }

    // === 便捷方法：窗口相关 ===

    /**
     * 设置窗口位置
     */
    public CompletableFuture<Response> setWindowPosition(Object params) {
        return api.window.position(params);
    }

    /**
     * 设置窗口边距
     */
    public CompletableFuture<Response> setWindowMargin(Object params) {
        return api.window.margin(params);
    }

    /**
     * 设置屏幕
     */
    public CompletableFuture<Response> setWindowScreen(Object params) {
        return api.window.screen(params);
    }

    // === 同步版本的便捷方法 ===

    public Response setFontSync(Object params) throws IOException {
        return api.font.fontSync(params);
    }

    public Response setFontColorSync(Object params) throws IOException {
        return api.font.colorSync(params);
    }

    public Response setFontStyleSync(Object params) throws IOException {
        return api.font.styleSync(params);
    }

    public Response setLyricsSync(Object params) throws IOException {
        return api.lyrics.lyricsSync(params);
    }

    public Response setLyricsAlignSync(Object params) throws IOException {
        return api.lyrics.alignSync(params);
    }

    public Response setWindowPositionSync(Object params) throws IOException {
        return api.window.positionSync(params);
    }

    public Response setWindowMarginSync(Object params) throws IOException {
        return api.window.marginSync(params);
    }

    public Response setWindowScreenSync(Object params) throws IOException {
        return api.window.screenSync(params);
    }

    /**
     * 关闭TaskbarLyrics
     */
    public CompletableFuture<Response> close(Object params) {
        return api.close(params);
    }

    /**
     * 关闭TaskbarLyrics (同步版本)
     */
    public Response closeSync(Object params) throws IOException {
        return api.closeSync(params);
    }

    /**
     * 关闭HTTP客户端资源
     */
    public void shutdown() {
        client.close();
    }
}
