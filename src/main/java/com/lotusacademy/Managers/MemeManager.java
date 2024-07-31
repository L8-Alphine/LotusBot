package com.lotusacademy.Managers;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Random;

public class MemeManager {
    private static final String IMGFLIP_USERNAME = "Alphine";
    private static final String IMGFLIP_PASSWORD = "JustSomeRandomPassword";
    private static final String[] GAMING_MEME_TEMPLATES = {
            "101470", // "Distracted Boyfriend"
            "87743020", // "Two Buttons"
            "61579", // "One Does Not Simply"
            "80707627", // "Sad Pablo Escobar"
            "112126428", // "Finding Neverland"
    };

    private final OkHttpClient client = new OkHttpClient();
    private final Random random = new Random();

    public String fetchRandomMeme() throws IOException {
        String templateId = GAMING_MEME_TEMPLATES[random.nextInt(GAMING_MEME_TEMPLATES.length)];
        String url = "https://api.imgflip.com/caption_image?" +
                "template_id=" + templateId +
                "&username=" + IMGFLIP_USERNAME +
                "&password=" + IMGFLIP_PASSWORD +
                "&text0=" + "When you realize gaming is life" +
                "&text1=" + "But so is sleep";

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JSONObject jsonResponse = new JSONObject(response.body().string());
            if (!jsonResponse.getBoolean("success")) {
                throw new IOException("Failed to generate meme: " + jsonResponse.getString("error_message"));
            }

            return jsonResponse.getJSONObject("data").getString("url");
        }
    }
}
