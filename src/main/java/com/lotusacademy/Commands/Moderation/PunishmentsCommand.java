package com.lotusacademy.Commands.Moderation;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Handlers.DatabaseHandler;
import net.dv8tion.jda.api.EmbedBuilder;
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

public class PunishmentsCommand implements ICommand {

    @Override
    public String getName() {
        return "punishments";
    }

    @Override
    public String getDescription() {
        return "Gets a list of all punishments for the target user and their Punishment ID.";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "username", "The username of the member to lookup", false));
        options.add(new OptionData(OptionType.STRING, "userid", "The user ID of the member to lookup", false));
        return options;
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue(); // Send a "thinking" state to the user

        OptionMapping usernameOption = event.getOption("username");
        OptionMapping userIdOption = event.getOption("userid");
        String username = usernameOption != null ? usernameOption.getAsString() : null;
        String userId = userIdOption != null ? userIdOption.getAsString() : null;

        if (username == null && userId == null) {
            event.getHook().sendMessage("You must provide either a username or a user ID to lookup punishments.").setEphemeral(true).queue();
            return;
        }

        List<String> punishments = new ArrayList<>();
        try {
            ResultSet rs = null;
            if (userId != null) {
                rs = DatabaseHandler.getPunishmentsByUserId(userId);
            } else if (username != null) {
                rs = DatabaseHandler.getPunishmentsByUsername(username);
            }

            if (rs != null) {
                while (rs.next()) {
                    String punishmentId = rs.getString("punishment_id");
                    String reason = rs.getString("reason");
                    punishments.add("Punishment ID: " + punishmentId + "\nReason: " + reason);
                }
            }
        } catch (SQLException e) {
            event.getHook().sendMessage("An error occurred while retrieving punishments.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Punishments for " + (username != null ? username : userId))
                .setColor(Color.RED)
                .setThumbnail(event.getGuild().getIconUrl());

        if (punishments.isEmpty()) {
            embed.setDescription("No punishments found for this user.");
        } else {
            StringBuilder description = new StringBuilder();
            for (String punishment : punishments) {
                description.append(punishment).append("\n\n");
            }
            embed.setDescription(description.toString());
        }

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
}
