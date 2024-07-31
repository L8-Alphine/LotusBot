package com.lotusacademy.Events;

import com.lotusacademy.Managers.AfkManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class AfkEventListener extends ListenerAdapter {

    private static final long AFK_BACK_CHANNEL_ID = 1243374523751858238L; // Channel ID where the AFK back message should be sent
    private final AfkManager afkManager;

    public AfkEventListener(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = event.getAuthor();

        // Check if the author is back and remove AFK status
        if (afkManager.isAfk(author)) {
            afkManager.removeAfk(author);
            // Notify the user they are no longer AFK in the specified channel
            TextChannel afkBackChannel = event.getJDA().getTextChannelById(AFK_BACK_CHANNEL_ID);
            if (afkBackChannel != null) {
                afkBackChannel.sendMessage(author.getAsMention() + " is no longer AFK.").queue();
            }
        }

        // Check if any AFK users are mentioned directly (not via role or @everyone/@here)
        List<User> mentionedUsers = message.getMentions().getUsers();
        if (!mentionedUsers.isEmpty()) {
            for (User mentionedUser : mentionedUsers) {
                if (afkManager.isAfk(mentionedUser)) {
                    AfkManager.AfkInfo afkInfo = afkManager.getAfkInfo(mentionedUser);

                    // Notify the author that the mentioned user is AFK
                    EmbedBuilder embedBuilder = new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle("Users is AFK")
                            .setDescription(mentionedUser.getName() + " is currently AFK with the message: '" + afkInfo.getMessage() + "'.")
                            .setThumbnail(mentionedUser.getAvatarUrl())
                            .setFooter("AFK since: " + afkInfo.getTimestamp());

                    event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();

                    // Send a private message to the mentioned user
                    EmbedBuilder dmEmbed = new EmbedBuilder()
                            .setColor(Color.YELLOW)
                            .setTitle("You were pinged while AFK")
                            .setDescription(author.getAsMention() + " tried to ping you in " + event.getChannel().getAsMention() + " with the message: '" + afkInfo.getMessage() + "'.")
                            .setThumbnail(author.getAvatarUrl());

                    mentionedUser.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(dmEmbed.build()).queue());

                    // Delete the message if the user was mentioned directly
                    if (message.getMentions().getRoles().isEmpty() && !message.getMentions().mentionsEveryone()) {
                        message.delete().queue();
                    }
                }
            }
        }
    }

    @Override
    public void onUserUpdateOnlineStatus(@NotNull UserUpdateOnlineStatusEvent event) {
        User user = event.getUser();
        if (afkManager.isAfk(user)) {
            afkManager.removeAfk(user);
            // Notify the user they are no longer AFK in the specified channel
            TextChannel afkBackChannel = event.getJDA().getTextChannelById(AFK_BACK_CHANNEL_ID);
            if (afkBackChannel != null) {
                afkBackChannel.sendMessage(user.getAsMention() + " is no longer AFK.").queue();
            }
        }
    }
}
