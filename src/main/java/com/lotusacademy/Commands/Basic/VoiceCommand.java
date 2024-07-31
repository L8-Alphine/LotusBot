package com.lotusacademy.Commands.Basic;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Managers.VoiceChannelManager;
import com.lotusacademy.PrivateVoiceChannelSystem.VoiceChannelData;
import com.lotusacademy.PrivateVoiceChannelSystem.VoiceChannelData.VoiceChannelInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

import static com.lotusacademy.Managers.VoiceChannelManager.DEFAULT_USER_LIMIT;

public class VoiceCommand implements ICommand {
    private final VoiceChannelManager voiceChannelManager;

    public VoiceCommand(VoiceChannelManager voiceChannelManager) {
        this.voiceChannelManager = voiceChannelManager;
    }

    @Override
    public String getName() {
        return "voice";
    }

    @Override
    public String getDescription() {
        return "Allows you to add, remove, list, and change voice channels.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of(
                new SubcommandData("add", "Add a user to your private voice channel")
                        .addOptions(new OptionData(OptionType.USER, "user", "User to add", true)),
                new SubcommandData("remove", "Remove a user from your private voice channel")
                        .addOptions(new OptionData(OptionType.USER, "user", "User to remove", true)),
                new SubcommandData("list", "List private channels you are added to"),
                new SubcommandData("change", "Change the user limit or status of your private voice channel")
                        .addOptions(
                                new OptionData(OptionType.INTEGER, "limit", "New user limit", false).setMinValue(2).setMaxValue(99),
                                new OptionData(OptionType.BOOLEAN, "reset", "Reset the user limit to default", false),
                                new OptionData(OptionType.BOOLEAN, "private", "Make the channel private or public via true or false.", false)
                        )
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subcommand = event.getSubcommandName();
        Member member = event.getMember();
        if (member == null) {
            event.reply("You need to be in a server to use this command.").setEphemeral(true).queue();
            return;
        }

        VoiceChannel voiceChannel = (VoiceChannel) member.getVoiceState().getChannel();
        if (voiceChannel == null) {
            event.reply("You need to be in a voice channel to use this command.").setEphemeral(true).queue();
            return;
        }

        switch (subcommand) {
            case "add":
                event.deferReply(true).queue();
                handleAddUser(event, member, voiceChannel);
                break;
            case "remove":
                event.deferReply(true).queue();
                handleRemoveUser(event, member, voiceChannel);
                break;
            case "list":
                handleListChannels(event, member);
                break;
            case "change":
                event.deferReply(true).queue();
                handleChangeLimitOrStatus(event, member, voiceChannel);
                break;
            default:
                event.reply("Unknown subcommand").setEphemeral(true).queue();
                break;
        }
    }

    private void handleAddUser(SlashCommandInteractionEvent event, Member member, VoiceChannel voiceChannel) {
        VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(voiceChannel.getId());
        if (info == null || !info.getOwner().equals(member.getId())) {
            event.getHook().editOriginal("You do not own this voice channel.").queue();
            return;
        }

        Member userToAdd = event.getOption("user").getAsMember();
        if (userToAdd == null) {
            event.getHook().editOriginal("User not found.").queue();
            return;
        }

        voiceChannel.getManager().putMemberPermissionOverride(userToAdd.getIdLong(), EnumSet.of(
                Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VOICE_STREAM), null).queue();

        VoiceChannelData.addUserToChannel(voiceChannel, userToAdd.getId());
        event.getHook().editOriginal(userToAdd.getEffectiveName() + " has been added to the voice channel.").queue();
    }

    private void handleRemoveUser(SlashCommandInteractionEvent event, Member member, VoiceChannel voiceChannel) {
        VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(voiceChannel.getId());
        if (info == null || !info.getOwner().equals(member.getId())) {
            event.getHook().editOriginal("You do not own this voice channel.").queue();
            return;
        }

        Member userToRemove = event.getOption("user").getAsMember();
        if (userToRemove == null) {
            event.getHook().editOriginal("User not found.").queue();
            return;
        }

        if (!info.getUsers().contains(userToRemove.getId())) {
            event.getHook().editOriginal("This user is not in your voice channel.").queue();
            return;
        }

        voiceChannel.getManager().removePermissionOverride(userToRemove.getIdLong()).queue();
        VoiceChannelData.removeUserFromChannel(voiceChannel, userToRemove.getId());
        voiceChannelManager.removeMemberPermissions(voiceChannel, userToRemove);
        event.getHook().editOriginal(userToRemove.getEffectiveName() + " has been removed from the voice channel.").queue();
    }

    private void handleListChannels(SlashCommandInteractionEvent event, Member member) {
        List<VoiceChannelInfo> channels = VoiceChannelData.getChannelsForUser(member.getId());
        if (channels.isEmpty()) {
            event.reply("You are not added to any private channels.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Private Channels")
                .setColor(Color.BLUE);

        for (VoiceChannelInfo info : channels) {
            embed.addField("Channel ID", info.getChannelId(), false);
        }

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleChangeLimitOrStatus(SlashCommandInteractionEvent event, Member member, VoiceChannel voiceChannel) {
        VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(voiceChannel.getId());
        if (info == null || !info.getOwner().equals(member.getId())) {
            event.getHook().editOriginal("You do not own this voice channel.").queue();
            return;
        }

        boolean reset = event.getOption("reset") != null && event.getOption("reset").getAsBoolean();
        if (reset) {
            voiceChannel.getManager().setUserLimit(DEFAULT_USER_LIMIT).queue();
            VoiceChannelData.updateUserLimit(voiceChannel, DEFAULT_USER_LIMIT);
            event.getHook().editOriginal("User limit has been reset to " + DEFAULT_USER_LIMIT).queue();
            return;
        }

        int newLimit = event.getOption("limit") != null ? event.getOption("limit").getAsInt() : info.getUserLimit();
        Boolean newStatus = event.getOption("private") != null ? event.getOption("private").getAsBoolean() : null;

        if (newLimit != info.getUserLimit()) {
            voiceChannel.getManager().setUserLimit(newLimit).queue();
            VoiceChannelData.updateUserLimit(voiceChannel, newLimit);
            event.getHook().editOriginal("User limit has been set to " + newLimit).queue();
        }

        if (newStatus != null && newStatus != info.isPrivate()) {
            voiceChannelManager.updateChannelStatus(voiceChannel, newStatus);
            event.getHook().editOriginal("Channel status has been changed to " + (newStatus ? "private" : "public")).queue();
        }
    }
}
