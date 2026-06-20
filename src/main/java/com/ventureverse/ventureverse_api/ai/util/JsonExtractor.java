package com.ventureverse.ventureverse_api.ai.util;

public class JsonExtractor {

    private JsonExtractor() {
    }

    public static String extractJson(
            String response) {

        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");

        if (start == -1 || end == -1) {
            throw new RuntimeException(
                    "No JSON found in AI response"
            );
        }

        return response.substring(
                start,
                end + 1
        );
    }
}