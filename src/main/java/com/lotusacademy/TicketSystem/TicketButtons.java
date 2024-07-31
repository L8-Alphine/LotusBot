package com.lotusacademy.TicketSystem;

import com.lotusacademy.Managers.ConfigManager;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketButtons extends ListenerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketButtons.class);
    private static final ConfigManager configManager = new ConfigManager();
    private static final String SUPPORT_ROLE_ID = configManager.getSupportId();
    private final TicketManager ticketManager = new TicketManager();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId == null) {
            return;
        }

        if (event.getChannel() instanceof PrivateChannel) {
            handlePrivateChannelInteraction(event, buttonId);
        } else {
            TextChannel channel = event.getChannel().asTextChannel();
            handleTextChannelInteraction(event, buttonId, channel);
        }
    }

    private void handlePrivateChannelInteraction(ButtonInteractionEvent event, String buttonId) {
        Guild guild = event.getJDA().getGuildById("1232592412434497577");
        User user = event.getUser();

        if (guild == null) {
            event.reply("Guild not found!").setEphemeral(true).queue();
            return;
        }

        Member member = guild.getMember(user);

        if (member == null) {
            event.reply("Member not found in the guild!").setEphemeral(true).queue();
            return;
        }

        switch (buttonId) {
            case "verify-player":
                event.replyModal(createPlayerModal()).queue();
                break;
            case "verify-parent":
                event.replyModal(createParentModal()).queue();
                break;
            case "verify-scout":
                event.replyModal(createScoutModal()).queue();
                break;
            default:
                event.reply("Invalid verification type!").setEphemeral(true).queue();
                break;
        }
    }

    private Modal createPlayerModal() {
        return Modal.create("verify-player-modal", "Player Verification")
                .addActionRow(TextInput.create("academy-tier", "Select Tier", TextInputStyle.SHORT)
                        .setPlaceholder("Academy Tier 1, Academy Tier 2, Showcases")
                        .setRequired(true)
                        .build())
                .build();
    }

    private Modal createParentModal() {
        return Modal.create("verify-parent-modal", "Parent Verification")
                .addActionRow(TextInput.create("full-name", "Full Name", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("player-name", "Player's Name", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .build();
    }

    private Modal createScoutModal() {
        return Modal.create("verify-scout-modal", "Scout Verification")
                .addActionRow(TextInput.create("full-name", "Full Name", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("college-name", "College Esports Program Name", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("official-role", "Official Role in the Program", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("college-email", "College Email", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .build();
    }

    private void handleTextChannelInteraction(ButtonInteractionEvent event, String buttonId, TextChannel channel) {
        Member member = event.getMember();

        if (SUPPORT_ROLE_ID == null || SUPPORT_ROLE_ID.trim().isEmpty()) {
            LOGGER.error("Support role ID is empty or null");
            return;
        }

        switch (buttonId) {
            case "lock-ticket":
                lockTicket(event, channel, member, SUPPORT_ROLE_ID);
                break;
            case "unlock-ticket":
                unlockTicket(event, channel, member);
                break;
            case "close-ticket":
                event.deferReply(true).queue();
                event.getHook().sendMessage("Sending confirmation message...").queue();
                ticketManager.confirmCloseTicket(channel);
                break;
        }
    }

    private void lockTicket(ButtonInteractionEvent event, TextChannel channel, Member member, String roleId) {
        Role supportRole = channel.getGuild().getRoleById(SUPPORT_ROLE_ID);

        if (roleId == null || roleId.trim().isEmpty()) {
            LOGGER.error("Role ID is empty or null");
            return;
        }

        Role role = channel.getGuild().getRoleById(roleId);
        if (role == null) {
            LOGGER.error("Role not found with ID: {}", roleId);
            return;
        }

        if (supportRole != null) {
            channel.upsertPermissionOverride(supportRole).grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND).queue();
        }
        channel.upsertPermissionOverride(member).deny(Permission.MESSAGE_SEND).queue();
        channel.sendMessage("Ticket has been locked. Only support can type now.").queue();
        event.reply("Ticket has been locked. Only support can type now.").setEphemeral(true).queue();
    }

    private void unlockTicket(ButtonInteractionEvent event, TextChannel channel, Member member) {
        // Get the support role by ID
        Role supportRole = channel.getGuild().getRoleById(SUPPORT_ROLE_ID);

        // Revoke view and send message permissions for everyone
        channel.upsertPermissionOverride(channel.getGuild().getPublicRole())
                .deny(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                .queue();

        // Grant view and send message permissions to the support role
        if (supportRole != null) {
            channel.upsertPermissionOverride(supportRole)
                    .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                    .queue();
        }

        // Grant view and send message permissions to the member who created the ticket
        channel.upsertPermissionOverride(member)
                .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                .queue();

        // Send a message to the channel indicating that it has been unlocked
        channel.sendMessage("Ticket has been unlocked. You can type now.").queue();
        event.reply("Ticket has been unlocked. You can type now.").setEphemeral(true).queue();
    }
}
