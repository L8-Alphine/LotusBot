package com.lotusacademy.Handlers;

import com.lotusacademy.Managers.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class ChannelHandler {
    private static ChannelHandler instance;
    private static JDA jda;

    public ChannelHandler(JDA jda) {
        ChannelHandler.jda = jda;
    }

    public static ChannelHandler getInstance(JDA jda) {
        if (instance == null) {
            instance = new ChannelHandler(jda);
        }
        return instance;
    }

    public static void sendLogEmbed(EmbedBuilder embed) {
        TextChannel channel = jda.getTextChannelById(ConfigManager.getLogChannelId());
        if (channel == null) {
            System.out.println("LogChannel not found!");
            return;
        }

        sendSelfEmbed(embed, channel);
    }

    public static void sendLogEmbedButton(EmbedBuilder embed, Button button) {
        TextChannel channel = jda.getTextChannelById(ConfigManager.getLogChannelId());
        if (channel != null) {
            channel.sendMessageEmbeds(embed.build()).setActionRow(button).queue();
        }
    }

    private static void sendSelfEmbed(EmbedBuilder embed, TextChannel channel) {
        if (channel == null) {
            System.out.println("Channel not found!");
            return;
        }

        String imageURL = channel.getGuild().getIconUrl();

        if (embed.build().getFooter() == null) {
            embed.setFooter("Lotus 8 Academy", imageURL);
        }

        if (embed.build().getThumbnail() == null) {
            embed.setThumbnail(imageURL);
        }

        if (embed.build().getColor() == null) {
            embed.setColor(0x04D7D1);
        }

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
