package com.lotusacademy.Managers;

import com.lotusacademy.Commands.ICommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CommandManager extends ListenerAdapter {
    private static  CommandManager instance;
    private List<ICommand> commands = new ArrayList<>();

    public CommandManager(){

    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (Guild guild : event.getJDA().getGuilds()) {
            for (ICommand command : commands) {
                var commandData = guild.upsertCommand(command.getName(), command.getDescription());

                List<OptionData> options = command.getOptions();
                if (options != null && !options.isEmpty()) {
                    commandData = commandData.addOptions(options);
                }

                List<SubcommandData> subcommands = command.getSubcommands();
                if (subcommands != null && !subcommands.isEmpty()) {
                    commandData = commandData.addSubcommands(subcommands);
                }

                commandData.queue();
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        for(ICommand command: commands){
            if(command.getName().equals(event.getName())){
                command.execute(event);
                return;
            }
        }
    }

    public static CommandManager getInstance(){
        if (instance == null){
            instance = new CommandManager();
        }
        return instance;
    }

    public void add(ICommand command){
        if (commands.stream().noneMatch(c -> c.getName().equals(command.getName()))) {
            commands.add(command);
        } else {
            System.out.println("Command " + command.getName() + " already exists.");
        }
    }

    public List<ICommand> getCommands(){
        return  commands;
    }
}
