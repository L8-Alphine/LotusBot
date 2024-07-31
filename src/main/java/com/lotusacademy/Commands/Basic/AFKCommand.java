package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.AfkManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class AFKCommand implements ICommand {

    private final AfkManager afkManager;

    public AFKCommand(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @Override
    public String getName() {
        return "afk";
    }

    @Override
    public String getDescription() {
        return "Sets your afk message while you are away.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "message", "The message you want to set as your afk message.").setRequired(true)
        );
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        String message = event.getOption("message").getAsString();

        afkManager.setAfk(user, message);

        event.reply("Your status has been set to 'AWAY' with a message: '" + message + "'.")
                .setEphemeral(true)
                .queue();
    }
}
