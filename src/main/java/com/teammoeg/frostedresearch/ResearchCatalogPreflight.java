/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teammoeg.chorda.io.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Registry-independent Gradle preflight; runtime loading additionally executes the full production codecs. */
public final class ResearchCatalogPreflight {
    private ResearchCatalogPreflight() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ResearchCatalogPreflight <catalogue-directory>");
        }
        int count = validate(Path.of(args[0]));
        System.out.println("Preflight validated " + count + " research definitions in " + args[0]);
    }

    public static int validate(Path directory) {
        List<String> errors = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            throw new ResearchCatalog.ValidationException(List.of("Research catalogue directory is missing: " + directory));
        }
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(files::add);
        } catch (IOException e) {
            throw new ResearchCatalog.ValidationException(List.of("Cannot list catalogue: " + e.getMessage()));
        }
        if (files.isEmpty()) {
            throw new ResearchCatalog.ValidationException(List.of("Research catalogue contains no JSON definitions"));
        }

        Map<String, RawResearch> definitions = new LinkedHashMap<>();
        Map<String, String> researchIds = new HashMap<>();
        for (Path file : files) {
            String filename = file.getFileName().toString();
            String id = filename.substring(0, filename.length() - 5);
            try {
                JsonElement parsed = JsonParser.parseString(FileUtil.readString(file.toFile()));
                if (!parsed.isJsonObject()) {
                    errors.add(filename + ": root value must be an object");
                    continue;
                }
                JsonObject json = parsed.getAsJsonObject();
                RawResearch definition = readDefinition(id, json, errors);
                if (definitions.putIfAbsent(id, definition) != null) {
                    errors.add("Duplicate research id: " + id);
                }
                register(errors, researchIds, id, id, "research id");
                for (String legacyId : definition.legacyIds()) {
                    register(errors, researchIds, legacyId, id, "research legacy id");
                }
            } catch (Exception e) {
                errors.add(filename + ": " + e.getMessage());
            }
        }
        for (RawResearch definition : definitions.values()) {
            for (String parent : definition.parents()) {
                if (parent.equals(definition.id())) {
                    errors.add(definition.id() + ": research cannot parent itself");
                } else if (!definitions.containsKey(parent)) {
                    errors.add(definition.id() + ": missing parent " + parent);
                }
            }
        }
        detectCycles(definitions, errors);
        if (!errors.isEmpty()) {
            throw new ResearchCatalog.ValidationException(errors);
        }
        return definitions.size();
    }

    private static RawResearch readDefinition(String id, JsonObject json, List<String> errors) {
        if (!json.has("points") || json.get("points").getAsLong() <= 0) {
            errors.add(id + ": points must be greater than zero");
        }
        if (json.has("insight") && json.get("insight").getAsInt() < 0) {
            errors.add(id + ": insight must not be negative");
        }
        validatePositiveCounts(id + "/ingredient", array(json, "ingredients"), errors, false);
        validateClues(id, array(json, "clues"), errors);
        validateScopedIds(id + "/effect", array(json, "effects"), errors);
        return new RawResearch(id, strings(json, "parents"), strings(json, "legacyIds"));
    }

    private static void validateClues(String researchId, JsonArray clues, List<String> errors) {
        validateScopedIds(researchId + "/clue", clues, errors);
        for (JsonElement element : clues) {
            if (!element.isJsonObject()) continue;
            JsonObject clue = element.getAsJsonObject();
            String id = clue.has("id") ? clue.get("id").getAsString() : "<missing>";
            String owner = researchId + "/clue/" + id;
            if (!clue.has("value")) {
                errors.add(owner + ": contribution is required");
            } else {
                double value = clue.get("value").getAsDouble();
                if (!Double.isFinite(value) || value < 0 || value > 1) {
                    errors.add(owner + ": contribution must be finite and within [0,1]");
                }
            }
            if ("game".equals(string(clue, "type"))) {
                int level = clue.has("level") ? clue.get("level").getAsInt() : -1;
                if (level < 0 || level > 3) errors.add(owner + ": minigame level must be within [0,3]");
            }
            if ("item".equals(string(clue, "type")) && clue.has("item")) {
                int count = count(clue.get("item"));
                if (count <= 0) errors.add(owner + ": item count must be greater than zero");
            }
        }
    }

    private static void validatePositiveCounts(
            String owner, JsonArray values, List<String> errors, boolean requireCount) {
        int index = 0;
        for (JsonElement value : values) {
            int count = count(value);
            if ((requireCount || value.isJsonObject()) && count <= 0) {
                errors.add(owner + "[" + index + "]: count must be greater than zero");
            }
            index++;
        }
    }

    private static int count(JsonElement value) {
        if (!value.isJsonObject()) return 1;
        JsonObject object = value.getAsJsonObject();
        return object.has("count") ? object.get("count").getAsInt() : 1;
    }

    private static void validateScopedIds(String scope, JsonArray entries, List<String> errors) {
        Map<String, String> ids = new HashMap<>();
        int index = 0;
        for (JsonElement entry : entries) {
            if (!entry.isJsonObject()) {
                errors.add(scope + "[" + index + "]: entry must be an object");
                index++;
                continue;
            }
            JsonObject object = entry.getAsJsonObject();
            String owner = scope + "[" + index + "]";
            String id = string(object, "id");
            register(errors, ids, id, owner, "id");
            for (String legacyId : strings(object, "legacyIds")) {
                register(errors, ids, legacyId, owner, "legacy id");
            }
            index++;
        }
    }

    private static void register(
            List<String> errors, Map<String, String> ids, String id, String owner, String kind) {
        if (id == null || id.isBlank()) {
            errors.add(owner + ": " + kind + " must not be blank");
            return;
        }
        if (id.length() > ResearchCatalog.MAX_STABLE_ID_LENGTH) {
            errors.add(owner + ": " + kind + " exceeds "
                    + ResearchCatalog.MAX_STABLE_ID_LENGTH + " characters");
            return;
        }
        String previous = ids.putIfAbsent(id, owner);
        if (previous != null) errors.add(owner + ": " + kind + " " + id + " conflicts with " + previous);
    }

    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(object, key)) values.add(element.getAsString());
        return values;
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) ? object.get(key).getAsString() : null;
    }

    private static void detectCycles(Map<String, RawResearch> definitions, List<String> errors) {
        Map<String, Integer> state = new HashMap<>();
        Deque<String> path = new ArrayDeque<>();
        Set<String> reported = new HashSet<>();
        for (String id : definitions.keySet()) visit(id, definitions, state, path, reported, errors);
    }

    private static void visit(String id, Map<String, RawResearch> definitions, Map<String, Integer> state,
                              Deque<String> path, Set<String> reported, List<String> errors) {
        int current = state.getOrDefault(id, 0);
        if (current == 2) return;
        if (current == 1) {
            if (reported.add(id)) errors.add("Research parent cycle detected at " + id + " through " + path);
            return;
        }
        state.put(id, 1);
        path.addLast(id);
        for (String parent : definitions.get(id).parents()) {
            if (definitions.containsKey(parent)) visit(parent, definitions, state, path, reported, errors);
        }
        path.removeLast();
        state.put(id, 2);
    }

    private record RawResearch(String id, List<String> parents, List<String> legacyIds) {
    }
}
