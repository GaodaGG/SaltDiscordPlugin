package com.gg.SaltDiscordPlugin;

import com.gg.SaltDiscordPlugin.taskbarLyrics.TaskbarLyricsManager;
import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint;
import okhttp3.Response;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pf4j.Extension;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Extension
public class MainPluginExtension implements PlaybackExtensionPoint {
    private String playingSongPath = "";
    private long currentPositionMs = 0; // 当前播放位置（毫秒）
    private int totalDurationSeconds = 0; // 总时长（秒）

    public static void example(LyricsLine lyricsLine) {
        int betterncmApiPort = 3800;

        // Create TaskbarLyrics manager
        TaskbarLyricsManager manager = new TaskbarLyricsManager(betterncmApiPort);

        // === Lyrics setting example ===

        Map<String, Object> lyricsParams = new HashMap<>();
        lyricsParams.put("basic", lyricsLine.getPureMainText());
        lyricsParams.put("extra", lyricsLine.getPureSubText() == null ? "" : lyricsLine.getPureSubText());

        try {
            Response colorResponse = manager.setLyricsSync(lyricsParams);
            if (colorResponse.isSuccessful()) {
                System.out.println("Lyrics set successfully");
            } else {
                System.out.println("Lyrics setting failed: " + colorResponse.code());
            }
            colorResponse.close();
        } catch (IOException e) {
            System.err.println("Lyrics setting error: " + e.getMessage());
        }

    }

    @Override
    @Nullable
    public String updateLyrics(@NotNull MediaItem mediaItem) {
        playingSongPath = mediaItem.getPath();
        // 新歌曲开始时重置位置
        currentPositionMs = 0;
        totalDurationSeconds = getDurationSeconds(playingSongPath);
        DiscordRichPresence.getInstance().setListeningActivity(
                mediaItem.getTitle(),
                mediaItem.getArtist(),
                mediaItem.getAlbum(),
                calculateEndTimestamp()
        );

        return null;
    }

    /**
     * 获取音频文件时长（秒）
     */
    private int getDurationSeconds(String audioPath) {
        try {
            File audioFile = new File(audioPath);
            AudioFile f = AudioFileIO.read(audioFile);
            AudioHeader header = f.getAudioHeader();
            return header.getTrackLength();
        } catch (Exception e) {
            System.err.println("读取音频文件失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 计算歌曲结束时间戳
     * 基于当前播放位置和剩余时长
     */
    private Instant calculateEndTimestamp() {
        if (totalDurationSeconds <= 0) {
            return Instant.now();
        }

        long remainingSeconds = totalDurationSeconds - (currentPositionMs / 1000);
        if (remainingSeconds <= 0) {
            remainingSeconds = 1; // 至少保留1秒
        }

        return Instant.now().plusSeconds(remainingSeconds);
    }

    @Override
    public void onStateChanged(@NotNull State state) {

    }

    @Override
    public void onIsPlayingChanged(boolean b) {
        DiscordRichPresence.getInstance().updatePlayingState(calculateEndTimestamp(), b);
    }

    @Override
    public void onSeekTo(long positionMs) {
        // 更新当前播放位置
        currentPositionMs = positionMs;
        // 重新计算结束时间并更新Discord状态
        DiscordRichPresence.getInstance().updatePlayingState(calculateEndTimestamp(), true);
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
        if (lyricsLine == null) {
            return; // 如果歌词行为空，直接返回
        }
        example(lyricsLine);
    }
}
