/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.definition;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.knowledge.ResearchResultCatalog;
import com.teammoeg.frostedresearch.knowledge.link.*;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.*;

/**
 * 原子切换的数据包快照。失败重载保留上一版本，不改写队伍记录或发现历史。
 */
public final class KnowledgeDefinitions {
    public static final String ROOT = "frostedresearch/knowledge/";
    private static final Map<ResourceLocation, ObservationUse> USES = new LinkedHashMap<>();
    private static volatile Snapshot current = Snapshot.empty();
    private static long revision;

    private KnowledgeDefinitions() {
    }

    public static Snapshot current() {
        return current;
    }

    public record ReloadResult(boolean applied, List<String> diagnostics) {
        public ReloadResult {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public static ReloadResult reload(ResourceManager resources) {
        List<String> errors = new ArrayList<>();
        var ideas = read(resources, "ideas", IdeaDefinition.CODEC, errors);
        var results = read(resources, "results", ResultDefinition.CODEC, errors);
        var links = read(resources, "links", LinkRule.CODEC, errors);
        var items = read(resources, "item_retrieval", ItemRetrievalRule.CODEC, errors);
        var projects = read(resources, "projects", ProjectDefinition.CODEC, errors);
        var initial = read(resources, "initial", KnowledgeKey.CODEC.listOf(), errors).values().stream().flatMap(List::stream).distinct().toList();
        Snapshot candidate = new Snapshot(0, ideas, results, links, items, projects, initial);
        errors.addAll(validate(candidate));
        errors.sort(String::compareTo);
        if (!errors.isEmpty()) return new ReloadResult(false, errors);
        install(candidate);
        return new ReloadResult(true, List.of());
    }

    public static synchronized Snapshot install(Snapshot snapshot) {
        current = new Snapshot(++revision, snapshot.ideas, snapshot.results, snapshot.links, snapshot.itemRetrievalRules,
                snapshot.projects, snapshot.initialKnowledge, Map.copyOf(USES));
        return current;
    }

    public static synchronized void registerObservationUse(ResourceLocation id, ObservationUse use) {
        USES.put(id, use);
        install(current);
    }

    public static synchronized void unregisterObservationUse(ResourceLocation id) {
        if (USES.remove(id) != null) install(current);
    }

    public static synchronized void clearForTests() {
        USES.clear();
        revision = 0;
        current = Snapshot.empty();
    }

    public static List<String> validate(Snapshot s) {
        List<String> errors = new ArrayList<>();
        s.ideas.forEach((id, idea) -> {
            validateKeys(id, idea.understanding(), s, errors);
            for (var project : idea.projects())
                if (!s.projects.containsKey(project)) errors.add(id + ": missing project " + project);
        });
        s.results.forEach((id, result) -> {
            if (!id.equals(result.result().id())) errors.add(id + ": embedded result id must equal resource path");
            validateKeys(id, result.understanding(), s, errors);
        });
        s.projects.forEach((id, p) -> {
            if (p.results().isEmpty()) errors.add(id + ": project must produce at least one result");
            for (var result : p.results()) if (!resultExists(s, result)) errors.add(id + ": missing result " + result);
        });
        s.links.forEach((id, rule) -> {
            rule.validate().forEach(error -> errors.add(id + ": " + error));
            if (!s.ideas.containsKey(rule.output())) errors.add(id + ": missing output idea " + rule.output());
            for (var slot : rule.inputs())
                slot.definition().ifPresent(def -> {
                    if (slot.kind() == KnowledgeKey.Kind.IDEA && !s.ideas.containsKey(def)
                            || slot.kind() == KnowledgeKey.Kind.RESULT && !resultExists(s, def))
                        errors.add(id + ": missing input definition " + def);
                });
        });
        s.itemRetrievalRules.forEach((id, item) -> {
            if (item.item().isEmpty() && item.tag().isEmpty()) errors.add(id + ": item retrieval requires item or tag");
            if (item.nbtKeys().stream().anyMatch(String::isBlank)) errors.add(id + ": empty NBT key");
        });
        validateKeys(new ResourceLocation("frostedresearch", "initial"), s.initialKnowledge, s, errors);
        // An extant definition remains meaningful after its original producing path is removed.
        return List.copyOf(errors);
    }

    private static void validateKeys(ResourceLocation owner, List<KnowledgeKey> keys, Snapshot s, List<String> errors) {
        for (KnowledgeKey key : keys) {
            if (key.kind() == KnowledgeKey.Kind.OBSERVATION)
                errors.add(owner + ": static knowledge references cannot contain runtime observation UUIDs");
            else if (key.kind() == KnowledgeKey.Kind.IDEA ? !s.ideas.containsKey(key.definitionId()) : !resultExists(s, key.definitionId()))
                errors.add(owner + ": missing knowledge " + key);
        }
    }

    private static boolean resultExists(Snapshot s, ResourceLocation id) {
        return s.results.containsKey(id) || ResearchResultCatalog.current().results().containsKey(id);
    }

    private static <T> Map<ResourceLocation, T> read(ResourceManager resources, String kind, Codec<T> codec, List<String> errors) {
        String directory = ROOT + kind;
        Map<ResourceLocation, T> result = new LinkedHashMap<>();
        resources.listResources(directory, id -> id.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).forEach(entry -> {
                    ResourceLocation file = entry.getKey();
                    ResourceLocation id = new ResourceLocation(file.getNamespace(), file.getPath().substring(directory.length() + 1, file.getPath().length() - 5));
                    try (Reader reader = entry.getValue().openAsReader()) {
                        var decoded = codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader));
                        decoded.error().ifPresent(error -> errors.add(file + ": " + error.message()));
                        decoded.result().ifPresent(value -> result.put(id, value));
                    } catch (Exception exception) {
                        errors.add(file + ": " + exception.getMessage());
                    }
                });
        return result;
    }

