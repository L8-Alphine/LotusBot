package com.lotusacademy;

import com.lotusacademy.Commands.Basic.*;
import com.lotusacademy.Commands.Fun.GameCommand;
import com.lotusacademy.Commands.Fun.MemeCommand;
import com.lotusacademy.Commands.Fun.SlapCommand;
import com.lotusacademy.Commands.Moderation.*;
import com.lotusacademy.Commands.Verify.ScheduleCommand;
import com.lotusacademy.Commands.Verify.VerifyCommand;
import com.lotusacademy.ConsoleCommands.Power;
import com.lotusacademy.Events.ModerationEventListener;
import com.lotusacademy.Events.WelcomeListener;
import com.lotusacademy.Handlers.ChannelHandler;
import com.lotusacademy.Handlers.DatabaseHandler;
import com.lotusacademy.Handlers.LangHandler;
import com.lotusacademy.Managers.*;
import com.lotusacademy.TicketSystem.ReactionInteraction;
import com.lotusacademy.TicketSystem.TicketAdmin;
import com.lotusacademy.TicketSystem.TicketButtons;
import com.lotusacademy.VerificationSystem.VerificationSystem;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LotusBot extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LotusBot.class);
    private static int statusIndex = 0;

    public static void main(String[] args) {

        AfkManager afkManager = new AfkManager();
        BanCommand banCommand = new BanCommand();
        TicketManager ticketManager = new TicketManager();
        VoiceChannelManager voiceChannelManager = new VoiceChannelManager();
        DatabaseHandler.initializeDatabase();

        try {
            // Loading the configuration and initializing the command manager
            ConfigManager configManager = new ConfigManager();
            String token = configManager.getToken();
            LangHandler langHandler = new LangHandler(configManager.getLanguageFile());
            List<List<String>> statuses = configManager.getStatuses();
            CommandManager command = CommandManager.getInstance();

            // Basic Commands
            command.add(new AFKCommand(afkManager));
            command.add(new NexxaWaveCommand());
            command.add(new VoiceCommand(voiceChannelManager));
            command.add(new GameCommand());
            command.add(new MemeCommand());
            command.add(new SlapCommand());

            // Ticket Commands
            command.add(new AddUserCommand());
            command.add(new RemoveUserCommand());
            command.add(new CloseCommand());
            command.add(new PingTicketStaffCommand());

            // Moderation Commands
            command.add(new BanCommand());
            command.add(new UnbanCommand());
            command.add(new PunishmentsCommand());
            command.add(new WarnCommand());
            command.add(new UnWarnCommand());
            command.add(new KickCommand());
            command.add(new ClearCommand());

            // Verification
            command.add(new VerifyCommand(ticketManager));
            command.add(new ScheduleCommand());

            // Starting the JDA
            LOGGER.info("Initializing LotusBot...");
            JDA jda = JDABuilder.createDefault(token)
                    .enableCache(CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY, CacheFlag.MEMBER_OVERRIDES, CacheFlag.ROLE_TAGS)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(
                            command,
                            new LotusBot(),
                            new WelcomeListener(configManager, langHandler, ticketManager),
                            new VerificationSystem(ticketManager),
                            new ReactionInteraction(),
                            new VoiceChannelManager(),
                            new TicketAdmin(),
                            new TicketButtons(),
                            new ModerationEventListener(banCommand)
                    )
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build();
            jda.awaitReady();
            LOGGER.info("LotusBot Initialized and Ready!");

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                if (!statuses.isEmpty()) {
                    List<String> status = statuses.get(statusIndex);
                    LOGGER.info("Setting status to: " + status.get(0) + " " + status.get(1));
                    Activity activity = switch (status.get(0).toUpperCase()) {
                        case "PLAYING" -> Activity.playing(status.get(1));
                        case "WATCHING" -> Activity.watching(status.get(1));
                        case "LISTENING" -> Activity.listening(status.get(1));
                        default -> Activity.playing(status.get(1));
                    };
                    jda.getPresence().setActivity(activity);
                    statusIndex = (statusIndex + 1) % statuses.size();
                }
            }, 0, 30, TimeUnit.SECONDS);

            // Console command for stopping the bot
            Power power = new Power(jda);
            power.start();

            ChannelHandler channelHanndler = new ChannelHandler(jda);
            channelHanndler.getInstance(jda);

        } catch (IllegalArgumentException e) {
            LOGGER.error("Configuration error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred: ", e);
        }
    }
}
