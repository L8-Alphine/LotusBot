package com.lotusacademy.Commands.Moderation;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Handlers.ChannelHandler;
import com.lotusacademy.Handlers.DatabaseHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class BanCommand implements ICommand {

    @Override
    public String getName() {
        return "ban";
    }

    @Override
    public String getDescription() {
        return "Bans a user from the discord server with a reason and the amount of messages to delete";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.USER, "member", "The member that's gonna get banned", true));
        options.add(new OptionData(OptionType.STRING, "reason", "The reason you're banning the member", true));
        options.add(new OptionData(OptionType.INTEGER, "msg_del", "The amount of days you want to go back and delete the member's messages", true));
        return options;
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Member target = event.getOption("member").getAsMember();
        Member self = event.getGuild().getSelfMember();

        OptionMapping deleteMessage = event.getOption("msg_del");
        int deleteMessagesInt = deleteMessage.getAsInt();
        String reason = event.getOption("reason").getAsString();

        if (!member.canInteract(target) || !member.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("You don't have permission to ban members.").setEphemeral(true).queue();
            return;
        }

        if (!self.canInteract(target) || !self.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("I don't have permission to ban this member.").setEphemeral(true).queue();
            return;
        }

        String appealId = generateAppealId();

        // Defer reply to avoid interaction expiration
        event.deferReply().queue();

        // Send a private message to the banned user with a button to appeal
        EmbedBuilder banEmbed = new EmbedBuilder()
                .setTitle("You have been banned")
                .setColor(Color.RED)
                .setDescription("You have been banned from " + event.getGuild().getName() + ".")
                .addField("Reason", reason, false)
                .addField("Appeal ID", appealId, false);
        Button appealButton = Button.link("https://lotus8academy.com/appeals/", "Appeal Ban");
        target.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(banEmbed.build()).setActionRow(appealButton).queue());

        // Ban the member
        event.getGuild()
                .ban(target, deleteMessagesInt, TimeUnit.DAYS)
                .reason(reason)
                .queue();

        // Store punishment in the database asynchronously
        CompletableFuture.runAsync(() -> {
            DatabaseHandler.addPunishment(target.getEffectiveName(), target.getId(), appealId, reason);

            // Log the ban with an unban button
            String banString = "**Banned By:** " + member.getEffectiveName() +
                    "\n" +
                    "\n" +
                    "**Banned Member:** " + target.getEffectiveName() +
                    "\n" +
                    "\n" +
                    "**Reason:** " +
                    "```" + reason + "```" +
                    "\n" +
                    "\n" +
                    "**Deleted Messages:** " + deleteMessagesInt + " Days" +
                    "\n" +
                    "\n" +
                    "**Appeal ID:** " + appealId;

            EmbedBuilder banEmbedLog = new EmbedBuilder()
                    .setTitle("Member Banned")
                    .setThumbnail(target.getAvatarUrl())
                    .setColor(Color.RED)
                    .setDescription(banString);

            Button unbanButton = Button.success("unban:" + appealId, "Unban " + target.getEffectiveName());

            ChannelHandler.sendLogEmbedButton(banEmbedLog, unbanButton);

            // Send a confirmation to the command invoker
            event.getHook().sendMessage("You banned " + target.getEffectiveName() + " from " + event.getGuild().getName())
                    .setEphemeral(true)
                    .queue();
        });
    }

    private String generateAppealId() {
        return UUID.randomUUID().toString();
    }

    public void handleUnban(ButtonInteractionEvent event) {
        if (event.getButton().getId().startsWith("unban:")) {
            String punishmentId = event.getButton().getId().split(":")[1];

            // Check if the user has the BanMembers permission
            if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                event.reply("You don't have permission to unban members.").setEphemeral(true).queue();
                return;
            }

            // Retrieve punishment information from the database asynchronously
            CompletableFuture.runAsync(() -> {
                try (ResultSet rs = DatabaseHandler.getPunishmentById(punishmentId)) {
                    if (rs != null && rs.next()) {
                        String targetId = rs.getString("user_id");
                        String targetName = rs.getString("username");

                        // Show a modal to confirm the unban
                        Modal modal = Modal.create("unban-modal", "Unban Member")
                                .addActionRow(TextInput.create("unban_reason", "Reason for Unban", TextInputStyle.SHORT).setRequired(true).build())
                                .addActionRow(TextInput.create("target_id", "Target ID", TextInputStyle.SHORT).setValue(targetId).setRequired(true).build())
                                .addActionRow(TextInput.create("target_name", "Target Name", TextInputStyle.SHORT).setValue(targetName).setRequired(true).build())
                                .addActionRow(TextInput.create("punishment_id", "Punishment ID", TextInputStyle.SHORT).setValue(punishmentId).setRequired(true).build())
                                .build();

                        event.replyModal(modal).queue();
                    } else {
                        event.reply("Error retrieving punishment data.").setEphemeral(true).queue();
                    }
                } catch (SQLException e) {
                    event.reply("Error retrieving punishment data.").setEphemeral(true).queue();
                }
            });
        }
    }

    public void handleUnbanModal(ModalInteraction event) {
        if (event.getModalId().equals("unban-modal")) {
            String targetId = event.getValue("target_id").getAsString();
            String unbanReason = event.getValue("unban_reason").getAsString();
            String punishmentId = event.getValue("punishment_id").getAsString();

            event.getGuild().unban(UserSnowflake.fromId(targetId)).queue(success -> {
                // Send a private message with an invite link to the unbanned user
                event.getGuild().retrieveMemberById(targetId).queue(unbannedMember -> {
                    event.getGuild().retrieveInvites().queue(invites -> {
                        String inviteLink = invites.isEmpty() ? event.getGuild().getDefaultChannel().createInvite().setMaxUses(1).setMaxAge(7L, TimeUnit.DAYS).complete().getUrl() : invites.get(0).getUrl();

                        EmbedBuilder unbanEmbed = new EmbedBuilder()
                                .setTitle("You have been unbanned")
                                .setColor(Color.GREEN)
                                .setDescription("You have been unbanned from " + event.getGuild().getName() + ".")
                                .addField("Unbanned By", event.getUser().getName(), false)
                                .addField("Reason", unbanReason, false)
                                .addField("Invite Link", inviteLink, false);

                        unbannedMember.getUser().openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(unbanEmbed.build()).queue());
                    });

                    // Log the unban
                    EmbedBuilder unbanLog = new EmbedBuilder()
                            .setTitle("Member Unbanned")
                            .setColor(Color.GREEN)
                            .setDescription("**Unbanned Member:** " + unbannedMember.getEffectiveName() +
                                    "\n**Unbanned By:** " + event.getUser().getName() +
                                    "\n**Reason:** " + unbanReason)
                            .setThumbnail(unbannedMember.getAvatarUrl());

                    ChannelHandler.sendLogEmbed(unbanLog);

                    // Remove the punishment from the database
                    DatabaseHandler.removePunishment(punishmentId);

                    event.reply("Member " + unbannedMember.getEffectiveName() + " has been unbanned.").setEphemeral(true).queue();
                });
            });
        }
    }
}
