package com.gg.SaltDiscordPlugin.taskbarLyrics;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * TaskbarLyrics HTTP客户端
 * 移植自JavaScript的TaskbarLyricsFetch功能
 */
public class TaskbarLyricsClient {
    private static final String BASE_URL = "http://127.0.0.1:%d/taskbar";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final int port;

    public TaskbarLyricsClient(int betterncmApiPort) {
        this.port = betterncmApiPort - 2; // TaskbarLyricsPort = BETTERNCM_API_PORT - 2

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    /**
     * 发送POST请求到TaskbarLyrics服务器
     * @param path API路径
     * @param params 请求参数
     * @return CompletableFuture<Response>
     */
    public CompletableFuture<Response> fetch(String path, Object params) {
        String url = String.format(BASE_URL, port) + path;
        String jsonBody = gson.toJson(params);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        CompletableFuture<Response> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                future.complete(response);
            }
        });

        return future;
    }

    /**
     * 同步版本的fetch方法
     * @param path API路径
     * @param params 请求参数
     * @return Response
     * @throws IOException 网络异常
     */
    public Response fetchSync(String path, Object params) throws IOException {
        String url = String.format(BASE_URL, port) + path;
        String jsonBody = gson.toJson(params);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        return httpClient.newCall(request).execute();
    }

    /**
     * 关闭HTTP客户端
     */
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        if (httpClient.cache() != null) {
            try {
                httpClient.cache().close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }
}
