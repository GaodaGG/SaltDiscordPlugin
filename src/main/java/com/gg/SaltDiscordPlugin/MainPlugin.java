package com.gg.SaltDiscordPlugin;

import org.pf4j.Plugin;

public class MainPlugin extends Plugin {
    @Override
    public void start() {
        super.start();
        DiscordRichPresence.getInstance().initialize();
    }
}