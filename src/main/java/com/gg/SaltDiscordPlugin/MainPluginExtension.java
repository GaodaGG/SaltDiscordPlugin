package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.discord.DiscordRichPresence;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    /**
     * 配置变更时重建 R2 服务
     */
    public static void onConfigChanged() {
        System.out.println("配置已变更，重新初始化 R2 服务");
        Utils.initializeR2Service(Config.getInstance());
    }

    @Override
    public String onBeforeLoadLyrics(@NotNull MediaItem mediaItem) {
        Utils.uploadSongInfo(mediaItem);
        return null;
    }

    @Override
    public void onIsPlayingChanged(boolean b) {
        DiscordRichPresence.getInstance().updatePlayingState(b);
    }


    @Override
    public void onPositionUpdated(long position) {
        // 更新Discord Rich Presence的播放进度
        DiscordRichPresence.getInstance().updatePlaybackPosition(position);
    }

}
