package com.lotusacademy.Commands.Fun;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.MemeManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class MemeCommand implements ICommand {
    private final MemeManager memeManager = new MemeManager();

    @Override
    public String getName() {
        return "meme";
    }

    @Override
    public String getDescription() {
        return "Generates a random gaming meme for you to enjoy!";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            String memeUrl = memeManager.fetchRandomMeme();
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Here's a random gaming meme for you!")
                    .setColor(Color.GREEN)
                    .setImage(memeUrl);
            event.replyEmbeds(embed.build()).queue();
        } catch (IOException e) {
            event.reply("Failed to fetch meme. Please try again later.").queue();
        }
    }
}
