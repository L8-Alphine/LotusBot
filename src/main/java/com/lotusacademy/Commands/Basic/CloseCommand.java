package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class CloseCommand implements ICommand {
    @Override
    public String getName() {
        return "close";
    }

    @Override
    public String getDescription() {
        return "Closes the ticket you are currently in.";
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

        // Correctly retrieve the TextChannel
        TextChannel channel = (TextChannel) event.getChannel();
        Member member = event.getMember();

        TicketManager ticketManager = new TicketManager();

        if(!ticketManager.isTicketChannel(channel)){
            event.reply("This is not a ticket channel!").setEphemeral(true).queue();
            return;
        }

        event.reply("Sending confirmation message...").setEphemeral(true).queue();
        ticketManager.confirmCloseTicket(channel);
    }

}