package com.ventureverse.ventureverse_api.util;

public class JsonCleaner {

    private JsonCleaner() {}

    public static String clean(String response) {

        if (response == null) {
            return "";
        }

        return response
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}