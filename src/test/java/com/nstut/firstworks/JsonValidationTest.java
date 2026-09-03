package com.nstut.firstworks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    public void workshopSchemaUsesSupportedComponentTypes() throws Exception {
        Path schema = Path.of("src/main/resources/data/firstworks/kubejs/recipe_schema/workshop_processing.json");
        try (FileReader reader = new FileReader(schema.toFile())) {
            var keys = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("keys");
            var catalyst = keys.asList().stream()
                    .map(element -> element.getAsJsonObject())
                    .filter(key -> key.get("name").getAsString().equals("catalyst"))
                    .findFirst()
                    .orElseThrow();
            var consumeCatalyst = keys.asList().stream()
                    .map(element -> element.getAsJsonObject())
                    .filter(key -> key.get("name").getAsString().equals("consume_catalyst"))
                    .findFirst()
                    .orElseThrow();
            assertEquals("optional_ingredient", catalyst.get("type").getAsString());
            assertEquals("boolean", consumeCatalyst.get("type").getAsString());
        }
    }
}
