package com.gg.SaltDiscordPlugin.taskbarLyrics;

/**
 * Windows枚举常量
 * 移植自JavaScript的WindowsEnum
 */
public class WindowsEnum {

    /**
     * 窗口对齐方式
     */
    public static class WindowAlignment {
        public static final int ADAPTIVE = 0;  // WindowAlignmentAdaptive
        public static final int LEFT = 1;      // WindowAlignmentLeft
        public static final int CENTER = 2;    // WindowAlignmentCenter
        public static final int RIGHT = 3;     // WindowAlignmentRight
    }

    /**
     * DirectWrite文本对齐方式
     */
    public static class DWriteTextAlignment {
        public static final int LEADING = 0;    // DWRITE_TEXT_ALIGNMENT_LEADING
        public static final int TRAILING = 1;   // DWRITE_TEXT_ALIGNMENT_TRAILING
        public static final int CENTER = 2;     // DWRITE_TEXT_ALIGNMENT_CENTER
        public static final int JUSTIFIED = 3;  // DWRITE_TEXT_ALIGNMENT_JUSTIFIED
    }

    /**
     * DirectWrite字体粗细
     */
    public static class DWriteFontWeight {
        public static final int THIN = 100;         // DWRITE_FONT_WEIGHT_THIN
        public static final int EXTRA_LIGHT = 200; // DWRITE_FONT_WEIGHT_EXTRA_LIGHT
        public static final int ULTRA_LIGHT = 200; // DWRITE_FONT_WEIGHT_ULTRA_LIGHT
        public static final int LIGHT = 300;       // DWRITE_FONT_WEIGHT_LIGHT
        public static final int SEMI_LIGHT = 350;  // DWRITE_FONT_WEIGHT_SEMI_LIGHT
        public static final int NORMAL = 400;      // DWRITE_FONT_WEIGHT_NORMAL
        public static final int REGULAR = 400;     // DWRITE_FONT_WEIGHT_REGULAR
        public static final int MEDIUM = 500;      // DWRITE_FONT_WEIGHT_MEDIUM
        public static final int DEMI_BOLD = 600;   // DWRITE_FONT_WEIGHT_DEMI_BOLD
        public static final int SEMI_BOLD = 600;   // DWRITE_FONT_WEIGHT_SEMI_BOLD
        public static final int BOLD = 700;        // DWRITE_FONT_WEIGHT_BOLD
        public static final int EXTRA_BOLD = 800;  // DWRITE_FONT_WEIGHT_EXTRA_BOLD
        public static final int ULTRA_BOLD = 800;  // DWRITE_FONT_WEIGHT_ULTRA_BOLD
        public static final int BLACK = 900;       // DWRITE_FONT_WEIGHT_BLACK
        public static final int HEAVY = 900;       // DWRITE_FONT_WEIGHT_HEAVY
        public static final int EXTRA_BLACK = 950; // DWRITE_FONT_WEIGHT_EXTRA_BLACK
        public static final int ULTRA_BLACK = 950; // DWRITE_FONT_WEIGHT_ULTRA_BLACK
    }

    /**
     * DirectWrite字体样式
     */
    public static class DWriteFontStyle {
        public static final int NORMAL = 0;   // DWRITE_FONT_STYLE_NORMAL
        public static final int OBLIQUE = 1;  // DWRITE_FONT_STYLE_OBLIQUE
        public static final int ITALIC = 2;   // DWRITE_FONT_STYLE_ITALIC
    }
}
