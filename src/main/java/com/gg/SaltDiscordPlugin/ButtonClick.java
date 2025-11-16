package com.gg.SaltDiscordPlugin;

import com.xuncorp.spw.workshop.api.WorkshopApi;

import java.io.IOException;

@SuppressWarnings("unused")
public class ButtonClick {
    public static void onButtonClick() {
        String url = "https://github.com/GaodaGG/SaltDiscordPlugin/blob/master/CLOUDFLARE_R2_SETUP.md";
        openWebpage(url);
    }

    private static void openWebpage(String url) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", url);
            Runtime.getRuntime().exec(pb.command().toArray(new String[0]));
        } catch (IOException e) {
            WorkshopApi.ui().toast("无法打开链接，请手动访问: " + url, WorkshopApi.Ui.ToastType.Error);
        }
    }
}
