package com.example.platemate;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class OpenAIRequest {

    public static RequestBody createRequestBody(String model, String userPrompt, String imageUrl) {
        // Construct the JSON string with proper formatting for text and image_url
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

        // Log the generated JSON for debugging
        System.out.println("Generated JSON: " + json);

        // Return the JSON as a RequestBody
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // Helper method to escape special characters
    private static String escapeString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
