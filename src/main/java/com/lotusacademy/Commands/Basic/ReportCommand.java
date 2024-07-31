package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class ReportCommand implements ICommand {
    @Override
    public String getName() {
        return "report";
    }

    @Override
    public String getDescription() {
        return "Creates a report ticket for the staff team to know.";
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

    }
}
