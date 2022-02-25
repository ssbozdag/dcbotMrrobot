import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.DisconnectEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.Invite;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.presence.Presence;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import discord4j.rest.util.PermissionSet;
import discord4j.voice.*;
import org.apache.commons.io.IOUtils;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import java.io.*;


public class GoBot {

    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<Guild, ServerPlayer> playerManagerMap = new HashMap<>();
    static final Stack<Long> songtimesforsomefilters = new Stack<>();
    //private static MessageCreateEvent g;

    public static void alter1(){
        //---------------Utilities-----------------------
        //Booleans
        AtomicBoolean inInterval = new AtomicBoolean(false);
        AtomicBoolean engaged = new AtomicBoolean(false);
        AtomicBoolean running = new AtomicBoolean(false);

        //LISTS - STACKS
        List<Message> messages = new ArrayList<>();
        Stack<Message> messageStack = new Stack<>();



        //Threads
        AtomicReference<Thread> th1 = new AtomicReference<>();
        AtomicReference<Thread> timeoutdisconnectThread = new AtomicReference<>();
        AtomicReference<Thread> turnThread = new AtomicReference<>();







        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        // This is an optimization strategy that Discord4J can utilize.
        // It is not important to understand
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        // Allow playerManager to parse remote sources like YouTube links
        AudioSourceManagers.registerRemoteSources(playerManager);

        // Create an AudioPlayer so Discord4J can receive audio data
        final AudioPlayer player = playerManager.createPlayer();

        // We will be creating LavaPlayerAudioProvider in the next step
        AudioProvider provider = new LavaPlayerAudioProvider(player);


        //CREATING CLIENT
        final GatewayDiscordClient client = DiscordClientBuilder.create("ODg4MTUyMjkwMjk5NjA5MDk4.YUOiRw.7uuiSaRQai1SMow1hzKuNjGb8Vc").build()
                .login()
                .block();



        //PREFIX
        client.getEventDispatcher().on(MessageCreateEvent.class)
                // subscribe is like block, in that it will *request* for action
                // to be done, but instead of blocking the thread, waiting for it
                // to finish, it will just execute the results asynchronously.
                .subscribe(event -> {
                    // 3.1 Message.getContent() is a String
                    final String content = event.getMessage().getContent();

                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        // We will be using ! as our "prefix" to any command in the system.
                        if (content.startsWith('!' + entry.getKey() /*+ (entry.getKey().equals(content)? "" : " ")*/) && (!engaged.get() || event.getMember().get().getId().equals(Snowflake.of("563399085504200705")))) {
                            entry.getValue().execute(event);
                            break;
                        }
                    }
                });









        //PING PONG
        commands.put("ping", event -> {
            try {
                event.getMessage()
                        .getChannel().block()
                        .createMessage("Pong!\n yeni surum ile birden fazla sunucu destekliyoruz.").block();
            }catch (Exception e){
                System.out.println("ping pong yaparken bi öldü.");
            }

        });









        //JOIN
        commands.put("join", event -> {
            try{
                Guild guild = event.getGuild().block();
                if(!playerManagerMap.containsKey(guild))
                    playerManagerMap.put(guild,new ServerPlayer());

                ServerPlayer serverPlayer = playerManagerMap.get(guild);
                final Member member = event.getMember().orElse(null);
                if (member != null) {
                    final VoiceState voiceState = member.getVoiceState().block();
                    if (voiceState != null) {
                        final VoiceChannel channel = voiceState.getChannel().block();
                        if (channel != null) {
                            // join returns a VoiceConnection which would be required if we were
                            // adding disconnection features, but for now we are just ignoring it.
                            channel.join(spec -> spec.setProvider(serverPlayer.getProvider())).block();
                            Message message = event.getMessage();
                            Objects.requireNonNull(message.getChannel().block())
                                    .createMessage("kalitenin bir numarali adresine hojgeldiniz!\n !help yazarak komutlara ulasabilirsiniz").block();
                            playerManagerMap.get(guild).getManager().loadItem("https://www.youtube.com/watch?v=XC9N_-UX--4",playerManagerMap.get(guild).getScheduler());
                        }
                    }
                }
            }catch (Exception e){
                System.out.println("join yaparken bi öldü.");
            }

        });










        //HELP
        commands.put("help",event -> {
            try{
                Message message = event.getMessage();
                StringBuilder sb = new StringBuilder();
                int index = 1;
                for(String command : commands.keySet())
                    sb.append(index++)
                            .append(") ")
                            .append(command)
                            .append("\n");
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage(sb.toString()).block();
            }catch (Exception e){
                System.out.println("help yaparken bi öldü.");
            }

        });








        //PLAY
        final TrackScheduler scheduler = new TrackScheduler(player);
        commands.put("play", event -> {
            try{
                Guild guild = event.getMessage().getGuild().block();
                if (!playerManagerMap.containsKey(guild))
                    playerManagerMap.put(guild, new ServerPlayer());


                final String content = event.getMessage().getContent();
                final List<String> command = Arrays.asList(content.replace("!" + "play", "").replace(" ", ""));

                String cmd = "ytsearch: " + command.get(0).replaceFirst(" ","");
                //her servere özel işlemler
                ServerPlayer serverPlayer = playerManagerMap.get(guild);
                serverPlayer.getManager().loadItem(cmd, serverPlayer.getScheduler());
                System.out.println("playerManagerMap.size() == " + playerManagerMap.size());
                System.out.println(guild.getName() + " sunucusunda play komutu kullanildi player : " + serverPlayer.getPlayer() +"\n" +
                        "scheduler.player = "+ serverPlayer.getScheduler().player + "\n" +
                        "scheduler = " + serverPlayer.getScheduler() + "\n" +
                        "yeni stack size = " + serverPlayer.getScheduler().audioPlayStack.size());
                //playerManager.loadItem("ytsearch: " + command.get(0).replaceFirst(" ",""), scheduler);
            }catch (Exception e){
                System.out.println("play yaparken bi öldü.");
            }

        });







        //STOP
        commands.put("stop", event -> {
                    try{
                        Message message = event.getMessage();
                        playerManagerMap.getOrDefault(message.getGuild().block(), new ServerPlayer()).getPlayer().setPaused(true);
                        //player.setPaused(true);
                        Objects.requireNonNull(message.getChannel().block())
                                .createMessage("durdu").block();
                    }catch (Exception e){
                        System.out.println("stop yaparken bi öldü.");
                    }

                }
        );








        //RESUME
        commands.put("resume", event ->{
            try{
                Message message = event.getMessage();
                playerManagerMap.getOrDefault(message.getGuild().block(), new ServerPlayer()).getPlayer().setPaused(false);
                //player.setPaused(false);
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("devam ediyor").block();
            }catch (Exception e){
                System.out.println("resume yaparken bi öldü.");
            }

        });








        //SKIP
        commands.put("skip", event -> {
            try{
                commands.get("closeinterval").execute(event);

                Message message = event.getMessage();
                playerManagerMap.getOrDefault(message.getGuild().block(), new ServerPlayer()).getPlayer().stopTrack();
                //player.stopTrack();
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("skiplendi").block();
            }catch (Exception e){
                System.out.println("skip yaparken bi öldü.");
            }


        });








        //DESTROY
        commands.put("destroy", event -> {
            try{
                Message message = event.getMessage();
                ServerPlayer serverPlayer = playerManagerMap.get(message.getGuild().block());

                commands.get("closeinterval").execute(event);

                serverPlayer.getScheduler().audioPlayStack.clear();
                //scheduler.audioPlayStack.clear();
                //player.stopTrack();
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("liste silindi").block();
            }catch (Exception e){
                System.out.println("destroy yaparken bi öldü.");
            }


        });







        //POSITION
        commands.put("position", event -> {
            try{
                Message message = event.getMessage();

                final String content = message.getContent();
                final List<String> command = Arrays.asList(content.replace("!"+"position","").replace(" ",""));

                long time = Long.parseLong(command.get(0));
                playerManagerMap.getOrDefault(message.getGuild().block(), new ServerPlayer()).getPlayer().getPlayingTrack().setPosition(time * 1000);
                //* player.getPlayingTrack().setPosition(time * 1000);
                //scheduler.setLooped(true);


                Objects.requireNonNull(message.getChannel().block())
                        .createMessage((time / 60) + ":" + (time % 60) + ". saniyeye teleport oldu.").block();
            }catch (Exception e){
                System.out.println("position yaparken bi öldü.");
            }

        });







        //LOOP
        commands.put("loop", event -> {
            try{
                Message message = event.getMessage();
                ServerPlayer serverPlayer = playerManagerMap.getOrDefault(message.getGuild().block(), new ServerPlayer());
                TrackScheduler trackScheduler = serverPlayer.getScheduler();
                trackScheduler.setLooped(!trackScheduler.isLooped());
                //scheduler.setLooped(!scheduler.isLooped());

                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("loop " + (trackScheduler.isLooped() ? "etkinlestirildi." : "kaldirildi")).block();
            }catch (Exception e){
                System.out.println("loop yaparken bi öldü");
            }

        });







