package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class NexxaWaveCommand implements ICommand {
    @Override
    public String getName() {
        return "nexxawavetech";
    }

    @Override
    public String getDescription() {
        return "Gets the NexxaWaveTech website link and Owner.";
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
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(0x00ff00);
        embed.setAuthor("NexxaWaveTech", "https://www.nexxawavetech.com", "https://www.nexxawavetech.com/favicon.ico");
        embed.setTitle("NexxaWaveTech", "https://www.nexxawavetech.com");
        embed.setDescription("This is the official website of NexxaWaveTech and the creator of this bot.");
        embed.addField("Owner", "Krystian Carter", false);
        embed.addField("Bot Creator", "alphineghost", false);
        embed.setFooter("NexxaWaveTech ©", "https://www.nexxawavetech.com/favicon.ico");
        embed.setThumbnail("https://www.nexxawavetech.com/favicon.ico");

        event.replyEmbeds(embed.build()).queue();
    }
}
