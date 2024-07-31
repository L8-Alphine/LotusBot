package com.lotusacademy.Commands.Moderation;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Handlers.ChannelHandler;
import com.lotusacademy.Handlers.DatabaseHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
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
import java.util.concurrent.TimeUnit;

public class UnbanCommand implements ICommand {

    @Override
    public String getName() {
        return "unban";
    }

    @Override
    public String getDescription() {
        return "Unbans a user from the discord server with a reason";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "reason", "The reason for unbanning the member", true));
        options.add(new OptionData(OptionType.STRING, "username", "The username of the member to unban", false));
        options.add(new OptionData(OptionType.STRING, "punishment_id", "The punishment ID of the ban to reverse", false));
        return options;
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Member self = event.getGuild().getSelfMember();

        String username = event.getOption("username") != null ? event.getOption("username").getAsString() : null;
        String punishmentId = event.getOption("punishment_id") != null ? event.getOption("punishment_id").getAsString() : null;
        String reason = event.getOption("reason").getAsString();

        if (!member.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("You don't have permission to unban members.").setEphemeral(true).queue();
            return;
        }

        if (!self.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("The bot doesn't have permission to unban members.").setEphemeral(true).queue();
            return;
        }

        if (punishmentId != null) {
            handleUnbanByPunishmentId(event, punishmentId, reason);
        } else if (username != null) {
            handleUnbanByUsername(event, username, reason);
        } else {
            event.reply("You must provide either a username or a punishment ID to unban a member.").setEphemeral(true).queue();
        }
    }

    private void handleUnbanByPunishmentId(SlashCommandInteractionEvent event, String punishmentId, String reason) {
        CompletableFuture.runAsync(() -> {
            try (ResultSet rs = DatabaseHandler.getPunishmentById(punishmentId)) {
                if (rs != null && rs.next()) {
                    String targetId = rs.getString("user_id");
                    String targetName = rs.getString("username");

                    performUnban(event, targetId, targetName, reason, punishmentId);
                } else {
                    event.getHook().sendMessage("No ban found with the provided punishment ID.").setEphemeral(true).queue();
                }
            } catch (SQLException e) {
                event.getHook().sendMessage("Error retrieving punishment data.").setEphemeral(true).queue();
            }
        });
    }

    private void handleUnbanByUsername(SlashCommandInteractionEvent event, String username, String reason) {
        CompletableFuture.runAsync(() -> {
            try (ResultSet rs = DatabaseHandler.getPunishmentsByUsername(username)) {
                if (rs != null && rs.next()) {
                    String targetId = rs.getString("user_id");
                    String punishmentId = rs.getString("punishment_id");

                    performUnban(event, targetId, username, reason, punishmentId);
                } else {
                    event.getHook().sendMessage("No ban found for the provided username.").setEphemeral(true).queue();
                }
            } catch (SQLException e) {
                event.getHook().sendMessage("Error retrieving punishment data.").setEphemeral(true).queue();
            }
        });
    }

    private void performUnban(SlashCommandInteractionEvent event, String targetId, String targetName, String reason, String punishmentId) {
        event.getGuild().retrieveBanList().queue(banList -> {
            if (banList.stream().anyMatch(ban -> ban.getUser().getId().equals(targetId))) {
                event.getGuild().unban(UserSnowflake.fromId(targetId)).queue(success -> {
                    event.getGuild().retrieveMemberById(targetId).queue(unbannedMember -> {
                        event.getGuild().retrieveInvites().queue(invites -> {
                            String inviteLink = invites.isEmpty() ? event.getGuild().getDefaultChannel().createInvite().setMaxUses(1).setMaxAge(7L, TimeUnit.DAYS).complete().getUrl() : invites.get(0).getUrl();

                            EmbedBuilder unbanEmbed = new EmbedBuilder()
                                    .setTitle("You have been unbanned")
                                    .setColor(Color.GREEN)
                                    .setDescription("You have been unbanned from " + event.getGuild().getName() + ".")
                                    .addField("Unbanned By", event.getUser().getName(), false)
                                    .addField("Reason", reason, false)
                                    .addField("Invite Link", inviteLink, false);

                            unbannedMember.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(unbanEmbed.build()).queue());

                            // Log the unban
                            EmbedBuilder unbanLog = new EmbedBuilder()
                                    .setTitle("Member Unbanned")
                                    .setColor(Color.GREEN)
                                    .setDescription("**Unbanned Member:** " + targetName +
                                            "\n**Unbanned By:** " + event.getUser().getName() +
                                            "\n**Reason:** " + reason)
                                    .setThumbnail(unbannedMember.getAvatarUrl());

                            ChannelHandler.sendLogEmbed(unbanLog);

                            // Remove the punishment from the database
                            DatabaseHandler.removePunishment(punishmentId);

                            event.getHook().sendMessage("Member " + targetName + " has been unbanned.").setEphemeral(true).queue();
                        });
                    });
                });
            } else {
                event.getHook().sendMessage("The user is not banned.").setEphemeral(true).queue();
            }
        });
    }
}
