package com.example.platemate;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class OpenAIRequest {

    public static RequestBody createRequestBody(String model, String userPrompt, String imageUrl) {
        String json = "{"
                + "\"model\": \"" + model + "\","
                + "\"messages\": ["
                + "    {"
                + "        \"role\": \"user\","
                + "        \"content\": ["
                + "            {"
                + "                \"type\": \"text\","
                + "                \"text\": \"" + escapeString(userPrompt) + "\""
                + "            },"
                + "            {"
                + "                \"type\": \"image_url\","
                + "                \"image_url\": {"
                + "                    \"url\": \"" + escapeString(imageUrl) + "\""
                + "                }"
                + "            }"
                + "        ]"
                + "    }"
                + "]"
                + "}";
        System.out.println("Generated JSON: " + json);


        return RequestBody.create(MediaType.parse("application/json"), json);
    }


    private static String escapeString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
