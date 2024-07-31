package com.lotusacademy.VerificationSystem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VerificationSystem extends ListenerAdapter {

    private final TicketManager ticketManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationSystem.class);
    private static final String SCHEDULE_FILE = "scheduleData.json";
    private static final long STAFF_ROLE_ID = 1247102548570013781L;

    public VerificationSystem(TicketManager ticketManager) {
        this.ticketManager = ticketManager;
        LOGGER.info("VerificationSystem initialized");
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith("verify-")) {
            String roleType = componentId.split("-")[1];
            Modal modal = createVerificationModal(roleType);
            event.replyModal(modal).queue();
        }
    }

    private Modal createVerificationModal(String roleType) {
        switch (roleType) {
            case "player":
                return Modal.create("verify-player-modal", "Player Verification")
                        .addActionRow(TextInput.create("academy-tier", "Select Tier", TextInputStyle.SHORT)
                                .setPlaceholder("Academy Tier 1, Academy Tier 2, Showcases")
                                .setRequired(true)
                                .build())
                        .build();
            case "scout":
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
            case "parent":
                return Modal.create("verify-parent-modal", "Parent Verification")
                        .addActionRow(TextInput.create("full-name", "Full Name", TextInputStyle.SHORT)
                                .setRequired(true)
                                .build())
                        .addActionRow(TextInput.create("player-name", "Player's Name", TextInputStyle.SHORT)
                                .setRequired(true)
                                .build())
                        .addActionRow(TextInput.create("parent-email", "Parent Email", TextInputStyle.SHORT)
                                .setRequired(true)
                                .build())
                        .build();
            default:
                return null;
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        Guild guild = event.getGuild();

        if (guild == null) {
            event.reply("This action can only be performed within a server.").setEphemeral(true).queue();
            return;
        }

        Member member = guild.getMember(UserSnowflake.fromId(event.getUser().getIdLong()));

        if (member == null) {
            event.reply("An error occurred while fetching your guild information.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl())
                .setThumbnail(event.getUser().getAvatarUrl())
                .setFooter("Verification Ticket created by " + event.getUser().getName())
                .setTimestamp(event.getTimeCreated());

        switch (modalId) {
            case "verify-player-modal":
                String academyTier = event.getValue("academy-tier").getAsString();
                embed.setTitle("Player Verification")
                        .setDescription("Academy Tier: " + academyTier);
                ticketManager.createTicket(event.getUser(), guild, embed, "player", event);
                break;
            case "verify-scout-modal":
                String fullName = event.getValue("full-name").getAsString();
                String collegeName = event.getValue("college-name").getAsString();
                String officialRole = event.getValue("official-role").getAsString();
                String collegeEmail = event.getValue("college-email").getAsString();
                embed.setTitle("Scout Verification")
                        .addField("Full Name", fullName, true)
                        .addField("College Esports Program Name", collegeName, true)
                        .addField("Official Role", officialRole, true)
                        .addField("College Email", collegeEmail, true);

                // Create the ticket and store the ticket channel ID
                ticketManager.createTicket(event.getUser(), guild, embed, "scout", event);

                // Schedule a task to send the schedule times selection message
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        sendScheduleTimeSelection(event.getUser().getIdLong());
                    }
                }, 2000); // Delay for 2 seconds

                break;
            case "verify-parent-modal":
                String parentName = event.getValue("full-name").getAsString();
                String playerName = event.getValue("player-name").getAsString();
                String parentEmail = event.getValue("parent-email").getAsString();
                embed.setTitle("Parent Verification")
                        .addField("Full Name", parentName, true)
                        .addField("Player's Name", playerName, true)
                        .addField("Parent Email", parentEmail, true);
                ticketManager.createTicket(event.getUser(), guild, embed, "parent", event);
                break;
            default:
                event.reply("Unknown modal interaction.").setEphemeral(true).queue();
                break;
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("schedule-select")) {
            String selectedOption = event.getSelectedOptions().get(0).getLabel();
            Member member = event.getMember();

            if (member == null) {
                event.reply("An error occurred while fetching your information.").setEphemeral(true).queue();
                return;
            }

            Guild guild = event.getGuild();
            if (guild == null) {
                event.reply("An error occurred while fetching the guild information.").setEphemeral(true).queue();
                return;
            }

            TextChannel ticketChannel = (TextChannel) event.getChannel();

            ticketChannel.sendMessage("<@&" + STAFF_ROLE_ID + ">").queue();
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription(member.getAsMention() + " selected " + selectedOption + " as the scheduled showcase.")
                    .setColor(Color.BLUE);

            ticketChannel.sendMessageEmbeds(embed.build()).queue();

            event.reply("You have selected " + selectedOption + " as your scheduled showcase.").setEphemeral(true).queue();
        }
    }

    private void sendScheduleTimeSelection(long userId) {
        String ticketChannelId = ticketManager.getLastCreatedTicketId();
        if (ticketChannelId == null) {
            LOGGER.error("Ticket channel ID not found for user: " + userId);
            return;
        }

        Guild guild = ticketManager.getGuild();
        if (guild == null) {
            LOGGER.error("Guild is null");
            return;
        }

        TextChannel ticketChannel = guild.getTextChannelById(ticketChannelId);
        if (ticketChannel == null) {
            LOGGER.error("Ticket channel not found for ID: " + ticketChannelId);
            return;
        }

        Member member = guild.getMemberById(userId);
        if (member == null) {
            LOGGER.error("Member not found for user: " + userId);
            return;
        }

        List<String> scheduleOptions = getScheduleOptions();
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("schedule-select")
                .setPlaceholder("Select Showcase Time that is Scheduled")
                .setRequiredRange(1, 1);

        for (int i = 0; i < scheduleOptions.size(); i++) {
            menuBuilder.addOption((i + 1) + ". " + scheduleOptions.get(i), String.valueOf(i + 1));
        }

        StringSelectMenu menu = menuBuilder.build();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Schedule Time Selection")
                .setDescription(member.getAsMention() + ", please select a time for a showcase schedule:")
                .setColor(Color.BLUE)
                .setFooter(null)
                .setTimestamp(null);

        ticketChannel.sendMessageEmbeds(embed.build())
                .setActionRow(menu)
                .queue();

        LOGGER.info("Sent schedule time selection menu to user: " + member.getEffectiveName());
    }

    private List<String> getScheduleOptions() {
        try (FileReader reader = new FileReader(SCHEDULE_FILE)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            List<String> options = gson.fromJson(reader, listType);
            LOGGER.info("Loaded schedule options from JSON file");
            return options;
        } catch (IOException e) {
            LOGGER.error("Error reading schedule data from JSON file", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equalsIgnoreCase("!verifymsg") &&
                event.getMember() != null && event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Verification")
                    .setDescription("When you first joined you should have received a private message from our Lotus Bot that will ask you to verify before anything else. If you do not see the private message, please use the bottom buttons to start the verification process.\n\n" +
                            "## Player Verification\n" +
                            "To be verified as a **Player** you must choose the \"Player\" option below to get started.\n" +
                            "From here you will choose the following within the modal popup:\n" +
                            "- Academy Tier 1: If you are here for Academy Tier 1, select the 'Academy Tier 1' option.\n" +
                            "- Academy Tier 2: If you are here for Academy Tier 2, select the 'Academy Tier 2' option.\n" +
                            "- Showcases: If you are here for Showcases, select the 'Showcases' option.\n\n" +
                            "## College Scouts\n" +
                            "To be verified as a **Scout** you must choose the \"Scout\" option below to get started.\n" +
                            "From here you will fill out the following within the modal popup:\n" +
                            "- Enter your full name.\n" +
                            "- College Esports Program Name: Enter the name of the college esports program you represent.\n" +
                            "- Official Role in the Program: Enter your official role in the program (e.g., Head Coach, Recruitment Officer).\n" +
                            "- College Email: Enter your college email address.\n\n" +
                            "## Parents\n" +
                            "To be verified as a **Parent** you must choose the \"Parent\" option below to get started.\n" +
                            "From here you will fill out the following within the modal popup:\n" +
                            "- Enter your full name.\n" +
                            "- Player's Parent: Enter the name of your child (the player).\n\n" +
                            "If you have any questions please create a support ticket in the \"support-ticket\" channel.")
                    .setColor(Color.BLUE);

            event.getChannel().sendMessageEmbeds(embed.build()).setActionRow(
                    net.dv8tion.jda.api.interactions.components.buttons.Button.primary("verify-player", "Player"),
                    net.dv8tion.jda.api.interactions.components.buttons.Button.primary("verify-scout", "Scout"),
                    net.dv8tion.jda.api.interactions.components.buttons.Button.primary("verify-parent", "Parent")
            ).queue();
        }
    }
}
