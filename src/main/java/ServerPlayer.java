import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.voice.AudioProvider;

import java.util.HashMap;

public class ServerPlayer {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final AudioProvider provider;
    private final TrackScheduler scheduler;
    private boolean isRunning;

    public ServerPlayer(){
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(playerManager);

        player = playerManager.createPlayer();

        provider = new LavaPlayerAudioProvider(player);

        scheduler = new TrackScheduler(player);

        isRunning = false;

    }

    public AudioPlayerManager getManager(){
        return this.playerManager;
    }
    public AudioPlayer getPlayer(){
        return this.player;
    }
    public AudioProvider getProvider(){
        return this.provider;
    }
    public TrackScheduler getScheduler(){
        return this.scheduler;
    }

}
