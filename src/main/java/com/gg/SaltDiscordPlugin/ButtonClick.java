package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.discord.DiscordRichPresence;
import com.xuncorp.spw.workshop.api.WorkshopApi;

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

    public static void onReconnectClick() {
        DiscordRichPresence.getInstance().initialize(new DiscordRichPresence.InitializeCallback() {
            @Override
            public void onSuccess() {
                WorkshopApi.ui().toast("Discord Rich Presence 重连成功!", WorkshopApi.Ui.ToastType.Success);
            }

            @Override
            public void onFailure(String errorMessage) {
                WorkshopApi.ui().toast("Discord Rich Presence 重连失败，" + errorMessage, WorkshopApi.Ui.ToastType.Error);
            }
        });
    }
}
