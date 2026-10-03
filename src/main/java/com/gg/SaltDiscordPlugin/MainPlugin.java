package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.discord.DiscordRichPresence;
import com.xuncorp.spw.workshop.api.PluginContext;
import com.xuncorp.spw.workshop.api.SpwPlugin;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainPlugin extends SpwPlugin {
    private final AtomicBoolean startupSongUploaded = new AtomicBoolean(false);

    public MainPlugin(@NotNull PluginContext pluginContext) {
        super(pluginContext);
    }

    @Override
    public void start() {
        Utils.initializeR2Service(Config.getInstance());

        DiscordRichPresence.getInstance().initialize(new DiscordRichPresence.InitializeCallback() {
            @Override
            public void onSuccess() {
                if (!startupSongUploaded.compareAndSet(false, true)) {
                    return;
                }

                // 异步执行，避免阻塞 Discord 回调线程（内部含曲库阻塞查询与网络请求）
                new Thread(Utils::uploadCurrentSong).start();
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
        Utils.closeR2Service();
    }
}
