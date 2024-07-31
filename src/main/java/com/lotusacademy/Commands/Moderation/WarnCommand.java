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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarnCommand implements ICommand {

    @Override
    public String getName() {
        return "warn";
    }

    @Override
    public String getDescription() {
        return "Warns the target user and assigns a punishment id to the warn.";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.USER, "member", "The member to warn", true));
        options.add(new OptionData(OptionType.STRING, "reason", "The reason for the warning", true));
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
        String reason = event.getOption("reason").getAsString();

        if (!member.hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("You don't have permission to warn members.").setEphemeral(true).queue();
            return;
        }

        String punishmentId = UUID.randomUUID().toString();

        // Log the warning
        EmbedBuilder logEmbed = new EmbedBuilder()
                .setTitle("Member Warned")
                .setColor(Color.YELLOW)
                .addField("Warned Member", target.getEffectiveName(), false)
                .addField("Warned By", member.getEffectiveName(), false)
                .addField("Punishment ID", punishmentId, false)
                .addField("Reason", reason, false)
                .setThumbnail(target.getAvatarUrl());

        ChannelHandler.sendLogEmbed(logEmbed);

        // Send a private message to the warned user
        EmbedBuilder dmEmbed = new EmbedBuilder()
                .setTitle("You have been warned")
                .setColor(Color.YELLOW)
                .addField("Warned By", member.getEffectiveName(), false)
                .addField("Punishment ID", punishmentId, false)
                .addField("Reason", reason, false);

        target.getUser().openPrivateChannel().queue(channel -> {
            channel.sendMessageEmbeds(dmEmbed.build()).queue();
        });

        // Store the warning in the database
        DatabaseHandler.addPunishment(target.getEffectiveName(), target.getId(), punishmentId, reason);

        // Send a confirmation to the command invoker
        event.reply("You warned " + target.getEffectiveName() + " for " + reason)
                .setEphemeral(true)
                .queue();
    }
}
