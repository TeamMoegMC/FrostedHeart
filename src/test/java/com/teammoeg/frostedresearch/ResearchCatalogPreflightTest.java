/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchCatalogPreflightTest {
    @TempDir
    Path directory;

    @Test
    void validCatalogIsAccepted() throws IOException {
        write("root.json", definition(100, "[]", "[]", "[]"));
        write("child.json", definition(200, "[\"root\"]", "[]", "[]"));

        assertEquals(2, ResearchCatalogPreflight.validate(directory));
    }

    @Test
    void validationAggregatesDeterministicallyOrderedDiagnostics() throws IOException {
        write("b.json", """
                {"points":0,"insight":-1,"parents":["missing"],
                 "clues":[{"type":"game","id":"same","legacyIds":["same"],"value":2,"level":4}],
                 "effects":[{"type":"custom","id":""}]}
                """);
        write("a.json", """
                {"points":100,"parents":["a"],"legacyIds":["b"],
                 "clues":[{"type":"item","id":"item","value":0.5,"item":{"item":"minecraft:stone","count":0}}],
                 "effects":[]}
                """);

        ResearchCatalog.ValidationException failure = assertThrows(
                ResearchCatalog.ValidationException.class,
                () -> ResearchCatalogPreflight.validate(directory));

        String message = failure.getMessage();
        assertTrue(message.indexOf("a: research cannot parent itself")
                < message.indexOf("b: missing parent missing"));
        assertTrue(message.contains("points must be greater than zero"));
        assertTrue(message.contains("insight must not be negative"));
        assertTrue(message.contains("contribution must be finite and within [0,1]"));
        assertTrue(message.contains("minigame level must be within [0,3]"));
        assertTrue(message.contains("item count must be greater than zero"));
        assertTrue(message.contains("legacy id same conflicts"));
        assertTrue(message.contains("effect[0]: id must not be blank"));
        assertTrue(message.contains("research id b conflicts"));
    }

    @Test
    void cyclesAndEmptyCatalogsAreRejected() throws IOException {
        ResearchCatalog.ValidationException empty = assertThrows(
                ResearchCatalog.ValidationException.class,
                () -> ResearchCatalogPreflight.validate(directory));
        assertTrue(empty.getMessage().contains("contains no JSON definitions"));

        write("a.json", definition(100, "[\"b\"]", "[]", "[]"));
        write("b.json", definition(100, "[\"a\"]", "[]", "[]"));
        ResearchCatalog.ValidationException cycle = assertThrows(
                ResearchCatalog.ValidationException.class,
                () -> ResearchCatalogPreflight.validate(directory));
        assertTrue(cycle.getMessage().contains("cycle"));
    }

    @Test
    void packetBoundStableIdsAreRejectedDuringCatalogValidation() throws IOException {
        write("root.json", """
                {"points":100,"parents":[],"legacyIds":["%s"],"clues":[],"effects":[]}
                """.formatted("x".repeat(ResearchCatalog.MAX_STABLE_ID_LENGTH + 1)));

        ResearchCatalog.ValidationException failure = assertThrows(
                ResearchCatalog.ValidationException.class,
                () -> ResearchCatalogPreflight.validate(directory));

        assertTrue(failure.getMessage().contains("exceeds 128 characters"));
    }

    private static String definition(int points, String parents, String clues, String effects) {
        return "{\"points\":" + points + ",\"parents\":" + parents
                + ",\"clues\":" + clues + ",\"effects\":" + effects + "}";
    }

    private void write(String name, String contents) throws IOException {
        Files.writeString(directory.resolve(name), contents);
    }
}
