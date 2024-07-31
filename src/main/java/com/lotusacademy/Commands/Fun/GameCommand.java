package com.lotusacademy.Commands.Fun;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.GameManager;
import com.lotusacademy.Managers.GameManager.GameStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GameCommand implements ICommand {

    private final GameManager gameManager = new GameManager();

    @Override
    public String getName() {
        return "game";
    }

    @Override
    public String getDescription() {
        return "Allows you to lookup a player's stats for a selected game.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "game", "The game to lookup stats for", true),
                new OptionData(OptionType.STRING, "user", "The user ID to lookup stats for", true),
                new OptionData(OptionType.STRING, "platform", "The platform the user is on (pc, psn, xbox)", true)
        );
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String game = event.getOption("game").getAsString();
        String userId = event.getOption("user").getAsString();
        String platform = event.getOption("platform").getAsString();

        try {
            GameStats stats = gameManager.fetchGameStats(game, userId, platform);

            if (stats == null) {
                event.reply("Game not supported.").queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Game Stats")
                    .setColor(Color.BLUE)
                    .setThumbnail(stats.iconUrl)
                    .addField("Game", stats.game, true)
                    .addField("User", stats.username, true)
                    .addField("Platform", stats.platform, true)
                    .addField("Stats", stats.stats, false);

            event.replyEmbeds(embed.build()).queue();
        } catch (IOException e) {
            event.reply("Failed to fetch stats. Please try again later.").queue();
        }
    }
}
