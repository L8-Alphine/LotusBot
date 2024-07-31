package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.EnumSet;
import java.util.List;

public class AddUserCommand implements ICommand {
    @Override
    public String getName() {
        return "adduser";
    }

    @Override
    public String getDescription() {
        return "Adds the mentioned user to the ticket channel.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "The user to add to the ticket channel", true)
        );
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {

        TextChannel channel = (TextChannel) event.getChannel();
        Member memberToAdd = event.getOption("user").getAsMember();

        TicketManager ticketManager = new TicketManager();

        if(!ticketManager.isTicketChannel(channel)){
            event.reply("This is not a ticket channel!").setEphemeral(true).queue();
            return;
        }

        // Check if the member to add is not null
        if (memberToAdd == null) {
            event.reply("The specified user could not be found.").setEphemeral(true).queue();
            return;
        }

        // Add the member to the channel with the same permissions
        channel.getManager().putPermissionOverride(memberToAdd, EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_EMBED_LINKS
        ), null).queue(
                success -> event.reply("Users " + memberToAdd.getAsMention() + " has been added to the ticket channel.").queue(),
                failure -> event.reply("An error occurred while adding the user to the channel.").setEphemeral(true).queue()
        );
    }
}
