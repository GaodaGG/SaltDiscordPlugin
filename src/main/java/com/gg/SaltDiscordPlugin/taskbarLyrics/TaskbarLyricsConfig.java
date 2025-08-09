package com.gg.SaltDiscordPlugin.taskbarLyrics;

import java.util.HashMap;
import java.util.Map;

/**
 * TaskbarLyrics配置类
 * 移植自JavaScript的defaultConfig和pluginConfig
 */
public class TaskbarLyricsConfig {

    /**
     * 颜色配置
     */
    public static class ColorConfig {
        public int hexColor;
        public double opacity;

        public ColorConfig(int hexColor, double opacity) {
            this.hexColor = hexColor;
            this.opacity = opacity;
        }
    }

    /**
     * 字体粗细配置
     */
    public static class FontWeightConfig {
        public int value;
        public String textContent;

        public FontWeightConfig(int value, String textContent) {
            this.value = value;
            this.textContent = textContent;
        }
    }

    /**
     * 字体配置
     */
    public static class FontConfig {
        public String fontFamily;

        public FontConfig(String fontFamily) {
            this.fontFamily = fontFamily;
        }
    }

    /**
     * 颜色主题配置
     */
    public static class ColorThemeConfig {
        public ColorConfig light;
        public ColorConfig dark;

        public ColorThemeConfig(ColorConfig light, ColorConfig dark) {
            this.light = light;
            this.dark = dark;
        }
    }

    /**
     * 字体样式配置
     */
    public static class FontStyleConfig {
        public FontWeightConfig weight;
        public int slope;
        public boolean underline;
        public boolean strikethrough;

        public FontStyleConfig(FontWeightConfig weight, int slope, boolean underline, boolean strikethrough) {
            this.weight = weight;
            this.slope = slope;
            this.underline = underline;
            this.strikethrough = strikethrough;
        }
    }

    /**
     * 选项配置（带值和文本描述）
     */
    public static class OptionConfig {
        public int value;
        public String textContent;

        public OptionConfig(int value, String textContent) {
            this.value = value;
            this.textContent = textContent;
        }
    }

    /**
     * 边距配置
     */
    public static class MarginConfig {
        public int left;
        public int right;

        public MarginConfig(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }

    /**
     * 屏幕配置
     */
    public static class ScreenConfig {
        public OptionConfig parentTaskbar;

        public ScreenConfig(String value, String textContent) {
            this.parentTaskbar = new OptionConfig(0, textContent); // 使用0作为默认值
            // 实际存储字符串值
            this.parentTaskbar = new OptionConfig(value.hashCode(), textContent);
        }
    }

    /**
     * 获取默认配置
     */
    public static Map<String, Object> getDefaultConfig() {
        Map<String, Object> config = new HashMap<>();

        // 字体配置
        Map<String, Object> font = new HashMap<>();
        font.put("font_family", "Microsoft YaHei UI");
        config.put("font", font);

        // 颜色配置
        Map<String, Object> color = new HashMap<>();
        Map<String, Object> basic = new HashMap<>();
        basic.put("light", new ColorConfig(0x000000, 1.0));
        basic.put("dark", new ColorConfig(0xFFFFFF, 1.0));
        color.put("basic", basic);

        Map<String, Object> extra = new HashMap<>();
        extra.put("light", new ColorConfig(0x000000, 1.0));
        extra.put("dark", new ColorConfig(0xFFFFFF, 1.0));
        color.put("extra", extra);
        config.put("color", color);

        // 样式配置
        Map<String, Object> style = new HashMap<>();
        Map<String, Object> basicStyle = new HashMap<>();
        basicStyle.put("weight", new FontWeightConfig(WindowsEnum.DWriteFontWeight.NORMAL, "Normal (400)"));
        basicStyle.put("slope", WindowsEnum.DWriteFontStyle.NORMAL);
        basicStyle.put("underline", false);
        basicStyle.put("strikethrough", false);
        style.put("basic", basicStyle);

        Map<String, Object> extraStyle = new HashMap<>();
        extraStyle.put("weight", new FontWeightConfig(WindowsEnum.DWriteFontWeight.NORMAL, "Normal (400)"));
        extraStyle.put("slope", WindowsEnum.DWriteFontStyle.NORMAL);
        extraStyle.put("underline", false);
        extraStyle.put("strikethrough", false);
        style.put("extra", extraStyle);
        config.put("style", style);

        // 歌词配置
        Map<String, Object> lyrics = new HashMap<>();
        lyrics.put("retrieval_method", new OptionConfig(1, "使用LibLyric解析获取歌词"));
        config.put("lyrics", lyrics);

        // 效果配置
        Map<String, Object> effect = new HashMap<>();
        effect.put("next_line_lyrics_position", new OptionConfig(0, "副歌词，下句歌词显示在这"));
        effect.put("extra_show", new OptionConfig(2, "当前翻译，没则用上个选项"));
        effect.put("adjust", 0.0);
        config.put("effect", effect);

        // 对齐配置
        Map<String, Object> align = new HashMap<>();
        align.put("basic", WindowsEnum.DWriteTextAlignment.LEADING);
        align.put("extra", WindowsEnum.DWriteTextAlignment.LEADING);
        config.put("align", align);

        // 位置配置
        Map<String, Object> position = new HashMap<>();
        position.put("position", new OptionConfig(WindowsEnum.WindowAlignment.ADAPTIVE, "自动，自适应选择左或右"));
        config.put("position", position);

        // 边距配置
        config.put("margin", new MarginConfig(0, 0));

        // 屏幕配置
        Map<String, Object> screen = new HashMap<>();
        screen.put("parent_taskbar", new OptionConfig(0, "主屏幕任务栏"));
        config.put("screen", screen);

        return config;
    }
}
