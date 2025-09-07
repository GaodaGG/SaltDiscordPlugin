package com.gg.SaltDiscordPlugin;

import com.xuncorp.spw.workshop.api.WorkshopApi;
import com.xuncorp.spw.workshop.api.config.ConfigHelper;
import com.xuncorp.spw.workshop.api.config.ConfigManager;

import java.nio.file.Files;

/**
 * 配置管理器，用于读取插件配置
 */
public class Config {
    private static Config instance;
    ConfigManager configManager = WorkshopApi.manager().createConfigManager("com.gg.SaltDiscordPlugin");
    ConfigHelper configHelper = configManager.getConfig();
    private final ConfigData configData = new ConfigData();

    private Config() {
        loadConfig();
        configWatcher();
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private void loadConfig() {
        if (Files.notExists(configHelper.getConfigPath())) {
            // 创建默认配置文件
            saveConfig();
            return;
        }

        configHelper.reload();
        configData.displayType = configHelper.get("displayType", "Details");
        configData.disableNetEase = configHelper.get("disableNetEase", false);
        configData.disableKugou = configHelper.get("disableKugou", false);
        configData.disableQQ = configHelper.get("disableQQ", false);
        configData.useCFR2 = configHelper.get("useCFR2", false);
        configData.cfr2BucketName = configHelper.get("cfr2BucketName", "");
        configData.cfr2Endpoint = configHelper.get("cfr2Endpoint", "");
        configData.cfr2PublicUrl = configHelper.get("cfr2PublicUrl", "");
        configData.cfr2AccessKey = configHelper.get("cfr2AccessKey", "");
        configData.cfr2SecretKey = configHelper.get("cfr2SecretKey", "");

        System.out.println("配置文件加载成功");
    }

    public void saveConfig() {
        configHelper.set("displayType", configData.displayType);
        configHelper.set("disableNetEase", configData.disableNetEase);
        configHelper.set("disableKugou", configData.disableKugou);
        configHelper.set("disableQQ", configData.disableQQ);
        configHelper.set("useCFR2", configData.useCFR2);
        configHelper.set("cfr2BucketName", configData.cfr2BucketName);
        configHelper.set("cfr2Endpoint", configData.cfr2Endpoint);
        configHelper.set("cfr2PublicUrl", configData.cfr2PublicUrl);
        configHelper.set("cfr2AccessKey", configData.cfr2AccessKey);
        configHelper.set("cfr2SecretKey", configData.cfr2SecretKey);

        configHelper.save();
    }

    private void configWatcher() {
        configManager.addConfigChangeListener(configHelper -> {
            loadConfig();
            // 通知主扩展重新初始化 R2 服务
            MainPluginExtension.onConfigChanged();
        });
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

    public boolean isDisableNetEase() {
        return configData.disableNetEase;
    }

    public boolean isDisableKugou() {
        return configData.disableKugou;
    }

    public boolean isDisableQQ() {
        return configData.disableQQ;
    }

    public ConfigData getConfigData() {
        return configData;
    }

    public static class ConfigData {
        public String displayType = "Details";
        public boolean disableNetEase = false;
        public boolean disableKugou = false;
        public boolean disableQQ = false;
        public boolean useCFR2 = false;
        public String cfr2BucketName = "";
        public String cfr2Endpoint = "";
        public String cfr2PublicUrl = "";
        public String cfr2SecretKey = "";
        public String cfr2AccessKey = "";
    }
}
