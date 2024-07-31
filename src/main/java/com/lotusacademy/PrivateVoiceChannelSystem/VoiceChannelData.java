package com.lotusacademy.PrivateVoiceChannelSystem;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VoiceChannelData {
    public static final String VOICE_CHANNELS_PATH = "./voicechannels/";

    public static void saveVoiceChannelData(VoiceChannel voiceChannel, String creatorId, String ownerId, int userLimit, boolean isPrivate, List<String> users) {
        try {
            Path directoryPath = Paths.get(VOICE_CHANNELS_PATH);
            Files.createDirectories(directoryPath);

            Path filePath = directoryPath.resolve(voiceChannel.getId() + ".yml");
            StringBuilder content = new StringBuilder();
            content.append("channelId: ").append(voiceChannel.getId()).append("\n");
            content.append("creator: ").append(creatorId).append("\n");
            content.append("owner: ").append(ownerId).append("\n");
            content.append("userLimit: ").append(userLimit).append("\n");
            content.append("status: ").append(isPrivate ? "private" : "public").append("\n");
            content.append("users:\n");
            for (String user : users) {
                content.append("  - ").append(user).append("\n");
            }

            Files.write(filePath, content.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteVoiceChannelData(VoiceChannel voiceChannel) {
        try {
            Path filePath = Paths.get(VOICE_CHANNELS_PATH, voiceChannel.getId() + ".yml");
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static VoiceChannelInfo loadVoiceChannelData(String channelId) {
        Path filePath = Paths.get(VOICE_CHANNELS_PATH, channelId + ".yml");
        if (Files.exists(filePath)) {
            try {
                List<String> lines = Files.readAllLines(filePath);
                String creator = null;
                String owner = null;
                int userLimit = 0;
                boolean isPrivate = false;
                List<String> users = new ArrayList<>();
                for (String line : lines) {
                    if (line.startsWith("creator:")) {
                        creator = line.split(": ")[1].trim();
                    } else if (line.startsWith("owner:")) {
                        owner = line.split(": ")[1].trim();
                    } else if (line.startsWith("userLimit:")) {
                        userLimit = Integer.parseInt(line.split(": ")[1].trim());
                    } else if (line.startsWith("status:")) {
                        isPrivate = "private".equalsIgnoreCase(line.split(": ")[1].trim());
                    } else if (line.startsWith("  - ")) {
                        users.add(line.split("- ")[1].trim());
                    }
                }
                return new VoiceChannelInfo(channelId, creator, owner, userLimit, isPrivate, users);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void addUserToChannel(VoiceChannel voiceChannel, String userId) {
        Path filePath = Paths.get(VOICE_CHANNELS_PATH, voiceChannel.getId() + ".yml");
        try {
            List<String> lines = Files.readAllLines(filePath);
            lines.add("  - " + userId);
            Files.write(filePath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeUserFromChannel(VoiceChannel voiceChannel, String userId) {
        Path filePath = Paths.get(VOICE_CHANNELS_PATH, voiceChannel.getId() + ".yml");
        try {
            List<String> lines = Files.readAllLines(filePath);
            lines.removeIf(line -> line.trim().equals("- " + userId));
            Files.write(filePath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<VoiceChannelInfo> getChannelsForUser(String userId) {
        List<VoiceChannelInfo> channels = new ArrayList<>();
        try {
            Files.list(Paths.get(VOICE_CHANNELS_PATH)).forEach(path -> {
                String channelId = path.getFileName().toString().replace(".yml", "");
                VoiceChannelInfo info = loadVoiceChannelData(channelId);
                if (info != null && info.getUsers().contains(userId)) {
                    channels.add(info);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return channels;
    }

    public static void updateUserLimit(VoiceChannel voiceChannel, int userLimit) {
        Path filePath = Paths.get(VOICE_CHANNELS_PATH, voiceChannel.getId() + ".yml");
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("userLimit:")) {
                    lines.set(i, "userLimit: " + userLimit);
                    break;
                }
            }
            Files.write(filePath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateChannelStatus(VoiceChannel voiceChannel, boolean isPrivate) {
        Logger.getLogger(VoiceChannelData.class.getName()).info("Voice Channel Change data received: " + voiceChannel.getName() + " " + isPrivate);
        Path filePath = Paths.get(VOICE_CHANNELS_PATH, voiceChannel.getId() + ".yml");
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("status:")) {
                    lines.set(i, "status: " + (isPrivate ? "private" : "public"));
                    Logger.getLogger(VoiceChannelData.class.getName()).info("Channel status updated: " + (isPrivate ? "private" : "public"));
                    break;
                }
            }
            Files.write(filePath, lines);
            Logger.getLogger(VoiceChannelData.class.getName()).info("File written...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class VoiceChannelInfo {
        private final String channelId;
        private final String creator;
        private final String owner;
        private final int userLimit;
        private final boolean isPrivate;
        private final List<String> users;

        public VoiceChannelInfo(String channelId, String creator, String owner, int userLimit, boolean isPrivate, List<String> users) {
            this.channelId = channelId;
            this.creator = creator;
            this.owner = owner;
            this.userLimit = userLimit;
            this.isPrivate = isPrivate;
            this.users = users;
        }

        public String getChannelId() {
            return channelId;
        }

        public String getCreator() {
            return creator;
        }

        public String getOwner() {
            return owner;
        }

        public int getUserLimit() {
            return userLimit;
        }

        public boolean isPrivate() {
            return isPrivate;
        }

        public List<String> getUsers() {
            return users;
        }

        public void addUser(String userId) {
            if (!users.contains(userId)) {
                users.add(userId);
            }
        }

        public void removeUser(String userId) {
            users.remove(userId);
        }
    }
}
