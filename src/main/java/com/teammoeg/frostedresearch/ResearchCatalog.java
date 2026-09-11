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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.teammoeg.chorda.io.FileUtil;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.Clue;
import com.teammoeg.frostedresearch.research.clues.ItemClue;
import com.teammoeg.frostedresearch.research.clues.MinigameClue;
import com.teammoeg.frostedresearch.research.effects.Effect;
import net.minecraft.world.item.crafting.Ingredient;

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

/** Parses and validates an entire research catalogue before it is installed. */
public final class ResearchCatalog {
    public static final int MAX_STABLE_ID_LENGTH = 128;

    private ResearchCatalog() {
    }

    public static Candidate load(Path directory) {
        List<String> errors = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            throw new ValidationException(List.of("Research catalogue directory is missing: " + directory));
        }
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(files::add);
        } catch (IOException e) {
            throw new ValidationException(List.of("Cannot list research catalogue " + directory + ": " + e.getMessage()));
        }
        if (files.isEmpty()) {
            throw new ValidationException(List.of("Research catalogue contains no JSON definitions: " + directory));
        }

        List<Research> definitions = new ArrayList<>(files.size());
        for (Path file : files) {
            String filename = file.getFileName().toString();
            String id = filename.substring(0, filename.length() - 5);
            try {
                JsonElement json = JsonParser.parseString(FileUtil.readString(file.toFile()));
                if (!json.isJsonObject()) {
                    errors.add(filename + ": root value must be a JSON object");
                    continue;
                }
                Research parsed = Research.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(message -> errors.add(filename + ": " + message))
                        .orElse(null);
                if (parsed != null) {
                    parsed.setId(id);
                    definitions.add(parsed);
                }
            } catch (Exception e) {
                errors.add(filename + ": " + e.getMessage());
            }
        }
        errors.addAll(validate(definitions));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return new Candidate(List.copyOf(definitions));
    }

    public static List<String> validate(List<Research> definitions) {
        List<String> errors = new ArrayList<>();
        Map<String, Research> byId = new LinkedHashMap<>();
        Map<String, String> researchIdOwners = new HashMap<>();
        for (Research research : definitions) {
            String id = research.getId();
            if (id == null || id.isBlank()) {
                errors.add("Research definition has a blank id");
                continue;
            }
            if (byId.putIfAbsent(id, research) != null) {
                errors.add("Duplicate research id: " + id);
            }
            registerId(errors, researchIdOwners, id, id, "research");
        }

        for (Research research : definitions) {
            String id = research.getId();
            for (String legacyId : research.getLegacyIds()) {
                registerId(errors, researchIdOwners, legacyId, id, "research legacy id");
            }
            if (research.getRequiredPoints() <= 0) {
                errors.add(id + ": points must be greater than zero");
            }
            if (research.getInsight() < 0) {
                errors.add(id + ": insight must not be negative");
            }
            for (Pair<Ingredient, Integer> required : research.getRequiredItems()) {
                if (required.getSecond() == null || required.getSecond() <= 0) {
                    errors.add(id + ": required item counts must be greater than zero");
                }
            }
            for (String parentId : research.getParentIds()) {
                if (parentId == null || parentId.isBlank()) {
                    errors.add(id + ": parent id must not be blank");
                } else if (parentId.equals(id)) {
                    errors.add(id + ": research cannot parent itself");
                } else if (!byId.containsKey(parentId)) {
                    errors.add(id + ": missing parent " + parentId);
                }
            }
            validateClues(errors, research);
            validateEffects(errors, research);
        }
        validateCycles(errors, byId);
        return List.copyOf(errors);
    }

    private static void validateClues(List<String> errors, Research research) {
        Map<String, String> ids = new HashMap<>();
        for (Clue clue : research.getClues()) {
            String owner = research.getId() + "/clue/" + clue.getNonce();
            registerId(errors, ids, clue.getNonce(), owner, "clue id");
            for (String legacyId : clue.getLegacyIds()) {
                registerId(errors, ids, legacyId, owner, "clue legacy id");
            }
            float contribution = clue.getResearchContribution();
            if (!Float.isFinite(contribution) || contribution < 0 || contribution > 1) {
                errors.add(owner + ": contribution must be finite and within [0,1]");
            }
            if (clue instanceof MinigameClue minigame
                    && (minigame.getLevel() < 0 || minigame.getLevel() > 3)) {
                errors.add(owner + ": minigame level must be within [0,3]");
            }
            if (clue instanceof ItemClue item && item.getRequiredCount() <= 0) {
                errors.add(owner + ": item count must be greater than zero");
            }
        }
    }

    private static void validateEffects(List<String> errors, Research research) {
        Map<String, String> ids = new HashMap<>();
        for (Effect effect : research.getEffects()) {
            String owner = research.getId() + "/effect/" + effect.getNonce();
            registerId(errors, ids, effect.getNonce(), owner, "effect id");
            for (String legacyId : effect.getLegacyIds()) {
                registerId(errors, ids, legacyId, owner, "effect legacy id");
            }
        }
    }

    private static void registerId(
            List<String> errors, Map<String, String> owners, String id, String owner, String kind) {
        if (id == null || id.isBlank()) {
            errors.add(owner + ": " + kind + " must not be blank");
            return;
        }
        if (id.length() > MAX_STABLE_ID_LENGTH) {
            errors.add(owner + ": " + kind + " exceeds " + MAX_STABLE_ID_LENGTH + " characters");
            return;
        }
        String previous = owners.putIfAbsent(id, owner);
        if (previous != null) {
            errors.add(owner + ": " + kind + " " + id + " conflicts with " + previous);
        }
    }

    private static void validateCycles(List<String> errors, Map<String, Research> byId) {
        Map<String, Integer> state = new HashMap<>();
        Deque<String> path = new ArrayDeque<>();
        Set<String> reported = new HashSet<>();
        for (String id : byId.keySet()) {
            visit(id, byId, state, path, reported, errors);
        }
    }

    private static void visit(
            String id,
            Map<String, Research> byId,
            Map<String, Integer> state,
            Deque<String> path,
            Set<String> reported,
            List<String> errors) {
        int current = state.getOrDefault(id, 0);
        if (current == 2) {
            return;
        }
        if (current == 1) {
            if (reported.add(id)) {
                errors.add("Research parent cycle detected at " + id + " through " + path);
            }
            return;
        }
        state.put(id, 1);
        path.addLast(id);
        Research research = byId.get(id);
        if (research != null) {
            for (String parentId : research.getParentIds()) {
                if (byId.containsKey(parentId)) {
                    visit(parentId, byId, state, path, reported, errors);
                }
            }
        }
        path.removeLast();
        state.put(id, 2);
    }

    public record Candidate(List<Research> definitions) {
    }

    public static final class ValidationException extends IllegalStateException {
        private final List<String> diagnostics;

        public ValidationException(List<String> diagnostics) {
            super("Invalid research catalogue:\n - " + String.join("\n - ", diagnostics));
            this.diagnostics = List.copyOf(diagnostics);
        }

        public List<String> diagnostics() {
            return diagnostics;
        }
    }
}
