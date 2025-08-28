package com.gg.SaltDiscordPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.jetbrains.annotations.NotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 配置管理器，用于读取插件配置
 */
public class Config {
    private static final String CONFIG_FILE = "config.json";
    private static volatile Config instance;
    private ConfigData configData;

    private Config() {
        loadConfig();
        new Thread(this::configWatcher).start();
    }

    public static Config getInstance() {
        if (instance == null) {
            synchronized (Config.class) {
                if (instance == null) {
                    instance = new Config();
                }
            }
        }
        return instance;
    }

    @NotNull
    private static Path getConfigPath() {
        return Path.of(System.getenv("APPDATA") + "/Salt Player for Windows/workshop/data/Discord 丰富状态/", CONFIG_FILE);
    }

    private void loadConfig() {
        Path configPath = getConfigPath();
        Gson gson = new Gson();

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                configData = gson.fromJson(reader, ConfigData.class);
                System.out.println("配置文件加载成功");
            } catch (IOException e) {
                System.err.println("读取配置文件失败: " + e.getMessage());
                configData = new ConfigData();
            }
        } else {
            // 创建默认配置文件
            configData = new ConfigData();
            saveConfig();
        }
    }

    public void saveConfig() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Path configPath = getConfigPath();
        if (configPath.getParent() != null) {
            try {
                Files.createDirectories(configPath.getParent());
            } catch (IOException e) {
                System.err.println("创建配置目录失败: " + e.getMessage());
                return;
            }
        }
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(configData, writer);
            System.out.println("配置文件保存成功");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    private void configWatcher() {
        try {
            Path configPath = getConfigPath().getParent();
            WatchService watcher = FileSystems.getDefault().newWatchService();
            configPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    if (fileName.toString().equals(CONFIG_FILE)) {
                        System.out.println("配置文件已修改，重新加载配置");
                        loadConfig();
                        // 通知主扩展重新初始化 R2 服务
                        MainPluginExtension.onConfigChanged();
                    }
                }

                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("配置文件监听失败: " + e.getMessage());
        }
    }

    // Cloudflare R2 相关配置获取方法
    public boolean isUseCFR2() {
        return configData.useCFR2;
    }

    public String getCFR2BucketName() {
        return configData.cfr2BucketName;
    }

    public String getCFR2Endpoint() {
        return configData.cfr2Endpoint;
    }

    public String getCFR2PublicUrl() {
        return configData.cfr2PublicUrl;
    }

    public String getCFR2AccessKey() {
        return configData.cfr2AccessKey;
    }

    public String getCFR2SecretKey() {
        return configData.cfr2SecretKey;
    }

    public String getDisplayType() {
        return configData.displayType;
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public static class ConfigData {
        public String displayType = "Name";
        public boolean useCFR2 = false;
        public String cfr2BucketName = "";
        public String cfr2Endpoint = "";
        public String cfr2PublicUrl = "";
        public String cfr2SecretKey = "";
        public String cfr2AccessKey = "";
    }
}
