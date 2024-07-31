package com.lotusacademy.Managers;

import com.lotusacademy.Handlers.ChannelHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TicketManager {
    private TextChannel lastCreatedChannel;
    private static final int TICKET_ID_LENGTH = 4;
    private static final String TICKET_INFO_PATH = "./tickets/";
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketManager.class);
    private final Set<String> creatingTickets = ConcurrentHashMap.newKeySet();

    public void createTicket(User user, Guild guild, EmbedBuilder embed, String categoryPrefix, ButtonInteractionEvent event) {
        createTicket(user, guild, embed, categoryPrefix, event, null);
    }

    public void createTicket(User user, Guild guild, EmbedBuilder embed, String categoryPrefix, ModalInteractionEvent event) {
        createTicket(user, guild, embed, categoryPrefix, null, event);
    }

    private void createTicket(User user, Guild guild, EmbedBuilder embed, String categoryPrefix, ButtonInteractionEvent buttonEvent, ModalInteractionEvent modalEvent) {
        String userId = user.getId();
        if (creatingTickets.contains(userId)) {
            sendErrorReply(buttonEvent, modalEvent, "A ticket is already being created for you. Please wait.");
            return;
        }

        creatingTickets.add(userId);

        Member member = guild.getMember(user);
        if (member == null) {
            creatingTickets.remove(userId);
            sendErrorReply(buttonEvent, modalEvent, "An error occurred while fetching your guild information.");
            return;
        }

        Category category = guild.getCategoriesByName("Support", true).stream().findFirst().orElse(null);

        Consumer<String> ticketIdConsumer = ticketId -> {
            creatingTickets.remove(userId);
            sendSuccessReply(buttonEvent, modalEvent, user, guild, categoryPrefix, ticketId);
        };

        Consumer<Throwable> onError = error -> {
            creatingTickets.remove(userId);
            sendErrorReply(buttonEvent, modalEvent, "An error occurred while creating the ticket.");
        };

        if (category == null) {
            guild.createCategory("Support").queue(createdCategory -> {
                createTicketChannel(member, embed, createdCategory, categoryPrefix, ticketIdConsumer, onError);
            }, onError);
        } else {
            createTicketChannel(member, embed, category, categoryPrefix, ticketIdConsumer, onError);
        }
    }

    private void createTicketChannel(Member member, EmbedBuilder embed, Category category, String categoryPrefix, Consumer<String> ticketIdConsumer, Consumer<Throwable> onError) {
        Guild guild = member.getGuild();
        String ticketId = generateTicketId();
        String channelName = categoryPrefix + "-" + shortenUsername(member.getUser().getName()) + "-" + ticketId;
        guild.createTextChannel(channelName, category)
                .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)) // Deny everyone else
                .addPermissionOverride(guild.getSelfMember(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY), null) // Allow the bot
                .addPermissionOverride(guild.getRoleById(1247102548570013781L), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS), null) // Allow the Staff Team
                .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS), null) // Allow the member
                .queue(channel -> {
                    this.lastCreatedChannel = channel;
                    storeTicketInfo(member.getId(), categoryPrefix, member.getUser().getName(), ticketId);
                    channel.sendMessageEmbeds(embed.build())
                            .setActionRow(
                                    net.dv8tion.jda.api.interactions.components.buttons.Button.secondary("lock-ticket", "Lock"),
                                    net.dv8tion.jda.api.interactions.components.buttons.Button.success("unlock-ticket", "Unlock"),
                                    net.dv8tion.jda.api.interactions.components.buttons.Button.danger("close-ticket", "Close")
                            ).queue();
                    ticketIdConsumer.accept(ticketId); // Use ticketId instead of channelId
                }, onError);
    }

    private String generateTicketId() {
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder ticketId = new StringBuilder(TICKET_ID_LENGTH);
        for (int i = 0; i < TICKET_ID_LENGTH; i++) {
            ticketId.append(characters.charAt(random.nextInt(characters.length())));
        }
        return ticketId.toString();
    }

    private String shortenUsername(String username) {
        return username.length() > 10 ? username.substring(0, 10) : username;
    }

    public String getLastCreatedTicketId() {
        return lastCreatedChannel != null ? lastCreatedChannel.getId() : null;
    }

    private void storeTicketInfo(String userId, String categoryPrefix, String username, String ticketId) {
        String ticketInfo = "User ID: " + userId + "\nCategory: " + categoryPrefix + "\nUsername: " + username + "\nTicket ID: " + ticketId + "\n";
        Path directoryPath = Paths.get(TICKET_INFO_PATH);
        try {
            Files.createDirectories(directoryPath); // Create the directories if they do not exist
            Path filePath = Paths.get(TICKET_INFO_PATH + userId + "_" + ticketId + ".yml");
            if (!Files.exists(filePath)) {
                Files.write(filePath, ticketInfo.getBytes());
            } else {
                LOGGER.info("Ticket file already exists.");
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating the ticket file.", e);
        }
    }

    private void logTicketCreation(Guild guild, User user, String ticketId, String messageLink) {
        EmbedBuilder logEmbed = new EmbedBuilder()
                .setTitle("Ticket Created")
                .setDescription("A ticket has been created.")
                .setColor(Color.GREEN)
                .setTimestamp(java.time.Instant.now());
        logEmbed.addField("Ticket ID", ticketId, false);
        logEmbed.addField("Ticket Link", "[Click here](" + messageLink + ")", false);
        logEmbed.setFooter("Ticket created by " + user.getName(), user.getAvatarUrl());

        ChannelHandler.sendLogEmbed(logEmbed);
    }

    private void sendErrorReply(ButtonInteractionEvent buttonEvent, ModalInteractionEvent modalEvent, String message) {
        if (buttonEvent != null && !buttonEvent.isAcknowledged()) {
            buttonEvent.reply(message).setEphemeral(true).queue();
        } else if (modalEvent != null && !modalEvent.isAcknowledged()) {
            modalEvent.reply(message).setEphemeral(true).queue();
        }
    }

    private void sendSuccessReply(ButtonInteractionEvent buttonEvent, ModalInteractionEvent modalEvent, User user, Guild guild, String categoryPrefix, String ticketId) {
        String messageLink = "https://discord.com/channels/" + guild.getId() + "/" + lastCreatedChannel.getId();
        EmbedBuilder successEmbed = new EmbedBuilder()
                .setTitle("Ticket Created")
                .setDescription("Your ticket has been created. Click [here](" + messageLink + ") to view it.")
                .setFooter("Ticket created by " + user.getName())
                .setTimestamp(java.time.Instant.now());
        if (buttonEvent != null && !buttonEvent.isAcknowledged()) {
            buttonEvent.replyEmbeds(successEmbed.build()).setEphemeral(true).queue();
        } else if (modalEvent != null && !modalEvent.isAcknowledged()) {
            modalEvent.replyEmbeds(successEmbed.build()).setEphemeral(true).queue();
        }
        logTicketCreation(guild, user, ticketId, messageLink);
    }

    public void closeTicket(TextChannel channel) {
        String channelName = channel.getName();
        if (!isTicketChannel(channel)) {
            return;
        }
        channel.sendMessage("This channel will be closed in 3 seconds").queue();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore the interrupted status
            LOGGER.error("Thread was interrupted", e);
        }
        EmbedBuilder embed = getEmbed("Ticket Closed | " + channelName, "This ticket was closed by " + channel.getGuild().getSelfMember().getUser().getAsTag())
                .setFooter("Ticket closed at " + java.time.Instant.now())
                .setAuthor(channel.getGuild().getSelfMember().getUser().getAsTag(), null, channel.getGuild().getSelfMember().getUser().getAvatarUrl())
                .setThumbnail(channel.getGuild().getSelfMember().getUser().getAvatarUrl());
        channel.delete().queue();

        // Extract ticket ID and delete the ticket file
        String[] parts = channelName.split("-");
        if (parts.length == 3) {
            String ticketId = parts[2];
            Path filePath = Paths.get(TICKET_INFO_PATH + channel.getGuild().getSelfMember().getId() + "_" + ticketId + ".yml");
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                LOGGER.error("An error occurred while deleting the ticket file.", e);
            }

            // Send log message to the log channel
            EmbedBuilder logEmbed = new EmbedBuilder()
                    .setTitle("Ticket Closed")
                    .setDescription("A ticket has been closed.")
                    .setColor(Color.RED)
                    .setTimestamp(java.time.Instant.now());
            logEmbed.addField("Ticket ID", ticketId, false);
            logEmbed.setFooter("Ticket closed by " + channel.getGuild().getSelfMember().getUser().getAsTag(), channel.getGuild().getSelfMember().getUser().getAvatarUrl());

            ChannelHandler.sendLogEmbed(logEmbed);
        }
    }

    public boolean isTicketChannel(TextChannel channel) {
        String[] parts = channel.getName().split("-");
        return parts.length == 3 && parts[2].length() == TICKET_ID_LENGTH;
    }

    public void confirmCloseTicket(TextChannel channel) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Close Ticket")
                .setDescription("Are you sure you want to close this ticket?")
                .setColor(Color.RED);

        channel.sendMessageEmbeds(embedBuilder.build()).queue(message -> {
            message.addReaction(Emoji.fromUnicode("✅")).queue();
            message.addReaction(Emoji.fromUnicode("❌")).queue();
        });
    }

    public EmbedBuilder getEmbed(String title, String description) {
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(title);
        embed.setColor(0xc200eb);
        if (description != null) {
            embed.setDescription(description);
        }
        return embed;
    }

    public Guild getGuild() {
        return lastCreatedChannel.getGuild();
    }
}
