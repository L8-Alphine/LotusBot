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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class KickCommand implements ICommand {

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kicks the target user from the server with a reason and a punishment ID.";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.USER, "member", "The member that's gonna get kicked", true));
        options.add(new OptionData(OptionType.STRING, "reason", "The reason you're kicking the member", true));
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

        String reason = event.getOption("reason").getAsString();

        if (!member.hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("You don't have permission to kick members.").setEphemeral(true).queue();
            return;
        }

        if (!self.hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("I don't have permission to kick members.").setEphemeral(true).queue();
            return;
        }

        String punishmentId = generatePunishmentId();

        // Defer reply to avoid interaction expiration
        event.deferReply().queue();

        // Kick the member
        event.getGuild().kick(target, reason).queue();

        // Store punishment in the database asynchronously
        CompletableFuture.runAsync(() -> {
            DatabaseHandler.addPunishment(target.getEffectiveName(), target.getId(), punishmentId, reason);

            // Send a private message to the kicked user with a reason and punishment ID
            EmbedBuilder kickEmbed = new EmbedBuilder()
                    .setTitle("You have been kicked")
                    .setColor(Color.ORANGE)
                    .setDescription("You have been kicked from " + event.getGuild().getName() + ".")
                    .addField("Reason", reason, false)
                    .addField("Punishment ID", punishmentId, false)
                    .addField("Kicked By", member.getEffectiveName(), false);

            target.getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessageEmbeds(kickEmbed.build()).queue();
            });

            // Log the kick with a punishment ID
            String kickString = "**Kicked By:** " + member.getEffectiveName() +
                    "\n" +
                    "\n" +
                    "**Kicked Member:** " + target.getEffectiveName() +
                    "\n" +
                    "\n" +
                    "**Reason:** " +
                    "```" + reason + "```" +
                    "\n" +
                    "\n" +
                    "**Punishment ID:** " + punishmentId;

            EmbedBuilder kickEmbedLog = new EmbedBuilder()
                    .setTitle("Member Kicked")
                    .setThumbnail(target.getAvatarUrl())
                    .setColor(Color.ORANGE)
                    .setDescription(kickString);

            ChannelHandler.sendLogEmbed(kickEmbedLog);

            // Send a confirmation to the command invoker
            event.getHook().sendMessage("You kicked " + target.getEffectiveName() + " from " + event.getGuild().getName())
                    .setEphemeral(true)
                    .queue();
        });
    }

    private String generatePunishmentId() {
        return UUID.randomUUID().toString();
    }
}
