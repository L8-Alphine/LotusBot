package com.lotusacademy.Events;

import com.lotusacademy.Handlers.LangHandler;
import com.lotusacademy.Managers.ConfigManager;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class WelcomeListener extends ListenerAdapter {

    private final ConfigManager configManager;
    private final LangHandler langManager;
    private final TicketManager ticketManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeListener.class);

    public WelcomeListener(ConfigManager configManager, LangHandler langManager, TicketManager ticketManager) {
        this.configManager = configManager;
        this.langManager = langManager;
        this.ticketManager = ticketManager;
        LOGGER.info("WelcomeListener initialized");
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (!configManager.isWelcomeEnabled()) {
            return;
        }

        Member member = event.getMember();
        Guild guild = event.getGuild();
        String welcomeMessageTemplate = langManager.getMessage("welcomemessage");
        String welcomeChannelId = configManager.getWelcomeChannel();
        String welcomePrivateMessageTemplate = langManager.getMessage("welcomeprivatemessage");

        // Send welcome message
        String welcomeMessage = welcomeMessageTemplate.replace("{user}", member.getAsMention());
        String welcomePrivateMessage = welcomePrivateMessageTemplate
                .replace("{user}", member.getAsMention())
                .replace("{servername}", guild.getName());

        TextChannel welcomeChannel = guild.getTextChannelById(welcomeChannelId);
        if (welcomeChannel == null) {
            LOGGER.error("Welcome channel not found");
            return;
        }

        // Log Member Join event
        logMemberJoin(event);

        // Send the welcome message as a private message to the user
        sendPrivateWelcomeMessage(member, welcomePrivateMessage);

        if (configManager.useWelcomeEmbeds()) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setColor(Color.GREEN) // Green color
                    .setTitle("Welcome " + member.getEffectiveName() + "!")
                    .setDescription(welcomeMessage)
                    .setThumbnail(member.getUser().getAvatarUrl())
                    .setAuthor(guild.getName(), null, guild.getIconUrl())
                    .setTimestamp(event.getMember().getTimeJoined())
                    .setFooter("You are the " + guild.getMemberCount() + "th member!", guild.getIconUrl());
            welcomeChannel.sendMessageEmbeds(embedBuilder.build())
                    .queue(
                            success -> LOGGER.info("Sent welcome embed message to channel"),
                            error -> LOGGER.error("Failed to send welcome embed message to channel: " + error.getMessage())
                    );
        } else {
            welcomeChannel.sendMessage(welcomeMessage).queue(
                    success -> LOGGER.info("Sent welcome message to channel"),
                    error -> LOGGER.error("Failed to send welcome message to channel: " + error.getMessage())
            );
        }
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        if (!configManager.isWelcomeEnabled()) {
            return;
        }

        Member member = event.getMember();
        Guild guild = event.getGuild();

        // Log Member Leave event
        logMemberLeave(event);
    }

    private void sendPrivateWelcomeMessage(Member member, String messageTemplate) {
        EmbedBuilder privateMessageEmbed = new EmbedBuilder()
                .setTitle("Welcome to the Server!")
                .setColor(Color.GREEN) // Green color
                .setDescription(messageTemplate)
                .setFooter("Please verify to gain full access to the server. Go the `#verification` channel to verify yourself.");

        member.getUser().openPrivateChannel().queue(
                privateChannel -> privateChannel.sendMessageEmbeds(privateMessageEmbed.build())
                        .queue(
                                success -> LOGGER.info("Sent private welcome message to user"),
                                error -> LOGGER.error("Failed to send private welcome message to user: " + error.getMessage())
                        )
        );
    }

    private void logMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        String logChannelId = configManager.getLogChannelId();
        TextChannel logChannel = guild.getTextChannelById(logChannelId);

        if (logChannel != null) {
            EmbedBuilder logEmbed = new EmbedBuilder();
            logEmbed.setColor(Color.GREEN) // Green color
                    .setTitle("Member Joined")
                    .setDescription(member.getEffectiveName() + " joined the server")
                    .setThumbnail(member.getUser().getAvatarUrl())
                    .setAuthor(guild.getName(), null, guild.getIconUrl())
                    .setTimestamp(event.getMember().getTimeJoined())
                    .setFooter("Member count: " + guild.getMemberCount(), guild.getIconUrl());
            logChannel.sendMessageEmbeds(logEmbed.build())
                    .queue(
                            success -> LOGGER.info("Sent log embed message to channel"),
                            error -> LOGGER.error("Failed to send log embed message to channel: " + error.getMessage())
                    );
        } else {
            LOGGER.error("Log channel not found");
        }
    }

    private void logMemberLeave(GuildMemberRemoveEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        String logChannelId = configManager.getLogChannelId();
        TextChannel logChannel = guild.getTextChannelById(logChannelId);

        if (logChannel != null) {
            EmbedBuilder logEmbed = new EmbedBuilder();
            logEmbed.setColor(Color.RED)
                    .setTitle("Member Left")
                    .setDescription(member.getEffectiveName() + " left the server")
                    .setThumbnail(member.getUser().getAvatarUrl())
                    .setAuthor(guild.getName(), null, guild.getIconUrl())
                    .setTimestamp(java.time.Instant.now())
                    .setFooter("Member count: " + guild.getMemberCount(), guild.getIconUrl());
            logChannel.sendMessageEmbeds(logEmbed.build())
                    .queue(
                            success -> LOGGER.info("Sent log embed message to channel"),
                            error -> LOGGER.error("Failed to send log embed message to channel: " + error.getMessage())
                    );
        } else {
            LOGGER.error("Log channel not found");
        }
    }
}
