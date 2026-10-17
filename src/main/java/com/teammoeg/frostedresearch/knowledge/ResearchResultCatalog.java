/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Last-known-good, immutable catalogue of phase-one research results. */
public final class ResearchResultCatalog {
    private static Snapshot current = Snapshot.empty();
    private static long nextRevision;

    private ResearchResultCatalog() {
    }

    public static synchronized Snapshot current() {
        return current;
    }

    public static synchronized Snapshot install(Candidate candidate) {
        current = Snapshot.create(++nextRevision, candidate.topics(), candidate.profiles());
        return current;
    }

    public static synchronized void clearForTests() {
        current = Snapshot.empty();
        nextRevision = 0;
    }

    public record Candidate(
            Map<ResourceLocation, ResearchTopicDefinition> topics,
            Map<ResourceLocation, PrototypeProfileDefinition> profiles) {
        public Candidate {
            topics = immutableMap(topics);
            profiles = immutableMap(profiles);
        }
    }

    public record ResultEntry(ResourceLocation topicId, ResearchResult result) {
    }

    public static final class Snapshot {
        private final long revision;
        private final Map<ResourceLocation, ResearchTopicDefinition> topics;
        private final Map<ResourceLocation, PrototypeProfileDefinition> profiles;
        private final Map<ResourceLocation, ResultEntry> results;
        private final Set<ResourceLocation> managedRecipes;
        private final Set<ResourceLocation> managedMultiblocks;
        private final Set<ResourceLocation> managedBlocks;

        private Snapshot(long revision,
                Map<ResourceLocation, ResearchTopicDefinition> topics,
                Map<ResourceLocation, PrototypeProfileDefinition> profiles,
                Map<ResourceLocation, ResultEntry> results,
                Set<ResourceLocation> managedRecipes,
                Set<ResourceLocation> managedMultiblocks,
                Set<ResourceLocation> managedBlocks) {
            this.revision = revision;
            this.topics = topics;
            this.profiles = profiles;
            this.results = results;
            this.managedRecipes = managedRecipes;
            this.managedMultiblocks = managedMultiblocks;
            this.managedBlocks = managedBlocks;
        }

        private static Snapshot empty() {
            return new Snapshot(0, Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), Set.of());
        }

        private static Snapshot create(long revision,
                Map<ResourceLocation, ResearchTopicDefinition> topics,
                Map<ResourceLocation, PrototypeProfileDefinition> profiles) {
            Map<ResourceLocation, ResultEntry> results = new LinkedHashMap<>();
            Set<ResourceLocation> recipes = new LinkedHashSet<>();
            Set<ResourceLocation> multiblocks = new LinkedHashSet<>();
            Set<ResourceLocation> blocks = new LinkedHashSet<>();
            topics.forEach((topicId, topic) -> topic.results().forEach(result -> {
                results.put(result.id(), new ResultEntry(topicId, result));
                if (result instanceof ResearchResult.Design design) recipes.addAll(design.recipes());
                if (result instanceof ResearchResult.Construction construction) multiblocks.addAll(construction.multiblocks());
                if (result instanceof ResearchResult.Procedure procedure) blocks.addAll(procedure.usableBlocks());
            }));
            return new Snapshot(revision, immutableMap(topics), immutableMap(profiles),
                    immutableMap(results), immutableSet(recipes), immutableSet(multiblocks), immutableSet(blocks));
        }

        public long revision() {
            return revision;
        }

        public Map<ResourceLocation, ResearchTopicDefinition> topics() {
            return topics;
        }

        public Map<ResourceLocation, PrototypeProfileDefinition> profiles() {
            return profiles;
        }

        public Map<ResourceLocation, ResultEntry> results() {
            return results;
        }

        public ResultEntry result(ResourceLocation id) {
            return results.get(id);
        }

        public PrototypeProfileDefinition profile(ResourceLocation id) {
            return profiles.get(id);
        }

        public Set<ResourceLocation> managedRecipes() {
            return managedRecipes;
        }

        public Set<ResourceLocation> managedMultiblocks() {
            return managedMultiblocks;
        }

        public Set<ResourceLocation> managedBlocks() {
            return managedBlocks;
        }
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static <T> Set<T> immutableSet(Set<T> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    public static final class ValidationException extends RuntimeException {
        private final List<String> diagnostics;

        public ValidationException(List<String> diagnostics) {
            super("Research result catalogue is invalid:\n - " + String.join("\n - ", diagnostics));
            this.diagnostics = List.copyOf(diagnostics);
        }

        public List<String> diagnostics() {
            return diagnostics;
        }
    }
}
