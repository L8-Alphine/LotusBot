package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class RemoveUserCommand implements ICommand {
    private static final String STAFF_ROLE_ID = "1243332230332420117";

    @Override
    public String getName() {
        return "removeuser";
    }

    @Override
    public String getDescription() {
        return "Removes a added user from a ticket channel that the ticket creator has created.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "The user to remove from the ticket channel", true)
        );
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        TicketManager ticketManager = new TicketManager();
        TextChannel channel = (TextChannel) event.getChannel();

        if (!ticketManager.isTicketChannel(channel)) {
            event.reply("This is not a ticket channel!").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        if (userOption == null) {
            event.reply("You must specify a user to remove!").setEphemeral(true).queue();
            return;
        }

        Member memberToRemove = userOption.getAsMember();
        if (memberToRemove == null) {
            event.reply("The specified user is not valid or not in this server.").setEphemeral(true).queue();
            return;
        }

        boolean isStaff = memberToRemove.getRoles().stream().anyMatch(role -> role.getId().equals(STAFF_ROLE_ID));
        if (isStaff) {
            event.reply("You cannot remove a staff member!").setEphemeral(true).queue();
            return;
        }

        channel.getGuild().removeRoleFromMember(memberToRemove, channel.getPermissionOverride(event.getGuild().getPublicRole()).getRole()).queue(
                success -> event.reply(memberToRemove.getEffectiveName() + " has been removed from the ticket channel.").queue(),
                failure -> event.reply("Failed to remove the user from the ticket channel.").queue()
        );
    }
}
