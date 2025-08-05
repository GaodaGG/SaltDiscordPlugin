package com.gg.SaltDiscordPlugin;

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.Extension;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    @Override
    @Nullable
    public String updateLyrics(@NotNull MediaItem mediaItem) {
        DiscordRichPresence.getInstance().setListeningActivity(mediaItem.getTitle(), mediaItem.getArtist(), mediaItem.getAlbum());

        return null;
    }

    @Override
    public void onStateChanged(@NotNull State state) {

    }

    @Override
    public void onIsPlayingChanged(boolean b) {
        DiscordRichPresence.getInstance().updatePlayingState(b);
    }

    @Override
    public void onSeekTo(long l) {

    }

    @Nullable
    @Override
    public String onBeforeLoadLyrics(@NotNull MediaItem mediaItem) {
        return null;
    }

    @Nullable
    @Override
    public String onAfterLoadLyrics(@NotNull MediaItem mediaItem) {
        return null;
    }

    @Override
    public void onLyricsLineUpdated(@Nullable LyricsLine lyricsLine) {

    }
}
