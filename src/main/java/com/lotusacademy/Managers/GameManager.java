package com.lotusacademy.Managers;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameManager {

    private static final String API_KEY = "79b5c443-1989-4184-883d-504b52f10476";
    private static final Map<String, String> GAME_ENDPOINTS;

    static {
        GAME_ENDPOINTS = new HashMap<>();
        GAME_ENDPOINTS.put("rocketleague", "rocket-league");
        GAME_ENDPOINTS.put("steam", "steam");
        GAME_ENDPOINTS.put("overwatch", "overwatch");
        GAME_ENDPOINTS.put("apexlegends", "apex-legends");
        GAME_ENDPOINTS.put("rainbowsixsiege", "r6");
        GAME_ENDPOINTS.put("valorant", "valorant");
        GAME_ENDPOINTS.put("leagueoflegends", "lol");
        GAME_ENDPOINTS.put("fortnite", "fortnite");
    }

    private final OkHttpClient client = new OkHttpClient();

    public GameStats fetchGameStats(String game, String userId, String platform) throws IOException {
        String endpoint = GAME_ENDPOINTS.get(game.toLowerCase());
        if (endpoint == null) {
            return null;
        }

        String url = "https://api.tracker.gg/api/v2/" + endpoint + "/standard/profile/" + platform + "/" + userId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("TRN-Api-Key", API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject jsonResponse = new JSONObject(response.body().string());
            JSONObject data = jsonResponse.getJSONObject("data");

            String iconUrl = data.getJSONObject("platformInfo").getString("avatarUrl");
            String username = data.getJSONObject("platformInfo").getString("platformUserHandle");
            JSONObject stats = data.getJSONObject("segments").getJSONObject(String.valueOf(0)).getJSONObject("stats");

            return new GameStats(game, username, platform, iconUrl, stats.toString());
        }
    }

    public static class GameStats {
        public String game;
        public String username;
        public String platform;
        public String iconUrl;
        public String stats;

        public GameStats(String game, String username, String platform, String iconUrl, String stats) {
            this.game = game;
            this.username = username;
            this.platform = platform;
            this.iconUrl = iconUrl;
            this.stats = stats;
        }
    }
}
