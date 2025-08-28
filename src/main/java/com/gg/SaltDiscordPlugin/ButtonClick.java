package com.gg.SaltDiscordPlugin;

import java.io.IOException;

public class ButtonClick {
    public static void onButtonClick() {
        String url = "https://github.com/GaodaGG/SaltDiscordPlugin/blob/master/CLOUDFLARE_R2_SETUP.md";
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", url);
            Runtime.getRuntime().exec(pb.command().toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
