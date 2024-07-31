package com.lotusacademy.Managers;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

public class LangManager {

    private Map<String, String> messages;
    private static LangManager instance;

    public LangManager(ConfigManager configManager) {
        File langFile = new File(configManager.getLanguageFile());
        if (!langFile.exists()) {
            configManager.copyDefaultLang(langFile);
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(langFile.toPath())) {
            messages = yaml.load(inputStream);
        } catch (IOException e) {
            System.err.println("Error loading language file: " + e.getMessage());
            throw new RuntimeException("Error loading language file: " + e.getMessage(), e);
        }
    }

    public static LangManager getInstance(ConfigManager configManager) {
        if (instance == null) {
            instance = new LangManager(configManager);
        }
        return instance;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "Message not found: " + key);
    }
}
