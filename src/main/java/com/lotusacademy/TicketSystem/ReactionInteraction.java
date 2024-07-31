package com.lotusacademy.TicketSystem;

import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ReactionInteraction extends ListenerAdapter {

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        Member self = event.getGuild().getSelfMember();
        if (event.getUser().isBot()){
            return;
        }
        TicketManager ticketManager = new TicketManager();
        TextChannel channel = event.getChannel().asTextChannel();

        String emoji = event.getEmoji().getAsReactionCode();
        String message = event.getMessageId();
        if (emoji.equals("✅")) {
            if(event.getMember().equals(self)){
                return;
            }
            if(!ticketManager.isTicketChannel(channel)){
                return;
            }
            event.getChannel().deleteMessageById(message).queue();
            ticketManager.closeTicket(channel);
        } else if (emoji.equals("❌")) {
            if(event.getMember().equals(self)){
                return;
            }
            if(!ticketManager.isTicketChannel(channel)){
                return;
            }
            event.getChannel().deleteMessageById(message).queue();
            channel.sendMessage("Ticket will remain open").queue();
        }
    }
}