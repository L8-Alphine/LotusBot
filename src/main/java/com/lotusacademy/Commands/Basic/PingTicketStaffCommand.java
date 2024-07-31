package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingTicketStaffCommand implements ICommand {
    private static final String ROLE_ID = "1247102548570013781";
    private static final long COOLDOWN = 60 * 1000; // 60 seconds in milliseconds
    private static final Map<String, Long> cooldowns = new HashMap<>();

    @Override
    public String getName() {
        return "pingstaff";
    }

    @Override
    public String getDescription() {
        return "Pings a staff member within a ticket channel to assist you.";
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
        String userId = event.getUser().getId();
        long currentTime = System.currentTimeMillis();

        TextChannel channel = (TextChannel) event.getChannel();
        TicketManager ticketManager = new TicketManager();
        if(!ticketManager.isTicketChannel(channel)){
            event.reply("This is not a ticket channel!").setEphemeral(true).queue();
            return;
        }

        if (cooldowns.containsKey(userId) && (currentTime - cooldowns.get(userId) < COOLDOWN)) {
            long timeLeft = (COOLDOWN - (currentTime - cooldowns.get(userId))) / 1000;
            event.reply("You must wait " + timeLeft + " seconds before using this command again.").setEphemeral(true).queue();
            return;
        }

        cooldowns.put(userId, currentTime);

        event.getChannel().sendMessage("Attention <@&" + ROLE_ID + ">! " + event.getUser().getAsMention() + " needs assistance.").queue();
        event.reply("A staff member has been pinged to assist you.").setEphemeral(true).queue();
    }
}
