package com.lotusacademy.Commands.Fun;

import com.lotusacademy.Commands.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.util.List;
import java.util.Random;

public class SlapCommand implements ICommand {
    private final Random random = new Random();
    private final String[] slappingGifs = {
            "https://media.giphy.com/media/Gf3AUz3eBNbTW/giphy.gif",
            "https://media.giphy.com/media/jLeyZWgtwgr2U/giphy.gif",
            "https://media.giphy.com/media/RXGNsyRb1hDJm/giphy.gif",
            "https://media.giphy.com/media/l3YSimA8CV1k41b1u/giphy.gif",
            "https://media.giphy.com/media/mEtSQlxqBtWWA/giphy.gif"
    };

    @Override
    public String getName() {
        return "slap";
    }

    @Override
    public String getDescription() {
        return "Slaps a selected user with a mention and a random slapping gif.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "target", "The user to slap", true)
        );
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var user = event.getUser();
        var target = event.getOption("target").getAsUser();
        var gifUrl = slappingGifs[random.nextInt(slappingGifs.length)];

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(user.getName() + " decided to slap " + target.getName() + "!")
                .setColor(Color.RED)
                .setImage(gifUrl);

        event.replyEmbeds(embed.build()).queue();
    }
}
