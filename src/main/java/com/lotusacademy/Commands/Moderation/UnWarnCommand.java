package com.lotusacademy.Commands.Moderation;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Handlers.ChannelHandler;
import com.lotusacademy.Handlers.DatabaseHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UnWarnCommand implements ICommand {
    @Override
    public String getName() {
        return "unwarn";
    }

    @Override
    public String getDescription() {
        return "Removes a warning from the target user via punishment ID.";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "punishment_id", "The punishment ID to remove", true));
        return options;
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("You don't have permission to unwarn members.").setEphemeral(true).queue();
            return;
        }

        OptionMapping punishmentIdOption = event.getOption("punishment_id");
        if (punishmentIdOption == null) {
            event.reply("You must provide a punishment ID to unwarn a member.").setEphemeral(true).queue();
            return;
        }

        String punishmentId = punishmentIdOption.getAsString();

        // Defer reply to avoid interaction expiration
        event.deferReply().queue();

        // Execute database operations asynchronously
        CompletableFuture.runAsync(() -> {
            try (ResultSet rs = DatabaseHandler.getPunishmentById(punishmentId)) {
                if (rs != null && rs.next()) {
                    String userId = rs.getString("user_id");
                    String username = rs.getString("username");
                    String reason = rs.getString("reason");

                    // Remove the punishment from the database
                    DatabaseHandler.removePunishment(punishmentId);

                    // Send a private message to the unwarned user
                    event.getGuild().retrieveMemberById(userId).queue(member -> {
                        EmbedBuilder dmEmbed = new EmbedBuilder()
                                .setTitle("Your warning has been removed")
                                .setColor(Color.YELLOW)
                                .addField("Removed By", event.getUser().getName(), false)
                                .addField("Punishment ID", punishmentId, false)
                                .addField("Original Reason", reason, false);

                        member.getUser().openPrivateChannel().queue(channel -> {
                            channel.sendMessageEmbeds(dmEmbed.build()).queue();
                        });

                        // Log the unwarn action
                        EmbedBuilder logEmbed = new EmbedBuilder()
                                .setTitle("Warning Removed")
                                .setColor(Color.YELLOW)
                                .addField("Member", username, false)
                                .addField("Removed By", event.getUser().getName(), false)
                                .addField("Punishment ID", punishmentId, false)
                                .addField("Original Reason", reason, false)
                                .setThumbnail(member.getAvatarUrl());

                        ChannelHandler.sendLogEmbed(logEmbed);

                        // Send a confirmation to the command invoker
                        event.getHook().sendMessage("Removed warning from " + username + ".").setEphemeral(true).queue();
                    });
                } else {
                    event.getHook().sendMessage("No punishment found with the provided ID.").setEphemeral(true).queue();
                }
            } catch (SQLException e) {
                event.getHook().sendMessage("An error occurred while removing the punishment.").setEphemeral(true).queue();
            }
        });
    }
}
