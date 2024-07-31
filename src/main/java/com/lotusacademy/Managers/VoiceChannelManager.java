package com.lotusacademy.Managers;

import com.lotusacademy.Handlers.ChannelHandler;
import com.lotusacademy.PrivateVoiceChannelSystem.VoiceChannelData;
import com.lotusacademy.PrivateVoiceChannelSystem.VoiceChannelData.VoiceChannelInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Manages the creation, deletion, and permissions of private voice channels in a Discord server.
 */
public class VoiceChannelManager extends ListenerAdapter {
    private static final String PRIVATE_CHANNELS_CATEGORY_NAME = "Private Channels";
    private static final String MAIN_VOICE_CHANNEL_NAME = "Join to Create...";
    private static final int CHANNEL_CREATION_DELAY = 3; // seconds
    private static final int CHANNEL_DELETION_DELAY = 30; // seconds
    public static final int DEFAULT_USER_LIMIT = 10;
    private Logger LOGGER = Logger.getLogger(VoiceChannelManager.class.getName());
    private final Map<String, String> voiceChannelOwners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> deletionTasks = new ConcurrentHashMap<>();

    /**
     * Initializes the VoiceChannelManager, creating necessary directories and loading existing channels.
     */
    public VoiceChannelManager() {
        try {
            Files.createDirectories(Paths.get(VoiceChannelData.VOICE_CHANNELS_PATH));
            loadExistingChannels();
            LOGGER.info("Voice Channel Manager initialized.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads existing channels from storage and registers their owners.
     */
    private void loadExistingChannels() {
        try {
            Files.list(Paths.get(VoiceChannelData.VOICE_CHANNELS_PATH)).forEach(path -> {
                String channelId = path.getFileName().toString().replace(".yml", "");
                VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(channelId);
                if (info != null) {
                    voiceChannelOwners.put(channelId, info.getOwner());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles voice update events, triggering join or leave handlers as appropriate.
     *
     * @param event the guild voice update event
     */
    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null) {
            onGuildVoiceJoin(event);
        }
        if (event.getChannelLeft() != null) {
            onGuildVoiceLeave(event);
        }
    }

    /**
     * Handles a member joining a voice channel.
     *
     * @param event the guild voice update event
     */
    public void onGuildVoiceJoin(GuildVoiceUpdateEvent event) {
        Member member = event.getEntity();
        VoiceChannel joinedChannel = (VoiceChannel) event.getChannelJoined();

        if (MAIN_VOICE_CHANNEL_NAME.equals(joinedChannel.getName())) {
            createPrivateVoiceChannel(member);
        } else if (voiceChannelOwners.containsKey(joinedChannel.getId())) {
            VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(joinedChannel.getId());
            if (info != null && !info.getOwner().equals(member.getId())) {
                setMemberPermissions(joinedChannel, member);
                VoiceChannelData.addUserToChannel(joinedChannel, member.getId());
            }
            cancelDeletionTask(joinedChannel.getId());
        }
    }

    /**
     * Handles a member leaving a voice channel.
     *
     * @param event the guild voice update event
     */
    public void onGuildVoiceLeave(GuildVoiceUpdateEvent event) {
        VoiceChannel leftChannel = (VoiceChannel) event.getChannelLeft();
        if (leftChannel != null && voiceChannelOwners.containsKey(leftChannel.getId())) {
            VoiceChannelData.removeUserFromChannel(leftChannel, event.getEntity().getId());
            if (leftChannel.getMembers().isEmpty()) {
                scheduleChannelDeletion(leftChannel);
            }
        }
    }

    /**
     * Creates a private voice channel for a member.
     *
     * @param member the member for whom the channel is being created
     */
    private void createPrivateVoiceChannel(@NotNull Member member) {
        Guild guild = member.getGuild();
        Category privateChannelsCategory = guild.getCategoriesByName(PRIVATE_CHANNELS_CATEGORY_NAME, true).stream().findFirst().orElse(null);

        if (privateChannelsCategory == null) {
            guild.createCategory(PRIVATE_CHANNELS_CATEGORY_NAME).queue(category -> createVoiceChannelInCategory(category, member));
        } else {
            createVoiceChannelInCategory(privateChannelsCategory, member);
        }
    }

    /**
     * Creates a voice channel in a specified category for a member.
     *
     * @param category the category in which to create the channel
     * @param member   the member for whom the channel is being created
     */
    private void createVoiceChannelInCategory(@NotNull Category category, @NotNull Member member) {
        if (hasExistingPrivateChannel(member)) {
            return;
        }

        String channelId = UUID.randomUUID().toString(); // Generate a unique channel ID
        VoiceChannelInfo info = new VoiceChannelInfo(channelId, member.getId(), member.getId(), DEFAULT_USER_LIMIT, true, Collections.emptyList());
        String channelName = member.getUser().getName().toLowerCase() + "'s-channel-" + (info.isPrivate() ? "private" : "public");

        category.createVoiceChannel(channelName).addPermissionOverride(member.getGuild().getSelfMember(), EnumSet.allOf(Permission.class), null)
                .addPermissionOverride(member, EnumSet.of(
                        Permission.VOICE_CONNECT,
                        Permission.VOICE_SPEAK,
                        Permission.VOICE_STREAM,
                        Permission.VOICE_MOVE_OTHERS,
                        Permission.VOICE_DEAF_OTHERS,
                        Permission.VOICE_MUTE_OTHERS,
                        Permission.MESSAGE_MANAGE,
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.MESSAGE_ATTACH_FILES,
                        Permission.MESSAGE_HISTORY,
                        Permission.MESSAGE_SEND,
                        Permission.VIEW_CHANNEL,
                        Permission.USE_APPLICATION_COMMANDS), null)
                .queue(voiceChannel -> {
                    voiceChannel.getManager().setUserLimit(DEFAULT_USER_LIMIT).queue();
                    voiceChannelOwners.put(voiceChannel.getId(), member.getId());
                    VoiceChannelData.saveVoiceChannelData(voiceChannel, member.getId(), member.getId(), DEFAULT_USER_LIMIT, info.isPrivate(), Collections.emptyList());

                    setAllPermissions(voiceChannel, member);

                    logVoiceChannelCreation(voiceChannel, member);

                    // Move member to the new voice channel after a delay
                    scheduler.schedule(() -> moveMemberToChannel(member, voiceChannel), CHANNEL_CREATION_DELAY, TimeUnit.SECONDS);
                });
    }

    /**
     * Checks if a member already has an existing private channel.
     *
     * @param member the member to check
     * @return true if the member has an existing private channel, false otherwise
     */
    private boolean hasExistingPrivateChannel(Member member) {
        return voiceChannelOwners.containsValue(member.getId());
    }

    /**
     * Sets all permissions for the bot, owner, and members for a specified voice channel.
     *
     * @param voiceChannel the voice channel to set permissions for
     * @param owner        the owner of the channel
     */
    private void setAllPermissions(@NotNull VoiceChannel voiceChannel, @NotNull Member owner) {
        addBotPermissions(voiceChannel);
        setOwnerPermissions(voiceChannel, owner);

        VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(voiceChannel.getId());
        if (info != null && info.isPrivate()) {
            setPrivateChannelPermissions(voiceChannel, owner);
        } else {
            setPublicChannelPermissions(voiceChannel, owner);
        }
    }

    /**
     * Adds all permissions to the bot for a specified voice channel.
     *
     * @param voiceChannel the voice channel for which to add bot permissions
     */
    private void addBotPermissions(@NotNull VoiceChannel voiceChannel) {
        Guild guild = voiceChannel.getGuild();
        Member selfMember = guild.getSelfMember();
        LOGGER.info("Setting bot permissions for channel: " + voiceChannel.getName());

        voiceChannel.getManager().putMemberPermissionOverride(selfMember.getIdLong(), EnumSet.allOf(Permission.class), null).queue(
                success -> LOGGER.info("Bot permissions set successfully."),
                error -> LOGGER.severe("Failed to set bot permissions: " + error.getMessage())
        );
    }

    /**
     * Sets owner permissions for a specified voice channel.
     *
     * @param voiceChannel the voice channel to set owner permissions for
     * @param owner        the owner of the channel
     */
    private void setOwnerPermissions(@NotNull VoiceChannel voiceChannel, @NotNull Member owner) {
        LOGGER.info("Setting owner permissions for channel: " + voiceChannel.getName());

        voiceChannel.getManager().putMemberPermissionOverride(owner.getIdLong(), EnumSet.of(
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_STREAM,
                Permission.VOICE_MOVE_OTHERS,
                Permission.VOICE_DEAF_OTHERS,
                Permission.VOICE_MUTE_OTHERS,
                Permission.MESSAGE_MANAGE,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_SEND,
                Permission.VIEW_CHANNEL,
                Permission.USE_APPLICATION_COMMANDS), null).queue(
                success -> LOGGER.info("Owner permissions set successfully."),
                error -> LOGGER.severe("Failed to set owner permissions: " + error.getMessage())
        );
    }

    /**
     * Sets private channel permissions.
     *
     * @param voiceChannel the voice channel to set permissions for
     * @param owner        the owner of the channel
     */
    private void setPrivateChannelPermissions(@NotNull VoiceChannel voiceChannel, @NotNull Member owner) {
        LOGGER.info("Setting private channel permissions for channel: " + voiceChannel.getName());

        // Everyone can view the channel but can't connect
        voiceChannel.getManager().putRolePermissionOverride(voiceChannel.getGuild().getPublicRole().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL), EnumSet.of(Permission.VOICE_CONNECT)).queue(
                success -> LOGGER.info("Private channel permissions set successfully."),
                error -> LOGGER.severe("Failed to set private channel permissions: " + error.getMessage())
        );

        // Ensure the owner has all necessary permissions
        voiceChannel.getManager().putMemberPermissionOverride(owner.getIdLong(), EnumSet.allOf(Permission.class), null).queue(
                success -> LOGGER.info("Owner permissions set successfully."),
                error -> LOGGER.severe("Failed to set owner permissions: " + error.getMessage())
        );
    }

    /**
     * Sets public channel permissions.
     *
     * @param voiceChannel the voice channel to set permissions for
     * @param owner        the owner of the channel
     */
    private void setPublicChannelPermissions(@NotNull VoiceChannel voiceChannel, @NotNull Member owner) {
        LOGGER.info("Setting public channel permissions for channel: " + voiceChannel.getName());

        // Everyone can view, connect, speak, send messages, and use application commands
        voiceChannel.getManager().putRolePermissionOverride(voiceChannel.getGuild().getPublicRole().getIdLong(), EnumSet.of(
                Permission.VIEW_CHANNEL,
                Permission.VOICE_CONNECT,
                Permission.VOICE_SPEAK,
                Permission.VOICE_STREAM,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_ADD_REACTION,
                Permission.USE_APPLICATION_COMMANDS), null).queue(
                success -> LOGGER.info("Public channel permissions set successfully."),
                error -> LOGGER.severe("Failed to set public channel permissions: " + error.getMessage())
        );

        // Ensure the owner has all necessary permissions
        voiceChannel.getManager().putMemberPermissionOverride(owner.getIdLong(), EnumSet.allOf(Permission.class), null).queue(
                success -> LOGGER.info("Owner permissions set successfully."),
                error -> LOGGER.severe("Failed to set owner permissions: " + error.getMessage())
        );
    }

    /**
     * Sets member permissions for a specified voice channel.
     *
     * @param voiceChannel the voice channel to set member permissions for
     * @param member       the member whose permissions to set
     */
    private void setMemberPermissions(@NotNull VoiceChannel voiceChannel, @NotNull Member member) {
        LOGGER.info("Setting permissions for member: " + member.getEffectiveName());

        voiceChannel.getManager().putMemberPermissionOverride(member.getIdLong(), EnumSet.of(
                Permission.VOICE_CONNECT,
                Permission.VIEW_CHANNEL,
                Permission.VOICE_SPEAK,
                Permission.VOICE_STREAM,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_ADD_REACTION,
                Permission.USE_APPLICATION_COMMANDS), null).queue(
                success -> LOGGER.info("Member permissions set successfully."),
                error -> LOGGER.severe("Failed to set member permissions: " + error.getMessage())
        );
    }

    /**
     * Removes permissions for a specified member from a voice channel.
     *
     * @param voiceChannel the voice channel to remove permissions from
     * @param member       the member whose permissions to remove
     */
    public void removeMemberPermissions(@NotNull VoiceChannel voiceChannel, @NotNull Member member) {
        LOGGER.info("Removing permissions for member: " + member.getEffectiveName());

        voiceChannel.getManager().removePermissionOverride(member.getIdLong()).queue(
                success -> LOGGER.info("Member permissions removed successfully."),
                error -> LOGGER.severe("Failed to remove member permissions: " + error.getMessage())
        );
    }

    /**
     * Deletes a private voice channel.
     *
     * @param voiceChannel the voice channel to delete
     */
    private void deletePrivateVoiceChannel(@NotNull VoiceChannel voiceChannel) {
        String voiceChannelId = voiceChannel.getId();
        voiceChannel.delete().queue(
                success -> {
                    voiceChannelOwners.remove(voiceChannelId);
                    VoiceChannelData.deleteVoiceChannelData(voiceChannel);
                    logVoiceChannelDeletion(voiceChannel);
                    LOGGER.info("Voice channel deleted successfully.");
                },
                error -> LOGGER.severe("Failed to delete voice channel: " + error.getMessage())
        );
    }

    /**
     * Logs the creation of a voice channel.
     *
     * @param voiceChannel the voice channel that was created
     * @param member       the member who created the voice channel
     */
    private void logVoiceChannelCreation(@NotNull VoiceChannel voiceChannel, @NotNull Member member) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Voice Channel Created")
                .setColor(Color.GREEN)
                .setDescription("A new private voice channel has been created.")
                .addField("Channel", voiceChannel.getName(), false)
                .addField("Creator", member.getEffectiveName(), false)
                .setTimestamp(java.time.Instant.now());

        ChannelHandler.sendLogEmbed(embed);
    }

    /**
     * Logs the deletion of a voice channel.
     *
     * @param voiceChannel the voice channel that was deleted
     */
    private void logVoiceChannelDeletion(@NotNull VoiceChannel voiceChannel) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Voice Channel Deleted")
                .setColor(Color.RED)
                .setDescription("A private voice channel has been deleted.")
                .addField("Channel", voiceChannel.getName(), false)
                .setTimestamp(java.time.Instant.now());

        ChannelHandler.sendLogEmbed(embed);
    }

    /**
     * Moves a member to a specified voice channel.
     *
     * @param member       the member to move
     * @param voiceChannel the voice channel to move the member to
     */
    private void moveMemberToChannel(Member member, VoiceChannel voiceChannel) {
        member.getGuild().moveVoiceMember(member, voiceChannel).queue(success -> {
            // Ensure permissions are set correctly after moving the member
            VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(voiceChannel.getId());
            if (info != null && !info.getOwner().equals(member.getId())) {
                setMemberPermissions(voiceChannel, member);
            }
        });
    }

    /**
     * Schedules the deletion of a voice channel after a delay.
     *
     * @param voiceChannel the voice channel to schedule for deletion
     */
    private void scheduleChannelDeletion(VoiceChannel voiceChannel) {
        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> {
            if (voiceChannel.getMembers().isEmpty()) {
                deletePrivateVoiceChannel(voiceChannel);
            }
        }, CHANNEL_DELETION_DELAY, TimeUnit.SECONDS);
        deletionTasks.put(voiceChannel.getId(), scheduledFuture);
    }

    /**
     * Cancels the scheduled deletion of a voice channel.
     *
     * @param channelId the ID of the voice channel to cancel deletion for
     */
    private void cancelDeletionTask(String channelId) {
        ScheduledFuture<?> scheduledFuture = deletionTasks.remove(channelId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    /**
     * Updates the status of a voice channel (private or public).
     *
     * @param voiceChannel the voice channel to update
     * @param isPrivate    true to make the channel private, false to make it public
     */
    public void updateChannelStatus(VoiceChannel voiceChannel, boolean isPrivate) {
        String newName = voiceChannel.getName().replaceAll("-private|-public", "") + (isPrivate ? "-private" : "-public");
        LOGGER.info("Updating channel name to: " + newName);
        voiceChannel.getManager().setName(newName).queue(success -> {
            LOGGER.info("Channel name updated to: " + newName);
            VoiceChannelInfo info = VoiceChannelData.loadVoiceChannelData(voiceChannel.getId());
            if (info != null) {
                Member owner = voiceChannel.getGuild().getMemberById(info.getOwner());
                if (owner != null) {
                    setOwnerPermissions(voiceChannel, owner);
                    if (isPrivate) {
                        setPrivateChannelPermissions(voiceChannel, owner);
                    } else {
                        setPublicChannelPermissions(voiceChannel, owner);
                    }
                }
            }
            VoiceChannelData.updateChannelStatus(voiceChannel, isPrivate);
            LOGGER.info("Sending Data to VoiceChannelData class");
        });
    }
}
