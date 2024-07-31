package com.lotusacademy.TicketSystem;

import com.lotusacademy.Handlers.UserHandler;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TicketAdmin extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketAdmin.class);
    private final UserHandler playerhandle = new UserHandler();
    private final TicketManager ticketManager = new TicketManager();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equals("!ticket")) {
            if (Objects.equals(event.getMember(), event.getMember().hasPermission(Permission.MANAGE_PERMISSIONS)) || event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                EmbedBuilder ticketEmbed = new EmbedBuilder();

                String embedTitle = "Create a ticket";

                String embedDescription = """
                        Thank you for coming to us for support.
                        We will try to assist you to the best of our abilities.

                        Interact with the tickets below this embed to choose a category 
                        that best suits your problem/inquiry.

                        **Category list is:**
                        | Support
                        | Reports
                        | Inquiries
                        | Appeals
                        | Billing
                        | Suggestions
                        """;

                Member self = event.getGuild().getSelfMember();

                ticketEmbed.setTitle(embedTitle)
                        .setDescription(embedDescription)
                        .setFooter("Lotus 8 Academy | TicketSystem", playerhandle.getAvatar(self))
                        .setThumbnail(playerhandle.getAvatar(self))
                        .setColor(Color.decode("#EB0056"));

                event.getChannel().sendMessageEmbeds(ticketEmbed.build()).addActionRow(
                        StringSelectMenu.create("ticket-type")
                                .addOption(
                                        "Option Reset",
                                        "reset",
                                        "Reset your ticket options",
                                        Emoji.fromUnicode("🔄")
                                )
                                .addOption(
                                        "Support",
                                        "support",
                                        "Get support for any issues you might have.",
                                        Emoji.fromUnicode("⚙️")
                                )
                                .addOption(
                                        "Reports",
                                        "reports",
                                        "Report any members that are breaking the rules.",
                                        Emoji.fromUnicode("🛡️")
                                )
                                .addOption(
                                        "Inquiries",
                                        "inquiries",
                                        "Ask questions about the server.",
                                        Emoji.fromUnicode("📝")
                                )
                                .addOption(
                                        "Appeals",
                                        "appeals",
                                        "Appeal any bans or mutes you may have.",
                                        Emoji.fromUnicode("🚩")
                                )
                                .addOption(
                                        "Billing",
                                        "billing",
                                        "Get support for any issues you may have with payments.",
                                        Emoji.fromUnicode("💰")
                                )
                                .addOption(
                                        "Suggestions",
                                        "suggestions",
                                        "Suggest any ideas you may have for the server.",
                                        Emoji.fromUnicode("💡")
                                )
                                .build()
                ).queue();

                // Schedule deletion of the user's message after 3 seconds
                event.getMessage().delete().queueAfter(3, TimeUnit.SECONDS);
            } else {
                event.getChannel().sendMessage("Invalid permissions").queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("ticket-type")) {
            return;
        }

        String ticketType = event.getValues().get(0);
        Modal modal;

        switch (ticketType) {
            case "reset":
                event.reply("Your selection was reset so you can choose another option.").setEphemeral(true).queue();
                return;
            case "support":
                modal = Modal.create("support", "Support Ticket")
                        .addActionRow(TextInput.create("supUser", "User", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("supType", "Platform", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("supDesc", "Support Description", TextInputStyle.PARAGRAPH).build())
                        .build();
                break;
            case "reports":
                modal = Modal.create("reports", "Report Ticket")
                        .addActionRow(TextInput.create("repUser", "User to Report", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("repReporter", "Reporter", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("repType", "Report Type", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("repDesc", "Report Description", TextInputStyle.PARAGRAPH).build())
                        .addActionRow(TextInput.create("repProof", "Proof", TextInputStyle.PARAGRAPH).build())
                        .build();
                break;
            case "inquiries":
                modal = Modal.create("inquiry", "Inquiry Ticket")
                        .addActionRow(TextInput.create("inqDesc", "Inquiry Description", TextInputStyle.PARAGRAPH).build())
                        .build();
                break;
            case "appeals":
                modal = Modal.create("appeal", "Appeal Ticket")
                        .addActionRow(TextInput.create("appUser", "User", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("appType", "Appeal Type", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("appID", "Appeal ID", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("appDesc", "Appeal Description", TextInputStyle.PARAGRAPH).build())
                        .addActionRow(TextInput.create("appProof", "Proof", TextInputStyle.PARAGRAPH).build())
                        .build();
                break;
            case "billing":
                modal = Modal.create("billing", "Billing Ticket")
                        .addActionRow(TextInput.create("billUser", "User", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("billType", "Billing Type", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("billPayment", "Payment ID", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("billDesc", "Billing Description", TextInputStyle.PARAGRAPH).build())
                        .build();
                break;
            case "suggestions":
                modal = Modal.create("suggestion", "Suggestion Ticket")
                        .addActionRow(TextInput.create("sugUser", "User", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("sugPlatform", "Platform", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("sugDesc", "Suggestion Description", TextInputStyle.PARAGRAPH).build())
                        .addActionRow(TextInput.create("sugType", "Suggestion Type", TextInputStyle.SHORT).build())
                        .addActionRow(TextInput.create("sugBen", "Benefit", TextInputStyle.PARAGRAPH).build())
                        .build();
                break;
            default:
                event.reply("Invalid ticket type").setEphemeral(true).queue();
                return;
        }
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(event.getUser().getName(), null, event.getUser().getAvatarUrl())
                .setThumbnail(event.getUser().getAvatarUrl())
                .setFooter("Ticket created by " + event.getUser().getName())
                .setTimestamp(event.getTimeCreated());

        String categoryPrefix;
        switch (modalId) {
            case "support":
                embed.setTitle("Support Ticket")
                        .addField("User", event.getValue("supUser").getAsString(), true)
                        .addField("Platform", event.getValue("supType").getAsString(), true)
                        .addField("Description", event.getValue("supDesc").getAsString(), false);
                categoryPrefix = "supp";
                break;
            case "reports":
                embed.setTitle("Report Ticket")
                        .addField("User to Report", event.getValue("repUser").getAsString(), true)
                        .addField("Reporter", event.getValue("repReporter").getAsString(), true)
                        .addField("Report Type", event.getValue("repType").getAsString(), true)
                        .addField("Description", event.getValue("repDesc").getAsString(), false)
                        .addField("Proof", event.getValue("repProof").getAsString(), false);
                categoryPrefix = "repo";
                break;
            case "inquiry":
                embed.setTitle("Inquiry Ticket")
                        .addField("Description", event.getValue("inqDesc").getAsString(), false);
                categoryPrefix = "inqu";
                break;
            case "appeal":
                embed.setTitle("Appeal Ticket")
                        .addField("User", event.getValue("appUser").getAsString(), true)
                        .addField("Appeal Type", event.getValue("appType").getAsString(), true)
                        .addField("Appeal ID", event.getValue("appID").getAsString(), true)
                        .addField("Description", event.getValue("appDesc").getAsString(), false)
                        .addField("Proof", event.getValue("appProof").getAsString(), false);
                categoryPrefix = "appe";
                break;
            case "billing":
                embed.setTitle("Billing Ticket")
                        .addField("User", event.getValue("billUser").getAsString(), true)
                        .addField("Billing Type", event.getValue("billType").getAsString(), true)
                        .addField("Payment ID", event.getValue("billPayment").getAsString(), true)
                        .addField("Description", event.getValue("billDesc").getAsString(), false);
                categoryPrefix = "bill";
                break;
            case "suggestion":
                embed.setTitle("Suggestion Ticket")
                        .addField("User", event.getValue("sugUser").getAsString(), true)
                        .addField("Platform", event.getValue("sugPlatform").getAsString(), true)
                        .addField("Description", event.getValue("sugDesc").getAsString(), false)
                        .addField("Suggestion Type", event.getValue("sugType").getAsString(), true)
                        .addField("Benefit", event.getValue("sugBen").getAsString(), false);
                categoryPrefix = "sugg";
                break;
            default:
                event.reply("Unknown modal interaction").setEphemeral(true).queue();
                return;
        }

        // Delegate ticket creation to TicketManager
        ticketManager.createTicket(event.getUser(), event.getGuild(), embed, categoryPrefix, event);
    }
}
