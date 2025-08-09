package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.taskbarLyrics.TaskbarLyricsAPI;
import com.gg.SaltDiscordPlugin.taskbarLyrics.TaskbarLyricsManager;
import com.gg.SaltDiscordPlugin.taskbarLyrics.WindowsEnum;
import okhttp3.Response;
import org.pf4j.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainPlugin extends Plugin {
    @Override
    public void start() {
        super.start();
        DiscordRichPresence.getInstance().initialize();
        try {
            TaskbarLyricsManager manager = new TaskbarLyricsManager(3800);
            // Set font family
            Map<String, Object> fontParams = new HashMap<>();
            fontParams.put("font_family", "Microsoft YaHei UI");

            // Async call
            CompletableFuture<Response> fontFuture = manager.setFont(fontParams);
            fontFuture.thenAccept(response -> {
                if (response.isSuccessful()) {
                    System.out.println("Font set successfully");
                } else {
                    System.out.println("Font setting failed: " + response.code());
                }
                response.close();
            }).exceptionally(throwable -> {
                System.err.println("Font setting error: " + throwable.getMessage());
                return null;
            });

            // === Color setting example ===

            Map<String, Object> colorParams = new HashMap<>();
            Map<String, Object> basicColor = new HashMap<>();
            Map<String, Object> lightColor = new HashMap<>();
            lightColor.put("hex_color", 0x000000);
            lightColor.put("opacity", 1.0);
            basicColor.put("light", lightColor);
            colorParams.put("basic", basicColor);

            // === Window position setting example ===

            Map<String, Object> positionParams = new HashMap<>();
            Map<String, Object> position = new HashMap<>();
            position.put("value", WindowsEnum.WindowAlignment.CENTER);
            position.put("textContent", "Center alignment");
            positionParams.put("position", position);

            manager.setWindowPosition(positionParams)
                    .thenAccept(response -> {
                        if (response.isSuccessful()) {
                            System.out.println("Window position set successfully");
                        }
                        response.close();
                    });

            // === Using default configuration ===

            Map<String, Object> defaultFontConfig = manager.getDefaultConfig("font");
            System.out.println("Default font config: " + defaultFontConfig);

            // === Direct API usage ===

            TaskbarLyricsAPI api = manager.getAPI();

            // Set font style
            Map<String, Object> styleParams = new HashMap<>();
            Map<String, Object> basicStyle = new HashMap<>();
            Map<String, Object> weight = new HashMap<>();
            weight.put("value", WindowsEnum.DWriteFontWeight.BOLD);
            weight.put("textContent", "Bold (700)");
            basicStyle.put("weight", weight);
            basicStyle.put("slope", WindowsEnum.DWriteFontStyle.NORMAL);
            basicStyle.put("underline", false);
            basicStyle.put("strikethrough", false);
            styleParams.put("basic", basicStyle);

            api.font.style(styleParams)
                    .thenAccept(response -> {
                        if (response.isSuccessful()) {
                            System.out.println("Font style set successfully");
                        }
                        response.close();
                    });

        } catch (Exception e) {
            System.err.println("Error during plugin start: " + e.getMessage());
        }

//        TaskbarLyricsManager.start(WorkshopPluginManager.DEFAULT_PLUGINS_DIR + );
    }
}