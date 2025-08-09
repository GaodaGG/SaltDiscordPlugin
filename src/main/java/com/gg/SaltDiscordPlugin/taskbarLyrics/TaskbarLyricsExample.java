package com.gg.SaltDiscordPlugin.taskbarLyrics;

import okhttp3.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * TaskbarLyrics Usage Example
 * Demonstrates how to use the ported HTTP communication functionality
 */
public class TaskbarLyricsExample {

    /**
     * Usage example
     */
    public static void example() {
        // Assume BETTERNCM_API_PORT is 3800
        int betterncmApiPort = 3800;

        // Create TaskbarLyrics manager
        TaskbarLyricsManager manager = new TaskbarLyricsManager(betterncmApiPort);

        try {
            // === Font setting example ===

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

            // Sync call
            try {
                Response colorResponse = manager.setFontColorSync(colorParams);
                if (colorResponse.isSuccessful()) {
                    System.out.println("Color set successfully");
                } else {
                    System.out.println("Color setting failed: " + colorResponse.code());
                }
                colorResponse.close();
            } catch (IOException e) {
                System.err.println("Color setting error: " + e.getMessage());
            }

            // === Lyrics setting example ===

            Map<String, Object> lyricsParams = new HashMap<>();
            lyricsParams.put("basic", "Current lyrics content");
            lyricsParams.put("extra", "Next line lyrics content");

            try {
                Response colorResponse = manager.setLyricsSync(lyricsParams);
                if (colorResponse.isSuccessful()) {
                    System.out.println("Lyrics set successfully");
                } else {
                    System.out.println("Lyrics setting failed: " + colorResponse.code());
                }
                colorResponse.close();
            } catch (IOException e) {
                System.err.println("Lyrics setting error: " + e.getMessage());
            }
//                .thenAccept(response -> {
//                    if (response.isSuccessful()) {
//                        System.out.println("Lyrics set successfully");
//                    }
//                    response.close();
//                })
//                .exceptionally(throwable -> {
//                    System.err.println("Lyrics setting error: " + throwable.getMessage());
//                    return null;
//                });

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

            // Wait for async operations to complete
            Thread.sleep(2000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted: " + e.getMessage());
        } finally {
            // Close resources
            manager.shutdown();
        }
    }

    /**
     * Demonstrates how to handle configuration
     */
    public static void configExample() {
        // Get default configuration
        Map<String, Object> defaultConfig = TaskbarLyricsConfig.getDefaultConfig();

        System.out.println("=== Default Configuration ===");
        defaultConfig.forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });

        // Demonstrate usage of Windows enums
        System.out.println("\n=== Windows Enum Constants ===");
        System.out.println("Window Alignment - Center: " + WindowsEnum.WindowAlignment.CENTER);
        System.out.println("Text Alignment - Center: " + WindowsEnum.DWriteTextAlignment.CENTER);
        System.out.println("Font Weight - Bold: " + WindowsEnum.DWriteFontWeight.BOLD);
        System.out.println("Font Style - Italic: " + WindowsEnum.DWriteFontStyle.ITALIC);
    }

    public static void main(String[] args) {
        System.out.println("TaskbarLyrics Java HTTP Client Example");

        // Run configuration example
//        configExample();

        // Run HTTP communication example
         example(); // Uncomment to run actual HTTP request example
    }
}
