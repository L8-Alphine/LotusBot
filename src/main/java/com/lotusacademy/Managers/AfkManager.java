package com.lotusacademy.Managers;

import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AfkManager {

    private static final String FILE_PATH = "afk_data.json";
    private final Map<String, AfkInfo> afkMap = new HashMap<>();
    private final Logger LOGGER = Logger.getLogger(AfkManager.class.getName());

    public AfkManager() {
        loadAfkData();
        LOGGER.info("AfkManager initialized");
    }

    public void setAfk(User user, String message) {
        afkMap.put(user.getId(), new AfkInfo(message, System.currentTimeMillis()));
        saveAfkData();
    }

    public void removeAfk(User user) {
        afkMap.remove(user.getId());
        saveAfkData();
    }

    public boolean isAfk(User user) {
        return afkMap.containsKey(user.getId());
    }

    public AfkInfo getAfkInfo(User user) {
        return afkMap.get(user.getId());
    }

    private void loadAfkData() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write("{}"); // Initialize the file with an empty JSON object
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (file.length() == 0) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("{}"); // Initialize the file with an empty JSON object if it is empty
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileReader reader = new FileReader(file)) {
                JSONObject jsonObject = new JSONObject(new JSONTokener(reader));
                jsonObject.keys().forEachRemaining(key -> {
                    JSONObject afkInfoJson = jsonObject.getJSONObject(key);
                    afkMap.put(key, new AfkInfo(afkInfoJson.getString("message"), afkInfoJson.getLong("timestamp")));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveAfkData() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileWriter writer = new FileWriter(file)) {
            JSONObject jsonObject = new JSONObject();
            afkMap.forEach((key, afkInfo) -> {
                JSONObject afkInfoJson = new JSONObject();
                afkInfoJson.put("message", afkInfo.getMessage());
                afkInfoJson.put("timestamp", afkInfo.getTimestamp());
                jsonObject.put(key, afkInfoJson);
            });
            writer.write(jsonObject.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class AfkInfo {
        private final String message;
        private final long timestamp;

        public AfkInfo(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