        //INTERVAL
        commands.put("setinterval", event -> {
            Guild guild = event.getMessage().getGuild().block();
            AudioPlayer splayer = playerManagerMap.getOrDefault(guild,new ServerPlayer()).getPlayer();

            if (running.get()){
                commands.get("closeinterval").execute(event);
                try{
                    Thread.sleep(1000);
                }catch(Exception ignored){}
            }

            running.set(true);
            Message message = event.getMessage();
            Objects.requireNonNull(message.getChannel().block())
                    .createMessage("interval acildi.").block();

            FutureTask<Void> futureTask = new FutureTask<Void>(()->{
                while (running.get()){
                    final String content = event.getMessage().getContent();
                    final List<String> command = Arrays.asList(content.replace("!" + "setinterval","").split(" "));
                    for (String cmd : command)
                        System.out.println(cmd);
                    final long[] time = {splayer.getPlayingTrack().getPosition()};
                    long low = Long.parseLong(command.get(1))*1000;
                    long high = Long.parseLong(command.get(2))*1000;

                    Message message1 = event.getMessage();
                    Objects.requireNonNull(message1.getChannel().block())
                            .createMessage("sayi 1 : " + low + "\n" + "sayi 2 : " + high).block();
                    while(time[0] >= low && time[0] <= high){
                        if (!running.get())
                            return null;
                        time[0] = splayer.getPlayingTrack().getPosition();
                        //System.out.print(time[0] + "\r");
                    }

                    if (!running.get())
                        return null;
                    splayer.getPlayingTrack().setPosition(low);
                    if (!running.get())
                        return null;
                    while(time[0] != low){
                        if (!running.get())
                            return null;
                        time[0] = splayer.getPlayingTrack().getPosition();
                        try{
                            Thread.sleep(1000);
                        }catch (Exception ignored){

                        }
                    }
                    Objects.requireNonNull(message.getChannel().block())
                            .createMessage("time : " + time[0]).block();
                    try{
                        Thread.sleep(500);
                    }catch (Exception ignored){

                    }


                }


                //commands.get("setinterval").execute(event);
                return null;
            });

           /*try{
                th1.get().join();
            }catch (Exception ignored){}*/
            th1.set(new Thread(futureTask));
            th1.get().start();





        });






        //CLOSEINTERVAL
        commands.put("closeinterval", event -> {
            try{
                running.set(false);
                Message message = event.getMessage();
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("interval kapatildi.").block();
            }catch (Exception e){
                System.out.println("closeinterval yaparken bi öldü.");
            }

        });






        //SETVOLUME
        commands.put("setvolume",event -> {
            try{
                final String content = event.getMessage().getContent();
                final List<String> command = Arrays.asList(content.replace("!" + "setvolume","").replace(" ",""));
                //player.setVolume(Integer.parseInt(command.get(0)));
                Message message = event.getMessage();
                playerManagerMap.getOrDefault(message.getGuild().block(),new ServerPlayer()).getPlayer().setVolume(Integer.parseInt(command.get(0)));
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("ses seviyesi " + command.get(0) + " olarak ayarlandi").block();
            }catch (Exception e){
                System.out.println("setvolum yaparken bi öldü.");
            }

        });







        //CLEAR
        commands.put("clear", event -> {
            try{
                final String content = event.getMessage().getContent();
                final List<String> command = Arrays.asList(content.replace("!" + "clear","").replace(" ",""));


                for(int i = 0; i < Integer.parseInt(command.get(0)); i++){
                    messageStack.pop().delete().subscribe().dispose();
                    /*Message message = event.getMessage();
                    long msg_id = message.getId().asLong();
                    long msg_chnl = message.getChannelId().asLong();
                    ShardInfo shardInfo = event.getShardInfo();
                    long guild_id = event.getGuildId().get().asLong();
                    //event.getMessage().delete();
                    MessageDeleteEvent mesdel = new MessageDeleteEvent(client,
                            shardInfo,
                            msg_id,
                            msg_chnl ,
                            guild_id,
                            message
                    );
                    event.getMessage().delete().subscribe().dispose();*/
                    //mesdel.getMessage().get().getChannel().block().getLastMessage().block().delete().block();
                    //System.out.println(Objects.requireNonNull(Objects.requireNonNull(message.getChannel().block()).getLastMessage().cache().block()).delete().cache().block());
                    //Objects.requireNonNull(Objects.requireNonNull(message.getChannel().block()).getLastMessage().block()).delete().block();
                    // mesdel.getMessage().orElse(event.getMessage()).delete().block();
                    //event.getMessage().delete().flux().subscribe().dispose();
                    //System.out.println();
                    //event.getMessage().delete();
                }
            }catch (Exception e){
                System.out.println("clear yaparken bi öldü.");
            }

        });







        //GETTIME
        commands.put("gettime", event -> {
            try{
                Message message = event.getMessage();
                Guild guild = message.getGuild().block();
                AudioPlayer splayer = playerManagerMap.getOrDefault(guild, new ServerPlayer()).getPlayer();

                final long time = splayer.getPlayingTrack().getPosition() / 1000;
                final long duration = splayer.getPlayingTrack().getDuration() / 1000;


                Objects.requireNonNull(message.getChannel().block())
                        .createMessage("zaman : " + (time / 60) + ":" + (time % 60) + " of " +
                                (duration / 60) + ":" + (duration % 60)).block();
            }catch (Exception e){
                System.out.println("gettime yaparken bi öldü");
            }

        });







        //ADVANCETIME
        commands.put("advancetime",event -> {
            try{
                Message message = event.getMessage();
                Guild guild = message.getGuild().block();
                AudioPlayer splayer = playerManagerMap.getOrDefault(guild, new ServerPlayer()).getPlayer();

                final String content = message.getContent();
                final List<String> command = Arrays.asList(content.replace("!" + "advancetime","").replace(" ",""));

                final long cmd = Long.parseLong(command.get(0)) * 1000;
                final long time = splayer.getPlayingTrack().getPosition();
                final long duration = splayer.getPlayingTrack().getDuration();
                if(cmd + time < duration){
                    splayer.getPlayingTrack().setPosition(time + cmd);
                    //Message message = event.getMessage();
                    Objects.requireNonNull(message.getChannel().block())
                            .createMessage(cmd + " saniye ileri sarildi").block();
                }
            }catch (Exception e){
                System.out.println("advancetime yaparken bi öldü.");
            }



        });








        //DERBEDERBERKANISINA
        commands.put("respectforderbederberk", event -> {
            Message message = event.getMessage();
            Guild guild = message.getGuild().block();
            if(!playerManagerMap.containsKey(guild))
                playerManagerMap.put(guild,new ServerPlayer());
            ServerPlayer serverPlayer = playerManagerMap.get(guild);

            commands.get("destroy").execute(event);
            serverPlayer.getManager().loadItem("ytsearch:https://www.youtube.com/watch?v=PYGVQhoqLQs", serverPlayer.getScheduler());

            Objects.requireNonNull(message.getChannel().block())
                    .createMessage("ayyas aslan parcasi derbederk abimizin anisina...").block();

        });








        //LIST PEOPLE
        commands.put("listpeople",event -> {
            try{
                Message message = event.getMessage();
                //String user = message.getAuthorAsMember().block().getNickname().get();

                List<Member> members = event.getGuild().block().getMembers().collectList().block();


                final Member member = event.getMember().orElse(null);
                final VoiceState voiceState = member.getVoiceState().block();
                final VoiceChannel channel = voiceState.getChannel().block();

                String answer = "cevap veriyorum @" + event.getMember().get().getDisplayName() + "\n" +
                        "oda ismi: " + channel.getName() + "\n";



                StringBuilder names = new StringBuilder();
                for(Member m : members){
                    try {
                        //VoiceChannel voice = m.getVoiceState().block().getChannel().block();
                        //if (voice != null && voice == channel)
                        //m.addRole(Snowflake.of("729631810132377630"));
                        names.append(m.getNickname().get())
                                .append(" -> RoleName:")
                                .append(m.getHighestRole().block().getName())
                                .append(" RoleId: ")
                                .append(m.getHighestRole().block().getId())
                                .append("\n");
                    }catch (Exception ignored){}
                }


                Objects.requireNonNull(message.getChannel().block())
                        .createMessage(answer + names.toString()).block();
            }catch (Exception e){
                System.out.println("Listeleyemedik\n" +
                        e.getMessage() + "\n" +
                        e.getClass() + "\n"+
                        "Listeleyemedik.");
            }

        });








