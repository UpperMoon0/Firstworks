package com.nstut.firstworks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JsonValidationTest {
    @Test
    public void validateAllJsonResources() throws Exception {
        Path resourcesDir = Path.of("src/main/resources");
        if (!Files.exists(resourcesDir)) return;

        try (Stream<Path> stream = Files.walk(resourcesDir)) {
            List<Path> jsonFiles = stream.filter(p -> p.toString().endsWith(".json")).toList();
            assertFalse(jsonFiles.isEmpty(), "Should have JSON resources to validate");

            for (Path jsonFile : jsonFiles) {
                assertDoesNotThrow(() -> {
                    try (FileReader reader = new FileReader(jsonFile.toFile())) {
                        JsonParser.parseReader(reader);
                    }
                }, "Malformed JSON in resource: " + jsonFile);
            }
        }
    }
}