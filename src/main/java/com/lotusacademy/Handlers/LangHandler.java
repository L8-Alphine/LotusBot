package com.lotusacademy.Handlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class LangHandler {
    private Properties lang;
    private String defaultLanguageFile;

    public LangHandler(String langFile) {
        this(langFile, null);
    }

    public LangHandler(String langFile, String defaultLangFile) {
        this.defaultLanguageFile = defaultLangFile;
        lang = new Properties();
        try {
            FileInputStream langFileStream = new FileInputStream(langFile);
            lang.load(langFileStream);
        } catch (IOException e) {
            System.err.println("Error loading language file: " + e.getMessage());
            if (defaultLangFile != null) {
                try {
                    FileInputStream defaultLangFileStream = new FileInputStream(defaultLangFile);
                    lang.load(defaultLangFileStream);
                } catch (IOException ex) {
                    System.err.println("Error loading default language file: " + ex.getMessage());
                    throw new RuntimeException("Error loading language files", ex);
                }
            } else {
                throw new RuntimeException("Error loading language file: " + e.getMessage(), e);
            }
        }
    }

    public String getMessage(String key) {
        String message = lang.getProperty(key);
        if (message != null) {
            message = message.replace("\"", "");
        } else {
            message = "Message not found for key: " + key;
        }
        return message;
    }

    public String getMessage(String key, String defaultMessage) {
        String message = lang.getProperty(key, defaultMessage);
        if (message != null) {
            message = message.replace("\"", "");
        }
        return message;
    }

    public String getFormattedMessage(String key, Object... args) {
        String message = getMessage(key);
        if (message != null) {
            return String.format(message, args);
        }
        return "Message not found for key: " + key;
    }

    public void setMessage(String key, String message) {
        lang.setProperty(key, message);
    }

    public String getLocalizedMessage(String key, String langFile) {
        Properties localizedLang = new Properties();
        try {
            FileInputStream langFileStream = new FileInputStream(langFile);
            localizedLang.load(langFileStream);
            String message = localizedLang.getProperty(key);
            if (message != null) {
                return message.replace("\"", "");
            } else {
                return "Message not found for key: " + key + " in language file: " + langFile;
            }
        } catch (IOException e) {
            return "Error loading language file: " + e.getMessage();
        }
    }
}