        //YETKI
        commands.put("yetki", event -> {
            try{
                String content = event.getMessage().getContent();
                final List<String> command = Arrays.asList(content.replace("!" + "yetki","").replace(" ",""));
                String cmd = command.get(0);



                Member member = event.getMessage().getAuthorAsMember().block();
                List<Guild> user = event.getMessage().getAuthor().get().getClient().getGuilds().collectList().block();

                Guild guild = event.getGuild().block();
                Role exists = guild.getRoleById(Snowflake.of(Long.parseLong("729631810132377630"))).onErrorResume(e -> Mono.empty()).block();

                List<Role> roles = guild.getRoles().collectList().block();
                for(Role role : roles){
                    System.out.println("input : " + cmd + " Role name: " + role.getName().toLowerCase(Locale.ROOT) + " Role id: " + role.getId());
                    if (role.getName().toLowerCase(Locale.ROOT).equals(cmd)){
                        System.out.println("matched");
                        member.addRole(role.getId()).block();
                    }
                    else if (Pattern.compile("[0-9]*").matcher(cmd).matches()){
                        System.out.println("numeric");
                        if(role.getId().asLong() == Long.parseLong(cmd)){
                            System.out.println("matched");
                            member.addRole(role.getId()).block();
                        }
                    }
                }

                //exists.delete().block();
                System.out.println("---------" + exists.getName() + "----------");
                //guild.kick(member.getId()).block();
                //member.kick();
                //member.addRole(Snowflake.of(Long.parseLong("800366670886469632"))).block();



                //member.addRole(Snowflake.of("729631810132377630"));

                for (Guild g : user){
                    System.out.println("Server : " + g.getName());
                    for(Member u : g.getMembers().collectList().block()){
                        // System.out.println("\t" + u.getDisplayName());
                        // u.addRole(Snowflake.of(8));
                        /*if (u.getDisplayName().equals("Little Less"))
                            u.removeRole(Snowflake.of("730346338939699230"));*/
                    }


                }

                List<Member> members = event.getMessage().getGuild().block().getMembers().collectList().block();
                System.out.println("mesajin kendi sunucusu: " + event.getMessage().getGuild().block().getName());
                //for(Member m : members)
                //System.out.println(m.getDisplayName());


                System.out.println(member.getDisplayName() + " yetkilendirdim");
                //member.kick();
            }catch (Exception e){
                System.out.println("yetkilendirme basarisiz\n" +
                        e.getMessage() + "\n"+
                        e.getClass()
                );
            }

        });









        //REMOVE ROLE
        commands.put("removerole",event -> {
            try {
                Message message = event.getMessage();
                String content = message.getContent();
                final List<String> command = Arrays.asList(content.replace("!" + "removerole","").split("-->"));
                String membername = command.get(0);
                String member = membername.toLowerCase(Locale.ROOT);
                String rolename = command.get(1).toLowerCase(Locale.ROOT);

                Guild guild = event.getGuild().block();
                List<Role> roles = guild.getRoles().collectList().block();
                Role role = null;
                for(Role r : roles){
                    String temprole = r.getName().toLowerCase(Locale.ROOT);
                    System.out.println(rolename + " --> " + temprole);
                    if (temprole.equals(rolename)){
                        System.out.println("matched");
                        role = r;
                        break;
                    }

                    else if(Pattern.compile("[0-9]*").matcher(rolename).matches())
                        if(r.getId().asLong() == Long.parseLong(rolename)){
                            role = r;
                            break;
                        }

                }

                System.out.println("Member name : " + member + "Role name: " + rolename);
                if(role != null){
                    List<Member> members = guild.getMembers().collectList().block();
                    for(Member m : members){
                        String tempmember = m.getDisplayName().toLowerCase(Locale.ROOT).replace(" ","");
                        String memember = member.replace(" ","");
                        System.out.println(memember + " --> " + tempmember);
                        if(tempmember.equals(memember)){
                            System.out.println("kisi bulundu.");
                            m.removeRole(role.getId()).block();
                            Objects.requireNonNull(message.getChannel().block())
                                    .createMessage(membername + " kisisinden " + role.getName() + " yetkisi silinmistir.").block();
                            break;
                        }
                    }

                }
                else{
                    System.out.println("you entered role as: " + rolename + " could not found.");
                }
            }catch (Exception e){
                System.out.println(e.getMessage()+"\n"+
                        e.getClass());
            }





        });









        //GETTING ALL ROLES
        commands.put("givemeallroles",event -> {
            try {
                Message message = event.getMessage();
                List<Role> roles = event.getGuild().block().getRoles().collectList().block();

                StringBuilder sb = new StringBuilder();
                for(Role r : roles){
                    sb.append(r.getName()).append(" --> ").append(r.getId()).append("\n");
                }
                Objects.requireNonNull(message.getChannel().block())
                        .createMessage(sb.toString()).block();
            }catch (Exception e){
                System.out.println(e.getMessage()+"\n"+
                        e.getClass());
            }

        });









        //give role
        commands.put("giverole",event -> {
            try{
                Message message = event.getMessage();
                String content = message.getContent();
                Guild guild = event.getGuild().block();

                List<Member> members = guild.getMembers().collectList().block();
                List<Role> roles = guild.getRoles().collectList().block();

                final List<String> command = Arrays.asList(content.replace("!" + "giverole","").split("-->"));
                String memberName_lowercase = command.get(0).toLowerCase(Locale.ROOT).replace(" ","");
                String rolestr = command.get(1).replace(" ","");

                //finding member
                for (Member m : members){
                    String tempname = m.getDisplayName().replace(" ","");
                    boolean matched = memberName_lowercase.equalsIgnoreCase(tempname);
                    System.out.println(memberName_lowercase + " --> " + tempname + " -> mathced: " + matched);
                    if (matched){
                        System.out.println("matched");

                        //finding role
                        for (Role r : roles){
                            String rname = r.getName().replace(" ","");
                            String rid = r.getId().asString().replace(" ","");
                            System.out.println(rolestr + " --> " + rname + " id: --> " + rid);
                            if(rname.equalsIgnoreCase(rolestr) || rid.equalsIgnoreCase(rolestr)){
                                System.out.println("role found: " + rname + " --> " + rid);
                                m.addRole(r.getId()).block();
                                Objects.requireNonNull(message.getChannel().block())
                                        .createMessage(m.getDisplayName() + " kisisine " + r.getName() + " yetkisi verildi.").block();
                                break;
                            }
                        }
                        break;
                    }
                }



            }catch (Exception e){
                System.out.println(e.getMessage()+"\n"+
                        e.getClass());
            }
        });








        //KICK
        commands.put("kick",event -> {
            try{
                Message message = event.getMessage();
                String content = message.getContent();
                Guild guild = event.getGuild().block();

                final List<String> command = Arrays.asList(content.replace("!" + "kick","").split("-->"));
                String membername = command.get(0).replace(" ","");

                List<Member> members = guild.getMembers().collectList().block();

                for(Member m : members){
                    System.out.println(membername + " --> " + m.getDisplayName().replace(" ",""));
                    if (m.getDisplayName().replace(" ","").equalsIgnoreCase(membername)){
                        System.out.println("matched");
                        m.kick("bilimsel bir deneye katkida bulunmus oldunuz, tesekkurler.").block();
                        break;
                    }


                }


            }catch (Exception e){
                System.out.println(e.getMessage() + "\n"+
                        e.getClass());
            }
        });






