package com.lotusacademy.Commands.Moderation;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Handlers.ChannelHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ClearCommand implements ICommand {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clears messages with the defined amount and optionally from a specific user or channel.";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.INTEGER, "amount", "The number of messages to delete", true));
        options.add(new OptionData(OptionType.USER, "user", "The user whose messages to delete", false));
        options.add(new OptionData(OptionType.CHANNEL, "channel", "The channel to delete messages from", false));
        return options;
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (!member.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("You don't have permission to manage messages.").setEphemeral(true).queue();
            return;
        }

        OptionMapping amountOption = event.getOption("amount");
        OptionMapping userOption = event.getOption("user");
        OptionMapping channelOption = event.getOption("channel");

        int amount = amountOption.getAsInt();
        Member targetUser = userOption != null ? userOption.getAsMember() : null;
        MessageChannel targetChannel = channelOption != null ? (MessageChannel) channelOption.getAsChannel() : event.getChannel();

        // Defer reply to avoid interaction expiration
        event.deferReply().queue();

        CompletableFuture.runAsync(() -> {
            List<Message> messages = new ArrayList<>();

            if (targetUser != null) {
                messages.addAll(targetChannel.getIterableHistory().stream()
                        .filter(message -> message.getAuthor().equals(targetUser.getUser()))
                        .limit(amount)
                        .collect(Collectors.toList()));
            } else {
                messages.addAll(targetChannel.getIterableHistory().stream()
                        .limit(amount)
                        .collect(Collectors.toList()));
            }

            if (!messages.isEmpty()) {
                targetChannel.purgeMessages(messages);

                // Log the action
                EmbedBuilder logEmbed = new EmbedBuilder()
                        .setTitle("Messages Cleared")
                        .setColor(Color.YELLOW)
                        .addField("Cleared By", member.getEffectiveName(), false)
                        .addField("Amount", String.valueOf(amount), false)
                        .addField("Channel", targetChannel.getName(), false);

                if (targetUser != null) {
                    logEmbed.addField("User", targetUser.getEffectiveName(), false);
                }

                ChannelHandler.sendLogEmbed(logEmbed);

                // Send a confirmation to the command invoker
                event.getHook().sendMessage("Successfully cleared " + messages.size() + " messages.")
                        .setEphemeral(true)
                        .queue();
            } else {
                event.getHook().sendMessage("No messages found to clear.")
                        .setEphemeral(true)
                        .queue();
            }
        });
    }
}
