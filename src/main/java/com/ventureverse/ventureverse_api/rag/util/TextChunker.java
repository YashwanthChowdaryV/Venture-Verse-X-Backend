package com.ventureverse.ventureverse_api.rag.util;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    private TextChunker() {}

    public static List<String> chunk(
            String text,
            int chunkSize) {

        List<String> chunks =
                new ArrayList<>();

        int start = 0;

        while (start < text.length()) {

            int end =
                    Math.min(
                            start + chunkSize,
                            text.length()
                    );

            chunks.add(
                    text.substring(
                            start,
                            end
                    )
            );

            start = end;
        }

        return chunks;
    }
}