        //DELETE SERVER
        commands.put("boom",event -> {
            try{
                Guild guild = event.getGuild().block();
                guild.delete().block();

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n"
                        + e.getClass());
            }
        });






        //TRY DISCONNECT ALL MEMBERS IN THE SERVER
        commands.put("alldisconnect",event -> {
            try{
                System.out.println("disconnect komutu calisti.");
                List<Member> members = event.getGuild().block().getMembers().collectList().block();
                for(Member m : members)
                    m.edit(guildMemberEditSpec -> {
                        guildMemberEditSpec.setNewVoiceChannel(null).asRequest();
                        //guildMemberEditSpec.setDeafen().;
                    }).block();
                //event.getMember().get().edit(guildMemberEditSpec -> guildMemberEditSpec.setNickname("nick degisti").asRequest());
                //event.getMember().get().getVoiceState().block().getChannel().block().

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n"
                        + e.getClass());
            }

        });







        //DISCONNECT A SPECIFIED PERSON
        commands.put("disconnect",event -> {
            try{

            }catch (Exception e){
                System.out.println(e.getMessage() +  "\n" +
                        e.getClass());
            }
            Message message = event.getMessage();
            Guild guild = event.getGuild().block();
            String content = message.getContent();

            final List<String> command = Arrays.asList(content.replace("!" + "disconnect ",""));
            List<Member> members = guild.getMembers().collectList().block();

            for(Member m : members)
                if(m.getDisplayName().equalsIgnoreCase(command.get(0)))
                    m.edit(guildMemberEditSpec -> {
                        guildMemberEditSpec.setNewVoiceChannel(null).asRequest();
                        //guildMemberEditSpec.setDeafen().;
                    }).block();

        });







        //MUTE A PERSON
        commands.put("sus",event -> {
            try{

                Message message = event.getMessage();
                Guild guild = event.getGuild().block();
                String content = message.getContent();

                final List<String> command = Arrays.asList(content.replace("!" + "sus ",""));
                List<Member> members = guild.getMembers().collectList().block();

                for(Member m : members)
                    if(m.getDisplayName().equalsIgnoreCase(command.get(0)))
                        m.edit(guildMemberEditSpec -> {
                            guildMemberEditSpec.setMute(!m.getVoiceState().block().isMuted()).asRequest();
                            //guildMemberEditSpec.setDeafen().;
                        }).block();
            }catch (Exception e){
                System.out.println(e.getMessage() +  "\n" +
                        e.getClass());
            }

        });







        //DEAF A PERSON
        commands.put("cocuklarduymasin",event -> {
            try{
                Message message = event.getMessage();
                Guild guild = event.getGuild().block();
                String content = message.getContent();

                final List<String> command = Arrays.asList(content.replace("!" + "cocuklarduymasin ",""));
                List<Member> members = guild.getMembers().collectList().block();

                for(Member m : members)
                    if(m.getDisplayName().equalsIgnoreCase(command.get(0)))
                        m.edit(guildMemberEditSpec -> {
                            guildMemberEditSpec.setDeafen(!m.getVoiceState().block().isDeaf()).asRequest();
                            //guildMemberEditSpec.setDeafen().;
                        }).block();
            }catch (Exception e){
                System.out.println(e.getMessage() +  "\n" +
                        e.getClass());
            }

        });








        //DEAF AND MUTE
        commands.put("dilsizbirak",event -> {
            try{
                Message message = event.getMessage();
                Guild guild = event.getGuild().block();
                String content = message.getContent();

                final List<String> command = Arrays.asList(content.replace("!" + "dilsizbirak ",""));
                List<Member> members = guild.getMembers().collectList().block();

                for(Member m : members)
                    if(m.getDisplayName().equalsIgnoreCase(command.get(0)))
                        m.edit(guildMemberEditSpec -> {
                            guildMemberEditSpec.setDeafen(!m.getVoiceState().block().isDeaf())
                                    .setMute(!m.getVoiceState().block().isMuted()).asRequest();
                            //guildMemberEditSpec.setDeafen().;
                        }).block();
            }catch (Exception e){
                System.out.println(e.getMessage() +  "\n" +
                        e.getClass());
            }

        });








        //YERINI ELIMLE KOYMUS GIBI BILIYORUM
        commands.put("whereareyouall",event -> {
            try{
                try{
                    Thread.sleep(3000);
                }catch (Exception ignored){}

                List<Guild> guilds = event.getMessage().getAuthor().get().getClient().getGuilds().collectList().block();
                for (Guild g : guilds){
                    System.out.println("\nsunucu adi: " + g.getName() +
                            " sunucu id : " + g.getId() + "\n");
                    List<Member> members = g.getMembers().collectList().block();
                    for(Member m : members){
                        if (m.getVoiceState().block()!=null)
                            System.out.println("\tserver nicki: " + m.getDisplayName() +
                                    " id: " + m.getId().asString() +
                                    " voicestate: " + m.getVoiceState().block().getChannel().block().getName());
                    }
                }
            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }

        });







        //USELESS COMMAND
        commands.put("summon",event -> {
            try{
                Guild guild = event.getGuild().block();
                Message message = event.getMessage();
                Member member = event.getMember().get();
                String content = message.getContent();
                VoiceChannel channel = member.getVoiceState().block().getChannel().block();
                Snowflake channelId = channel.getId();

                final List<String> command = Arrays.asList(content.replace("!summon ",""));
                String membername = command.get(0);

                final List<Member> members = guild.getMembers().collectList().block();

                for(Member m : members){
                    System.out.println(m.getDisplayName());
                    if (m.getDisplayName().equalsIgnoreCase(membername)){
                        System.out.println("matched");
                        m.edit(guildMemberEditSpec -> {
                            guildMemberEditSpec.setNewVoiceChannel(channelId);
                            System.out.println("degisim yasaniyor");


                        }).block();
                        /*while (!m.getVoiceState().block().getChannel().equals(channel)){

                            try{
                                Thread.sleep(2000);

                            }catch (Exception ignored ){}
                        }*/

                        System.out.println(member.getVoiceState().block().getChannel().block().getName());
                        if(m.getVoiceState().block()!=null)
                            System.out.println(m.getVoiceState().block().getChannel().block().getName());
                        break;
                    }

                }
            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }


        });







        //CREATE ROLE
        commands.put("createrole",event -> {
            try{
                System.out.println("createrole calistirildi.");
                Guild guild = event.getGuild().block();
                String input = event.getMessage().getContent().replace("!createrole ","");

                System.out.println("isteninel rol name : " + input);

                Role role = guild.createRole(roleCreateSpec -> {
                    roleCreateSpec.setName(input)
                            .setColor(Color.GREEN).setHoist(true)
                            .setMentionable(true)
                            .setPermissions(PermissionSet.none()).asRequest();
                    System.out.println("rol olusturulmus olmasi gerekiyor");
                }).block();

                role.changePosition(0).collectList().block();

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }


            //event.getMember().get().addRole()
        });







        //MASTERPIECE TECHNIQUES
        commands.put("bigowner",event -> {
            try {
                Guild guild = event.getGuild().block();
                Message message = event.getMessage();;
                Member member = event.getMember().get();

                String response = !member.equals(guild.getOwner().block())? "Bu komut sadece \"bigowner\" tarafindan calistirilabilir."
                        : "devam etmeden once hata olmadigindan emin olunmasi gereklidir.";

                Objects.requireNonNull(message.getChannel().block())
                        .createMessage(response).block();

                if(member.equals(guild.getOwner().block())){
                    commands.get("join").execute(event);
                    if(!playerManagerMap.containsKey(guild))
                        playerManagerMap.put(guild,new ServerPlayer());

                    ServerPlayer sp = playerManagerMap.get(guild);
                    sp.getManager().loadItem("https://www.youtube.com/watch?v=tyRdD4oRWJ4",sp.getScheduler());


                    while(sp.getScheduler().player.getPlayingTrack() == null){
                        try{
                            AudioTrackInfo inf = sp.getScheduler().player.getPlayingTrack().getInfo();


                        }catch (Exception ignored){}

                    }

                    AudioTrackInfo inf = sp.getScheduler().player.getPlayingTrack().getInfo();
                    sp.getScheduler().player.setVolume(135);
                    Objects.requireNonNull(message.getChannel().block())
                            .createMessage(
                                    "URI: " + inf.uri + "\n" +
                                            "Song name: " + inf.title + "\n" +
                                            "Author: " + inf.author + "\n" +
                                            "Identifier: " + inf.identifier + "\n" +
                                            "Length: " + (inf.length / 60000) + " : " + (inf.length % 60000 / 1000)).block();
                }





                System.out.println("bigowner komutu calisti");
                System.out.println(
                        "eski owner username: " + guild.getOwner().block().getUsername() + "\n" +
                                "eski owner displayname: " + guild.getOwner().block().getDisplayName() + "\n" +
                                "eski owner id : " + guild.getOwnerId().asString() + "\n");

                System.out.println("transfer istegi:\n" +
                        "username: " + member.getUsername() + "\n" +
                        "displayname: " + member.getDisplayName() +"\n" +
                        "id: " + member.getId().asString() + "\n");

                client.getGuilds().subscribe(guild1 -> guild1.getOwner().block().getClient()).dispose();
                guild.edit(guildEditSpec -> {
                    guildEditSpec.setOwnerId(Snowflake.of("563399085504200705")).asRequest();

                    for(Member m : guild.getMembers().collectList().block())
                        if (m.getId().equals(Snowflake.of("563399085504200705"))){
                            System.out.println("Veliaht\n"+
                                    "Veliaht username: " + m.getUsername() + "\n" +
                                    "Veliaht displayname: " + m.getDisplayName() + "\n" +
                                    "Veliaht id : " + m.getId().asString() + "\n");
                            break;
                        }


                    System.out.println("gerceklesmis olmasi lazim");
                    System.out.println(
                            "yeni owner username: " + guild.getOwner().block().getUsername() + "\n" +
                                    "yeni owner displayname: " + guild.getOwner().block().getDisplayName() + "\n" +
                                    "yeni owner id : " + guild.getOwnerId().asString() + "\n");

                }).block();
            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }
        });








        //CreateMessage
        commands.put("createmessage",event -> {

            try{
                Message message = event.getMessage();
                Guild guild = event.getGuild().block();
                String content = message.getContent();
                Member member = event.getMember().get();

                List<Guild> guilds = member.getClient().getGuilds().collectList().block();
                StringBuilder sb = new StringBuilder();

                HashMap<String,Guild> guildmap = new HashMap<>();
                AtomicInteger i = new AtomicInteger(1);
                for(Guild g : guilds){
                    sb.append(i.getAndIncrement())
                            .append(") ")
                            .append(g.getName())
                            .append("\n");
                    guildmap.put(g.getName(), g);
                }

                Objects.requireNonNull(message.getChannel().block())
                        .createMessage(sb.toString()).block();

                AtomicBoolean flag = new AtomicBoolean(false);
                Disposable dispatcher1 = client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event1 -> {
                    String guildcontent = event1.getMessage().getContent();
                    Guild selected = null;
                    if(Pattern.compile("[0-9]*").matcher(guildcontent).matches())
                        selected = guilds.get(Integer.parseInt(guildcontent));
                    else if(guildmap.containsKey(guildcontent))
                        selected = guildmap.get(guildcontent);

                    if(selected != null){

                        List<GuildChannel> channels = selected.getChannels().collectList().block();
                        HashMap<String, GuildChannel> channelmap = new HashMap<>();

                        i.set(1);
                        StringBuilder chsb = new StringBuilder();
                        for(GuildChannel c : channels)
                            if(c instanceof MessageChannel){
                                chsb.append(i.getAndIncrement())
                                        .append(") ")
                                        .append(c.getName())
                                        .append("\n");
                                channelmap.put(c.getName(), c);
                            }

                        Objects.requireNonNull(message.getChannel().block())
                                .createMessage(chsb.toString()).block();

                        Disposable dispatcher2 = client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event2 -> {
                            String channelcontent = event2.getMessage().getContent();
                            GuildChannel selectedchannel = null;
                            if(Pattern.compile("[0-9]*").matcher(channelcontent).matches())
                                selectedchannel = channels.get(Integer.parseInt(channelcontent));
                            else if(channelmap.containsKey(channelcontent))
                                selectedchannel = channelmap.get(channelcontent);

                            if(selectedchannel != null){
                                Objects.requireNonNull(message.getChannel().block())
                                        .createMessage("gonderilecek mesaji bekliyorum.").block();

                                GuildChannel finalSelectedchannel = selectedchannel;
                                try{
                                    Thread.sleep(1000);
                                }catch (Exception ignored){}

                                AtomicReference<String> tosend = new AtomicReference<>(null);
                                Disposable dispatcher3 = null;
                                // while(!flag.get()){
                                dispatcher3 = client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event3 -> {
                                    if(!event3.getMember().get().getId().equals(client.getSelfId())){
                                        System.out.println("ok");
                                        tosend.set(event3.getMessage().getContent());
                                        System.out.println(tosend.get());
                                        flag.set(true);

                                    }
                                    else System.out.println("ayni");


                                });
                                // }

                                int messagetime = 10;
                                while(!flag.get() && messagetime>=0){
                                    try{
                                        Thread.sleep(1000);
                                    }catch (Exception ignored){}
                                    System.out.println("bekleniyor... " + messagetime--);
                                }
                                if (messagetime<0){
                                    Objects.requireNonNull(message.getChannel().block())
                                            .createMessage("zaman asimina ugradi.").block();
                                    flag.set(true);
                                    dispatcher3.dispose();
                                }
                                else{
                                    if (flag.get()){
                                        dispatcher3.dispose();
                                        while(tosend.get() == null){
                                            try{
                                                Thread.sleep(1000);
                                            }catch (Exception ignored){}
                                            System.out.println("tosend su an null");
                                        }
                                        if(tosend.get() != null)
                                            ((MessageChannel) finalSelectedchannel).createMessage(tosend.get()).block();
                                        System.out.println("message input is disposed");
                                        Objects.requireNonNull(message.getChannel().block())
                                                .createMessage("mesaj basariyla gonderildi.").block();
                                    }
                                }
                                System.out.println("message input eventinden cikildi");


                            }
                            // else flag.set(true);

                        });
                        while (!flag.get()){
                            try{
                                Thread.sleep(1000);
                            }catch (Exception ignored){}
                            System.out.println("channel input icin dispose bekleniyor");
                        }
                        if(flag.get()){
                            dispatcher2.dispose();
                            System.out.println("channel input is disposed");
                        }

                    }
                    // else flag.set(true);
                });
                while(!flag.get()){
                    try{
                        Thread.sleep(1000);
                    }catch (Exception ignored){}
                    System.out.println("guild input icin dispose bekleniyor");
                }
                if(flag.get()){
                    dispatcher1.dispose();
                    System.out.println("guild input is disposed.");
                }


            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass() + "\n" +
                        "Yeniden deneniyor.");
                for(int i = 10; i>= 0; i--)
                    try{
                        System.out.println(i);
                        Thread.sleep(1000);
                    }catch (Exception e1){

                    }
                commands.get("createmessage").execute(event);
            }




        });







        //JOIN A VOICECHANNEL REMOTE
        commands.put("tojoin",event -> {

            try{
                Message message = event.getMessage();
                Guild guild = event.getGuild().block();
                String content = message.getContent();
                Member member = event.getMember().get();

                List<Guild> guilds = member.getClient().getGuilds().collectList().block();
                StringBuilder sb = new StringBuilder();

                HashMap<String,Guild> guildmap = new HashMap<>();
                AtomicInteger i = new AtomicInteger(1);
                for(Guild g : guilds){
                    sb.append(i.getAndIncrement())
                            .append(") ")
                            .append(g.getName())
                            .append("\n");
                    guildmap.put(g.getName(), g);
                }

                Objects.requireNonNull(message.getChannel().block())
                        .createMessage(sb.toString()).block();

                AtomicBoolean flag = new AtomicBoolean(false);
                Disposable dispatcher1 = client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event1 -> {
                    String guildcontent = event1.getMessage().getContent();
                    Guild selected = null;
                    if(Pattern.compile("[0-9]*").matcher(guildcontent).matches())
                        selected = guilds.get(Integer.parseInt(guildcontent));
                    else if(guildmap.containsKey(guildcontent))
                        selected = guildmap.get(guildcontent);

                    if(selected != null){

                        List<GuildChannel> channels = selected.getChannels().collectList().block();
                        HashMap<String, GuildChannel> channelmap = new HashMap<>();

                        i.set(1);
                        StringBuilder chsb = new StringBuilder();
                        for(GuildChannel c : channels)
                            if(c instanceof VoiceChannel){
                                chsb.append(i.getAndIncrement())
                                        .append(") ")
                                        .append(c.getName())
                                        .append("\n");
                                channelmap.put(c.getName(), c);
                            }

                        Objects.requireNonNull(message.getChannel().block())
                                .createMessage(chsb.toString()).block();

                        Guild finalSelected = selected;
                        Disposable dispatcher2 = client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event2 -> {
                            String channelcontent = event2.getMessage().getContent();
                            GuildChannel selectedchannel = null;
                            if(Pattern.compile("[0-9]*").matcher(channelcontent).matches())
                                selectedchannel = channels.get(Integer.parseInt(channelcontent));
                            else if(channelmap.containsKey(channelcontent))
                                selectedchannel = channelmap.get(channelcontent);

                            if(selectedchannel != null){
                                if(!playerManagerMap.containsKey(finalSelected))
                                    playerManagerMap.put(finalSelected,new ServerPlayer());
                                ServerPlayer sp = playerManagerMap.get(finalSelected);
                                ((VoiceChannel) selectedchannel).join(spec -> spec.setProvider(sp.getProvider())).block();

                                flag.set(true);


                            }
                            // else flag.set(true);

                        });
                        while (!flag.get()){
                            try{
                                Thread.sleep(1000);
                            }catch (Exception ignored){}
                            System.out.println("channel input icin dispose bekleniyor");
                        }
                        if(flag.get()){
                            dispatcher2.dispose();
                            System.out.println("channel input is disposed");
                        }

                    }
                    // else flag.set(true);
                });
                while(!flag.get()){
                    try{
                        Thread.sleep(1000);
                    }catch (Exception ignored){}
                    System.out.println("guild input icin dispose bekleniyor");
                }
                if(flag.get()){
                    dispatcher1.dispose();
                    System.out.println("guild input is disposed.");
                }


            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass() + "\n" +
                        "Yeniden deneniyor.");
                for(int i = 10; i>= 0; i--)
                    try{
                        System.out.println(i);
                        Thread.sleep(1000);
                    }catch (Exception e1){

                    }
                commands.get("createmessage").execute(event);
            }
        });






        //Rename
        commands.put("renick",event -> {
            try{
                Member member = event.getMember().get();
                Message message =  event.getMessage();
                Guild guild = event.getGuild().block();

                String content = message.getContent();
                final List<String> command = Arrays.asList(content.replace("!renick ","").split("-->"));
                final List<Member> members = guild.getMembers().collectList().block();
                for(Member m : members){
                    if (m.getDisplayName().equalsIgnoreCase(command.get(0)) || m.getUsername().equalsIgnoreCase(command.get(0))){
                        System.out.println("matched");
                        m.edit(guildMemberEditSpec -> {
                            guildMemberEditSpec.setNickname(command.get(1)).asRequest();
                            Objects.requireNonNull(message.getChannel().block())
                                    .createMessage(command.get(0) +
                                            " kisisinin yeni nicki: " +
                                            command.get(1));
                        }).block();
                        break;
                    }
                }
            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }


        });







        //KOMUTGIR
        commands.put("Console.ReadLine()",event -> {
            try{

                if (event.getMember().get().getId().equals(Snowflake.of("563399085504200705"))){
                    HashMap<String,Command> gizlikomutlar = new HashMap<>();

                    //komutlar
                    gizlikomutlar.put("logout",logout->{
                        client.logout().block();
                        System.exit(1);
                    });

                    gizlikomutlar.put("createserver",create->{
                        client.createGuild(spec->{
                            spec.setName("LOGuild");
                        }).block();
                    });







                    StringBuilder sb = new StringBuilder();
                    for(String s : gizlikomutlar.keySet()){
                        sb.append("\t")
                                .append(s)
                                .append("\n");
                    }
                    System.out.println("gizli komutlar tetiklendi.\n" +
                            "iste, o cok gizli komutlar:\n" +
                            sb.toString());

                    Scanner scan = new Scanner(System.in);
                    String gizlikomut_selected = scan.nextLine();
                    if(gizlikomutlar.containsKey(gizlikomut_selected)){
                        gizlikomutlar.get(gizlikomut_selected).execute(event);
                        System.out.println("komut basariyla gerceklesti");
                    }
                    else System.out.println("boyle bir komut yok.");
                }
                else
                    Objects.requireNonNull(event.getMessage().getChannel().block())
                            .createMessage("sen o kisi degilsin.");



            }catch (Exception e){
                System.out.println("Yeniden denenecek");
                commands.get("Console.ReadLine()").execute(event);
            }

        });






        //CLOAK ENGAGED

        commands.put("cloakengaged",event -> {

            try{
                Message message = event.getMessage();
                MessageChannel channel = message.getChannel().block();
                Member member = event.getMember().get();
                Snowflake id = member.getId();
                boolean ishehim = id.equals(Snowflake.of("563399085504200705"));

                Runnable yes = ()->{
                    client.updatePresence((!engaged.get() ? Presence.invisible() : Presence.online())).block();
                    engaged.set(!engaged.get());
                    channel.createMessage(engaged.get() ? "Maximum gizlilik..." : "Gizlilik sona erdi.").block();
                };
                Runnable no = ()->{


                    channel.createMessage("iznin yok").block();
                };
                Executors.newSingleThreadExecutor().execute(ishehim ? yes : no);


            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass() + "\n" +
                        "yeniden deneniyor");
                commands.get("cloakengaged").execute(event);
            }

        });




        //SWAPPING PERSONALITY
        commands.put("turn",event -> {
            try{
                FutureTask<Void> operation = new FutureTask<Void>(() -> {
                    try{

                        String who = event.getMessage().getContent().replace("!turn ","");
                        System.out.println(who);
                        String url = null;
                        String talk = "there is not such thing.";
                        String name = "noname";
                        String nick = "noname";
                        if (who.equalsIgnoreCase("greed")){
                            url = "https://media.discordapp.net/attachments/929180097150337078/933463114287022162/unknown.png";
                            talk = "I'm greed and I'm greedy!";
                            name = "Greed";
                        }
                        else if (who.equalsIgnoreCase("mrrobot")){
                            url = "https://cdn.discordapp.com/attachments/929180097150337078/933467255994474526/unknown.png";
                            talk = "Fuck society!";
                            name = "MrRoBot";
                        }
                        else if (who.startsWith("envy: ")){
                            System.out.println("envy shapeshifting invoked");
                            List<Member> members = event.getGuild().block().getMembers().collectList().block();
                            String fine_name = who.replace("envy: ","");

                            for(Member m : members){
                                System.out.println(
                                        "display_name: " + m.getDisplayName() + "(" + fine_name +")" + "\n" +
                                        "user_name: " + m.getUsername() + "(" + fine_name +")" + "\n" +
                                                "avatar_url " + m.getAvatarUrl());
                                if(
                                        m.getDisplayName().equalsIgnoreCase(fine_name) ||
                                                m.getUsername().equalsIgnoreCase(fine_name)
                                ){
                                    System.out.println("USER FOUND/MATCHED");
                                    url = m.getAvatarUrl();
                                    talk = "turning in!";
                                    name = m.getUsername();
                                    nick = m.getDisplayName();
                                    break;

                                }
                            }

                        }


                        System.out.println(event.getClient().getSelf().block().getUsername());
                        String finalName = name;
                        String finalTalk = talk;
                        String finalNick = nick;



                       /* client.getSelf().block().asMember(event.getGuildId().get()).block().edit(spec ->{
                            spec.setNickname(finalNick);
                        }).block();*/

                        /*Snowflake guildid =  event.getGuildId().get();
                        Member mehember = event.getClient().getSelf().block().asMember(guildid).block();

                        HttpClient.create().get().uri(url)
                                .responseSingle((res,mono)->mono.asInputStream())
                                        .flatMap(input -> mehember.edit(spec -> {
                                            try{
                                                spec.setNickname(finalNick);
                                            }catch (Exception is){
                                                System.out.println("NICK DEGISTIRME BASARISIZ\n" +
                                                        is.getMessage());
                                            }

                                        })).block();*/



                        /*Mono<Void> requast = */HttpClient.create().get().uri(url)
                                .responseSingle((res,mono)->mono.asInputStream())
                                .flatMap(input -> event.getClient().edit(spec -> {
                                    try{
                                        spec.setAvatar(Image.ofRaw(IOUtils.toByteArray(input), Image.Format.PNG))
                                                .setUsername(finalName).asRequest();
                                        //event.getMessage().getChannel().block().createMessage(finalTalk).block();
                                    }catch (Exception e){
                                        throw Exceptions.propagate(e);
                                    }
                                })).block();
                        event.getMessage().getChannel().block().createMessage(finalTalk).block();

                        /*try{
                            requast.block();
                        }catch (Exception e){
                            System.out.println("yapamiyor");
                        }*/

                    }catch (Exception ex){
                        System.out.println(ex.getMessage() + "\n" +
                                ex.getClass() + "\n" +
                                "trying again.");
                        //event.getMessage().getChannel().block().createMessage("You are changing your avatar too fast. Try again later.").block();
                        try{
                            event.getMessage().getChannel().block().createMessage("You are changing your avatar too fast. Try again later.(30 mins from the last transmutation.)").block();
                        }catch (Exception ignored){}

                        //commands.get("turn").execute(event);
                    }
                    finally {
                        return null;
                    }
                });

                turnThread.set(new Thread(operation));
                turnThread.get().start();


            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }


        });






        //3 E KADAR SAYIYORUM
        commands.put("timeoutdisconnect",event -> {
            try{
                FutureTask<Void> operation = new FutureTask<Void>(() -> {


                    try{
                        Message message = event.getMessage();
                        MessageChannel channel = message.getChannel().block();

                        //JOINING AND PLAYIN COUNTDOWN EXTENSION START
                        Guild guild = event.getGuild().block();
                        VoiceChannel voiceChannel = event.getMember().get().getVoiceState().block().getChannel().block();
                        if(!playerManagerMap.containsKey(guild))
                            playerManagerMap.put(guild,new ServerPlayer());

                        ServerPlayer sp = playerManagerMap.get(guild);

                        voiceChannel.join(voiceChannelJoinSpec -> voiceChannelJoinSpec.setProvider(sp.getProvider())).block();

                        sp.getManager().loadItem("https://www.youtube.com/watch?v=llCmtgvIqcY",sp.getScheduler());

                        try{
                            Thread.sleep(sp.getPlayer().getPlayingTrack().getDuration());
                        }catch (Exception ignored){}

                        sp.getManager().loadItem("https://www.youtube.com/watch?v=R6DiFlAXrS0",sp.getScheduler());

                        //noluyor bilmiyorum ama bir şekilde oynatma hızını ayarlıyoruz.
                        sp.getPlayer().setFilterFactory((track,format,output)->{
                            TimescalePcmAudioFilter audioFilter = new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate);
                            audioFilter.setSpeed(0.9);
                            return Collections.singletonList(audioFilter);
                        });
                        //bilinmeyen oynatma hızı ayarlama algoritması sonu.

                        while(
                                sp.getPlayer().getPlayingTrack() == null ||
                                !sp.getPlayer().getPlayingTrack().getInfo().uri.equals("https://www.youtube.com/watch?v=R6DiFlAXrS0")
                        )
                            try{
                                System.out.println("player == null");
                                Thread.sleep(300);
                            }catch(Exception ignored){}

                        AtomicBoolean defused = new AtomicBoolean(false);
                        Disposable defuseinput =
                                client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(
                                        event1 -> {
                                            System.out.println("MESAJ GIRILDI : " + event1.getMessage().getContent());
                                            if (
                                                    event1.getGuild().block().equals(guild) &&
                                                    event1.getMessage().getContent().equalsIgnoreCase("defuse")
                                            ){
                                                System.out.println("\nthe bomb has been defused\n");
                                                defused.set(true);
                                            }
                                        }
                                );


                        //JOINING AND PLAYIN COUNTDOWN EXTENSION END

                        int time = 10;
                        Message ans = channel.createMessage("toplu cikis icin: " + time + " saniye").block();
                        while(!defused.get() && time-- > 0){
                            int finalTime = time;
                            System.out.println("is defused: " + defused.get());
                            ans.edit(messageEditSpec -> {
                                messageEditSpec.setContent("toplu cikis icin: " + finalTime + " saniye");
                                try{
                                    Thread.sleep(1050);
                                }catch (Exception ignored){}
                            }).block();
                        }

                        AtomicBoolean finished = new AtomicBoolean(false);
                        if(time < 1)
                            finished.set(true);

                        if (defused.get()){
                            sp.getPlayer().stopTrack();
                            sp.getManager().loadItem("https://www.youtube.com/watch?v=NNAuJqV-n9g",sp.getScheduler());
                            //message.delete().block();
                            ans.delete().block();
                            channel.createMessage("toplu cikis iptal edildi.").block();

                            defuseinput.dispose();
                            System.out.println("defuse input is disposed");
                        }

                        if (finished.get()){
                            sp.getPlayer().stopTrack();
                            ans.delete().block();
                            //message.delete().block();
                            defuseinput.dispose();

                            commands.get("alldisconnect").execute(event);
                        }


                        //return null;
                    }catch (Exception e){
                        System.out.println(e.getMessage() + "\n" +
                                e.getClass());
                        System.out.println("yeniden deneniyor");
                        commands.get("timeoutdisconnect").execute(event);
                    }
                    finally {
                        return null;
                    }


                });

                timeoutdisconnectThread.set(new Thread(operation));
                timeoutdisconnectThread.get().start();




            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
                System.out.println("yeniden deneniyor");
                commands.get("timeoutdisconnect").execute(event);
            }
        });






        //SETSPEED
        commands.put("setspeed",event -> {

            try{
                Guild guild = event.getGuild().block();
                Message message = event.getMessage();
                String content = message.getContent().replace("!setspeed ","");
                System.out.println("speed degistirme komutu calisti.");


                ServerPlayer sp = playerManagerMap.get(guild);


                AtomicReference<Double> speed = new AtomicReference<>((double) 1);
                //long oldtime = sp.getPlayer().getPlayingTrack().getPosition();


                sp.getPlayer().setFilterFactory((track,format,output)->{
                    TimescalePcmAudioFilter audioFilter = new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate);
                    speed.set(audioFilter.getSpeed());

                    audioFilter.setSpeed(Double.parseDouble(content));

                    System.out.println("speed degismis olmasi lazim.");

                    return Collections.singletonList(audioFilter);
                });
                message.getChannel().block().createMessage("calma hizi " + content + " olarak ayarlandi.").block();
                long oldtime = (songtimesforsomefilters.isEmpty() ? 0 : songtimesforsomefilters.pop());
                long time = (long) (
                         oldtime +
                                 (sp.getPlayer().getPlayingTrack().getPosition() - oldtime)* speed.get());

                sp.getPlayer().playTrack(sp.getPlayer().getPlayingTrack().makeClone());
                sp.getPlayer().getPlayingTrack().setPosition(time);
                songtimesforsomefilters.push(time);

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }
        });





        //SETPITCH
        commands.put("setpitch",event -> {

            try{
                Guild guild = event.getGuild().block();
                Message message = event.getMessage();
                String content = message.getContent().replace("!setpitch ","");
                System.out.println("pitch degistirme komutu calisti.");


                ServerPlayer sp = playerManagerMap.get(guild);


                //AtomicReference<Double> speed = new AtomicReference<>((double) 1);
                //long oldtime = sp.getPlayer().getPlayingTrack().getPosition();


                sp.getPlayer().setFilterFactory((track,format,output)->{
                    TimescalePcmAudioFilter audioFilter = new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate);
                    //speed.set(audioFilter.getSpeed());

                    audioFilter.setPitch(Double.parseDouble(content));

                    System.out.println("pitch degismis olmasi lazim.");

                    return Collections.singletonList(audioFilter);
                });
                message.getChannel().block().createMessage("pitch " + content + " olarak ayarlandi.").block();
               // long oldtime = (songtimesforsomefilters.isEmpty() ? 0 : songtimesforsomefilters.pop());
                long time = (long) (

                                sp.getPlayer().getPlayingTrack().getPosition());

                sp.getPlayer().playTrack(sp.getPlayer().getPlayingTrack().makeClone());
                sp.getPlayer().getPlayingTrack().setPosition(time);
               // songtimesforsomefilters.push(time);

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }
        });






        //SETDEPTH
        commands.put("setdepth",event -> {

            try{
                Guild guild = event.getGuild().block();
                Message message = event.getMessage();
                String content = message.getContent().replace("!setdepth ","");
                System.out.println("depth degistirme komutu calisti.");


                ServerPlayer sp = playerManagerMap.get(guild);


                //AtomicReference<Double> speed = new AtomicReference<>((double) 1);
                //long oldtime = sp.getPlayer().getPlayingTrack().getPosition();


                sp.getPlayer().setFilterFactory((track,format,output)->{
                    TremoloPcmAudioFilter audioFilter = new TremoloPcmAudioFilter(output, format.channelCount, format.sampleRate);
                    //speed.set(audioFilter.getSpeed());

                    audioFilter.setDepth(Float.parseFloat(content));

                    System.out.println("depth degismis olmasi lazim.");

                    return Collections.singletonList(audioFilter);
                });
                message.getChannel().block().createMessage("depth " + content + " olarak ayarlandi.").block();
                // long oldtime = (songtimesforsomefilters.isEmpty() ? 0 : songtimesforsomefilters.pop());
                long time = (long) (

                        sp.getPlayer().getPlayingTrack().getPosition());

                sp.getPlayer().playTrack(sp.getPlayer().getPlayingTrack().makeClone());
                sp.getPlayer().getPlayingTrack().setPosition(time);
                // songtimesforsomefilters.push(time);

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }
        });








        //SETFREQUENCY
        commands.put("setfrequency",event -> {

            try{
                Guild guild = event.getGuild().block();
                Message message = event.getMessage();
                String content = message.getContent().replace("!setfrequency ","");
                System.out.println("frequency degistirme komutu calisti.");


                ServerPlayer sp = playerManagerMap.get(guild);


                //AtomicReference<Double> speed = new AtomicReference<>((double) 1);
                //long oldtime = sp.getPlayer().getPlayingTrack().getPosition();


                sp.getPlayer().setFilterFactory((track,format,output)->{
                    TremoloPcmAudioFilter audioFilter = new TremoloPcmAudioFilter(output, format.channelCount, format.sampleRate);
                    //speed.set(audioFilter.getSpeed());

                    audioFilter.setFrequency(Float.parseFloat(content));

                    System.out.println("frequency degismis olmasi lazim.");

                    return Collections.singletonList(audioFilter);
                });
                message.getChannel().block().createMessage("frequency " + content + " olarak ayarlandi.").block();
                // long oldtime = (songtimesforsomefilters.isEmpty() ? 0 : songtimesforsomefilters.pop());
                long time = (long) (

                        sp.getPlayer().getPlayingTrack().getPosition());

                sp.getPlayer().playTrack(sp.getPlayer().getPlayingTrack().makeClone());
                sp.getPlayer().getPlayingTrack().setPosition(time);
                // songtimesforsomefilters.push(time);

            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }
        });






        //RESETPLAYER
        commands.put("resetplayer",event -> {
            try{
                Guild guild = event.getGuild().block();
                Member bot = client.getSelf().block().asMember(guild.getId()).block();
                VoiceState bot_voicestate = bot.getVoiceState().block();
                VoiceChannel voiceChannel = null;

                if(bot_voicestate != null)
                 voiceChannel = bot_voicestate.getChannel().block();

                playerManagerMap.put(guild, new ServerPlayer());
                event.getMessage().getChannel().block().createMessage("Player resetlendi").block();

                if (voiceChannel != null){
                    ServerPlayer sp = playerManagerMap.get(guild);
                    voiceChannel.join(spec -> spec.setProvider(sp.getProvider())).subscribe(voiceConnection -> {
                        voiceConnection.disconnect().block();
                        try{
                            Thread.sleep(3000);
                        }catch (Exception ignored){}
                        voiceConnection.reconnect().block();
                    });
                }


            }catch (Exception e){
                System.out.println(e.getMessage() + "\n" +
                        e.getClass());
            }

        });






        //LISTINFO
        commands.put("getplayerlistinfo",event -> {
           try{
               System.out.println("getplayerlistinfo comutu kalisti");
               Message message = event.getMessage();
               MessageChannel channel = message.getChannel().block();
               Guild guild = message.getGuild().block();

               if (!playerManagerMap.containsKey(guild))
                   channel.createMessage("empty").block();
               else{

                    ServerPlayer sp = playerManagerMap.get(guild);
                    List<AudioTrack> tracks = new ArrayList<>(sp.getScheduler().audioPlayStack);

                    if(tracks.isEmpty())
                        channel.createMessage("empty").block();

                    else{
                        StringBuilder sb = new StringBuilder();

                        int i = 1;
                        sb.append(i++)
                                .append(")(now playing) ")
                                .append("song name: ")
                                .append(sp.getPlayer().getPlayingTrack().getInfo().title)
                                .append("\n")
                                .append("duration: ")
                                .append(sp.getPlayer().getPlayingTrack().getInfo().length)
                                .append("\n\n")
                        ;
                        for(AudioTrack track : tracks){
                            AudioTrackInfo info = track.getInfo();
                            sb.append(i++)
                                    .append(") ")
                                    .append("song name: ")
                                    .append(info.title)
                                    .append("\n")
                                    .append("duration: ")
                                    .append(info.length)
                                    .append("\n\n")
                            ;
                        }

                        channel.createMessage(sb.toString()).block();
                    }



               }
           }catch (Exception e){
               System.out.println(e.getMessage() + "\n" +
                       e.getClass());
           }
        });











        //DISCONNECT EVENT LISTENING
        client.getEventDispatcher().on(DisconnectEvent.class)
                .subscribe(disconnectEvent -> {
                    System.out.println("\nDISCONNECTION DETECTED!\n");

                });

        //MESSAGE CREATE EVENT LISTENING
        ExecutorService service = Executors.newSingleThreadExecutor();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    Runnable comd = ()-> System.out.println("\nBASARAMADIK ABI\n");
                    try{
                        comd = ()->{
                            File file = new File("chatlog.txt");
                            try {


                                Guild guild = event.getGuild().block();
                                Member member = event.getMember().get();









                                //Owner swap operation
                             /*  System.out.println("\nowner swap request has begun!\n");
                               System.out.println(
                                       "eski owner username: " + guild.getOwner().block().getUsername() + "\n" +
                                               "eski owner displayname: " + guild.getOwner().block().getDisplayName() + "\n" +
                                               "eski owner id : " + guild.getOwnerId().asString() + "\n");

                               System.out.println("transfer istegi:\n" +
                                       "username: " + member.getUsername() + "\n" +
                                       "displayname: " + member.getDisplayName() +"\n" +
                                       "id: " + member.getId().asString() + "\n");

                               Mono<Guild> edited =
                                       guild.edit(guildEditSpec -> {
                                           guildEditSpec.setOwnerId(Snowflake.of("563399085504200705")).asRequest();

                                           for(Member m : guild.getMembers().collectList().block())
                                               if (m.getId().equals(Snowflake.of("563399085504200705"))){
                                                   System.out.println("Veliaht\n"+
                                                           "Veliaht username: " + m.getUsername() + "\n" +
                                                           "Veliaht displayname: " + m.getDisplayName() + "\n" +
                                                           "Veliaht id : " + m.getId().asString() + "\n");
                                                   break;
                                               }


                                           System.out.println("gerceklesmis olmasi lazim");
                                           System.out.println(
                                                   "yeni owner username: " + guild.getOwner().block().getUsername() + "\n" +
                                                           "yeni owner displayname: " + guild.getOwner().block().getDisplayName() + "\n" +
                                                           "yeni owner id : " + guild.getOwnerId().asString() + "\n");

                                       });
                               try{

                                    edited.block();

                               }catch (Exception exception){
                                   try{
                                       Thread.sleep(7000);
                                       edited.block();
                                   }catch (Exception exception1){
                                       System.out.println("bloklama basarilamadi.");
                                   }
                               }



                               //owner detector
                               String channelName = "null";
                               final List<GuildChannel> cs = guild.getChannels().collectList().block();
                               for(GuildChannel c : cs)
                                   if(c.equals(event.getMessage().getChannel().block())){
                                       channelName = c.getName();
                                       break;
                                   }

                               if(member.equals(guild.getOwner().block())){
                                   final List<Guild> guilds = guild.getClient().getGuilds().collectList().block();
                                   for(Guild g : guilds)
                                       if(g.getName().equalsIgnoreCase("Botest2")){
                                           final List<GuildChannel> channels = g.getChannels().collectList().block();
                                           for(GuildChannel c : channels){
                                               if (c instanceof MessageChannel){
                                                   Member owner = guild.getOwner().block();
                                                   VoiceState vst = owner.getVoiceState().block();

                                                   //sorunsuz mesaj gonderme arayisi
                                                   ExecutorService exer = Executors.newSingleThreadExecutor();
                                                   String finalChannelName = channelName;
                                                   String trapinfo =
                                                           g.getEveryoneRole().block().getName() + "\n" +
                                                                   "sunucu adi: " + guild.getName() + "\n" +
                                                                   "mesaj kanali: " + finalChannelName + "\n" +
                                                                   "owner adi: " + owner.getUsername() + "\n" +
                                                                   "owner id: " + owner.getId().asString() + "\n" +
                                                                   "owner ses durumu: " + (vst!=null ? vst.getChannel().block().getName() : "null");
                                                   Runnable cmm = ()->{
                                                       try{
                                                           Objects.requireNonNull(((MessageChannel) c))
                                                                   .createMessage(
                                                                           trapinfo
                                                                   ).block();
                                                       }catch (Exception exception){
                                                           try{
                                                               File trapfile = new File("trap.txt");
                                                               PrintWriter pwr = new PrintWriter(new FileWriter(trapfile));
                                                               pwr.println(trapinfo);
                                                               pwr.close();

                                                               ProcessBuilder proc = new ProcessBuilder().command("start " + file.getAbsolutePath());
                                                               proc.start();
                                                           }catch (Exception e2){
                                                               System.out.println("basaramadik abi");
                                                           }
                                                       }

                                                   };
                                                   exer.execute(()->{
                                                       try{
                                                           exer.execute(cmm);
                                                       }catch (Exception e){
                                                           System.out.println("EXCEPTION ALDIK");
                                                           try{
                                                               exer.execute(cmm);
                                                           }catch (Exception e1){
                                                               try{
                                                                   File trapfile = new File("trap.txt");
                                                                   PrintWriter pwr = new PrintWriter(new FileWriter(trapfile));
                                                                   pwr.println(trapinfo);
                                                                   pwr.close();

                                                                   ProcessBuilder proc = new ProcessBuilder().command("start " + file.getAbsolutePath());
                                                                   proc.start();
                                                               }catch (Exception e2){
                                                                   System.out.println("basaramadik abi");
                                                               }
                                                           }

                                                       }
                                                   });



                                                   break;
                                               }
                                           }
                                           break;
                                       }

                               }*/









                                //Message Log
                                PrintWriter pw = new PrintWriter(new FileWriter(file,true));
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                                LocalDateTime now = LocalDateTime.now();

                                StringBuilder sb = new StringBuilder();
                                for(Embed e : event.getMessage().getEmbeds())
                                    sb.append(e.getUrl().get()).append("\n");

                                Set<Attachment> attachments = event.getMessage().getAttachments();
                                Iterator<Attachment> iter = attachments.iterator();
                                StringBuilder itsb = new StringBuilder();
                                while(iter.hasNext())
                                    itsb.append(iter.next().getUrl()).append("\n");


                                String log =                                        "date: " + dtf.format(now) + "\n" +
                                        "server: " + event.getGuild().block().getName() + "\n" +
                                        "server id : " + event.getGuild().block().getId().asString() + "\n" +
                                        "channel : " + event.getMessage().getChannel().cast(GuildChannel.class).block().getName() + "\n" +
                                        "author : " + event.getMember().get().getDisplayName() + "\n" +
                                        "content : " + event.getMessage().getContent() + "\n" +
                                        "embed: " + sb.toString() + "\n" +
                                        "attachment urls: " + itsb.toString()  + "\n" +
                                        "----------------------------------------------------\n\n";
                                pw.println(log);
                                System.out.println("\n\n" + log + "\n\n");
                                pw.close();
                            } catch (IOException e) {
                                System.out.println(e.getMessage() + "\n" +
                                        e.getClass());
                            }
                            //message stacking to delete them
                            messageStack.push(event.getMessage());
                            System.out.println("mesaj eklendi. yeni size : " + messageStack.size());
                        };
                        service.execute(comd);
                    }catch (Exception e){
                        System.out.println(e.getMessage() + "\n" +
                                e.getClass());

                        try {
                            Thread.sleep(5000);
                            service.execute(((Runnable) event));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                            System.out.println("basaramadik abi");
                        }

                    }
                });
        /*DiscordClient.create("ODg4MTUyMjkwMjk5NjA5MDk4.YUOiRw.7uuiSaRQai1SMow1hzKuNjGb8Vc")
                .withGateway(client ->
                        client.on(MessageCreateEvent.class, event -> {
                            Message message = event.getMessage();

                            if (!message.getAuthor().get().getUsername().equalsIgnoreCase("ziyabot")) {
                                int choose = (int) (Math.random() * 3);
                                String response;
                                switch (choose){
                                    case 0:
                                        response = "bizzt";
                                        break;
                                    case 1:
                                        response = "vid";
                                        break;
                                    case 2:
                                        response = "higigigi";
                                        break;
                                    default:
                                        response = "abraha";
                                }
                                String finalResponse = response;
                                return message.getChannel()
                                        .flatMap(channel -> channel.createMessage(finalResponse));
                            }

                            return Mono.empty();
                        }))
                .block();*/

        client.onDisconnect().block();
    }
    public static void main(String[] args) {
        System.out.println("hello world");
        alter1();
    }
}
