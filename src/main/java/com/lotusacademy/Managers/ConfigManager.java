package com.lotusacademy.Managers;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.yml";
    private static final String LANG_FILE = "lang/en_messages.yml";
    private static final String DEFAULT_CONFIG_FILE = "/default_config.yml";
    private static final String DEFAULT_LANG_FILE = "/en_messages.yml";
    private static Map<String, Object> config;
    private static ConfigManager configManager = new ConfigManager();

    public ConfigManager() {
        File configFile = new File(CONFIG_FILE);
        File langFile = new File(LANG_FILE);

        if (!configFile.exists()) {
            copyDefaultConfig(configFile);
        }

        if (!langFile.exists()) {
            copyDefaultLang(langFile);
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(configFile.toPath())) {
            config = yaml.load(inputStream);
        } catch (Exception e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            throw new IllegalArgumentException("Error loading configuration file: " + e.getMessage(), e);
        }
    }

    public Object get(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = config;
        for (int i = 0; i < keys.length - 1; i++) {
            Object value = currentMap.get(keys[i]);
            if (value instanceof Map) {
                currentMap = (Map<String, Object>) value;
            } else {
                return null;
            }
        }
        return currentMap.get(keys[keys.length - 1]);
    }

    private void copyDefaultConfig(File configFile) {
        try (InputStream inputStream = getClass().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Default configuration resource not found: " + DEFAULT_CONFIG_FILE);
            }
            Files.copy(inputStream, configFile.toPath());
        } catch (Exception e) {
            System.err.println("Failed to create default config file: " + e.getMessage());
            throw new RuntimeException("Failed to create default config file: " + e.getMessage(), e);
        }
    }

    void copyDefaultLang(File langFile) {
        try {
            // Ensure the directory exists
            Files.createDirectories(langFile.getParentFile().toPath());

            // Now copy the file
            try (InputStream inputStream = getClass().getResourceAsStream(DEFAULT_LANG_FILE)) {
                if (inputStream == null) {
                    throw new IllegalArgumentException("Default language resource not found: " + DEFAULT_LANG_FILE);
                }
                Files.copy(inputStream, langFile.toPath());
            } catch (Exception e) {
                System.err.println("Failed to create default language file: " + e.getMessage());
                throw new RuntimeException("Failed to create default language file: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            System.err.println("Failed to create lang directory: " + e.getMessage());
            throw new RuntimeException("Failed to create lang directory: " + e.getMessage(), e);
        }
    }

    // Main Config Methods
    public String getToken() {
        return (String) get("token");
    }

    public String getLanguageFile() {
        String langCode = (String) get("language");
        return "lang/" + langCode + "_messages.yml";
    }

    public static String getLogChannelId() {
        return (String) configManager.get("logging_channel_id");
    }

    public String getMainGuildID() {
        return (String) get("main_guild_id");
    }

    public Map<String, String> getAllowedGuilds() {
        return (Map<String, String>) get("allowed_guilds");
    }

    public List<List<String>> getStatuses() {
        return (List<List<String>>) get("statuses");
    }

    public String getOwnerID() {
        return (String) get("owner_id");
    }

    public String getBotAdminsIDs() {
        return (String) get("bot_admins");
    }

    public String getSupportId() {
        return (String) get("support_role_id");
    }
    // End of Main Config Methods

    // Welcome System Config Methods
    public boolean isWelcomeEnabled() {
        Map<String, Object> welcomeConfig = (Map<String, Object>) get("WelcomeSystem");
        return (boolean) welcomeConfig.get("enabled");
    }

    public boolean useWelcomeEmbeds() {
        Map<String, Object> welcomeConfig = (Map<String, Object>) get("WelcomeSystem");
        Boolean useEmbeds = (Boolean) welcomeConfig.get("use_embed");
        return useEmbeds != null && useEmbeds;
    }

    public String getWelcomeChannel() {
        Map<String, Object> welcomeConfig = (Map<String, Object>) get("WelcomeSystem");
        return (String) welcomeConfig.get("welcome_channel_id");
    }
    // End of Welcome System Config Methods
}
