package com.apilens.qa.fixtures;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class FileFactory {

    private FileFactory() {}

    public static String uniqueFileName(String ext) {
        return "file_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;
    }

    public static byte[] textContent() {
        return ("content_" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
    }
}
