package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.discord.DiscordRichPresence;
import com.xuncorp.spw.workshop.api.PluginContext;
import com.xuncorp.spw.workshop.api.SpwPlugin;
import org.jetbrains.annotations.NotNull;

public class MainPlugin extends SpwPlugin {
    public MainPlugin(@NotNull PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        DiscordRichPresence.getInstance().initialize(new DiscordRichPresence.InitializeCallback() {
            @Override
            public void onSuccess() {
                // do nothing
            }

            @Override
            public void onFailure(String errorMessage) {
                // do nothing
            }
        });
    }

    @Override
    public void stop() {
        DiscordRichPresence.getInstance().shutdown();
    }
}