    public static final class Snapshot {
        private final long revision;
        private final Map<ResourceLocation, IdeaDefinition> ideas;
        private final Map<ResourceLocation, ResultDefinition> results;
        private final Map<ResourceLocation, LinkRule> links;
        private final Map<ResourceLocation, ItemRetrievalRule> itemRetrievalRules;
        private final Map<ResourceLocation, ProjectDefinition> projects;
        private final List<KnowledgeKey> initialKnowledge;
        private final Map<ResourceLocation, ObservationUse> observationUses;
        private final List<LinkRule> observationRules;

        public Snapshot(long revision, Map<ResourceLocation, IdeaDefinition> ideas, Map<ResourceLocation, ResultDefinition> results,
                        Map<ResourceLocation, LinkRule> links, Map<ResourceLocation, ItemRetrievalRule> items,
                        Map<ResourceLocation, ProjectDefinition> projects, List<KnowledgeKey> initial) {
            this(revision, ideas, results, links, items, projects, initial, Map.of());
        }

        private Snapshot(long revision, Map<ResourceLocation, IdeaDefinition> ideas, Map<ResourceLocation, ResultDefinition> results,
                         Map<ResourceLocation, LinkRule> links, Map<ResourceLocation, ItemRetrievalRule> items,
                         Map<ResourceLocation, ProjectDefinition> projects, List<KnowledgeKey> initial, Map<ResourceLocation, ObservationUse> uses) {
            this.revision = revision;
            this.ideas = Map.copyOf(ideas);
            this.results = Map.copyOf(results);
            this.links = Map.copyOf(links);
            this.itemRetrievalRules = Map.copyOf(items);
            this.projects = Map.copyOf(projects);
            this.initialKnowledge = List.copyOf(initial);
            this.observationUses = Map.copyOf(uses);
            this.observationRules = links.values().stream().filter(r -> r.inputs().stream().anyMatch(s -> s.kind() == KnowledgeKey.Kind.OBSERVATION)).toList();
        }

        public static Snapshot empty() {
            return new Snapshot(0, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of());
        }

        public long revision() {
            return revision;
        }

        public Map<ResourceLocation, IdeaDefinition> ideas() {
            return ideas;
        }

        public Map<ResourceLocation, ResultDefinition> results() {
            return results;
        }

        public Map<ResourceLocation, LinkRule> links() {
            return links;
        }

        public Map<ResourceLocation, ItemRetrievalRule> itemRetrievalRules() {
            return itemRetrievalRules;
        }

        public Map<ResourceLocation, ProjectDefinition> projects() {
            return projects;
        }

        public List<KnowledgeKey> initialKnowledge() {
            return initialKnowledge;
        }

        public Map<ResourceLocation, ObservationUse> observationUses() {
            return observationUses;
        }

        public boolean hasObservationUse(Observation observation) {
            return hasObservationUse(observation, TagLookup.SERVER);
        }

        public boolean hasObservationUse(Observation observation, TagLookup tags) {
            KnowledgeElement element = KnowledgeElement.observation(observation);
            return observationRules.stream().flatMap(r -> r.inputs().stream()).anyMatch(s -> s.matches(element, tags))
                    || observationUses.values().stream().anyMatch(u -> u.matches(observation));
        }

        public boolean addsObservationDifference(Observation observation, List<Observation> archived) {
            return addsObservationDifference(observation, archived, TagLookup.SERVER);
        }

        public boolean addsObservationDifference(Observation observation, List<Observation> archived, TagLookup tags) {
            if (archived.stream().anyMatch(o -> o.recordId().equals(observation.recordId()))) return false;
            if (!hasObservationUse(observation, tags)) return false;
            KnowledgeElement candidate = KnowledgeElement.observation(observation);
            int count = 1;
            for (LinkRule rule : observationRules)
                count = Math.max(count, (int) rule.inputs().stream().filter(s -> s.matches(candidate, tags)).count());
            for (ObservationUse use : observationUses.values())
                if (use.matches(observation)) count = Math.max(count, use.independentRecords());
            int equivalentCount = 0;
            Set<UUID> seen = new HashSet<>();
            for (Observation old : archived)
                if (seen.add(old.recordId()) && equivalent(observation, old, tags) && ++equivalentCount >= count)
                    return false;
            return true;
        }

        private boolean equivalent(Observation first, Observation second, TagLookup tags) {
            if (first.type() != second.type()) return false;
            KnowledgeElement a = KnowledgeElement.observation(first), b = KnowledgeElement.observation(second);
            for (LinkRule rule : observationRules) {
                Set<String> matchingSlots = new HashSet<>();
                for (LinkRule.Slot slot : rule.inputs()) {
                    boolean am = slot.matches(a, tags), bm = slot.matches(b, tags);
                    if (am != bm) return false;
                    if (am) matchingSlots.add(slot.name());
                }
                for (LinkRule.CrossCondition cross : rule.conditions()) {
                    if (matchingSlots.contains(cross.left()) || matchingSlots.contains(cross.right()))
                        for (String field : cross.fieldsRead())
                            if (!first.value(field).equals(second.value(field))) return false;
                }
            }
            for (ObservationUse use : observationUses.values()) {
                boolean am = use.matches(first), bm = use.matches(second);
                if (am != bm || am && !use.equivalent(first, second).orElse(false)) return false;
            }
            return true;
        }
    }
}
