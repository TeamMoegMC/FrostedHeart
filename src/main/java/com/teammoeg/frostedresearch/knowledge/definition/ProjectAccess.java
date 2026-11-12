package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import java.util.*;

/** Fixed all/any lists; groups describe disclosure, never add start requirements. */
public record ProjectAccess(Map<String, KnowledgeCondition> conditions, List<String> startAll,
        List<String> discoverAny, List<Group> groups) {
    public static final Codec<ProjectAccess> CODEC = RecordCodecBuilder.create(i -> i.group(
        ProjectRequirement.field(Codec.unboundedMap(Codec.STRING, KnowledgeCondition.CODEC), "conditions", Map.of()).forGetter(ProjectAccess::conditions),
        ProjectRequirement.field(Codec.STRING.listOf(), "start_all", List.of()).forGetter(ProjectAccess::startAll),
        ProjectRequirement.field(Codec.STRING.listOf(), "discover_any", List.of()).forGetter(ProjectAccess::discoverAny),
        ProjectRequirement.field(Group.CODEC.listOf(), "groups", List.of()).forGetter(ProjectAccess::groups)
    ).apply(i, ProjectAccess::new));
    public record Group(String id, List<String> revealAny, List<String> conditions, String unknownHint) {
        public static final Codec<Group> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(Group::id),
            ProjectRequirement.field(Codec.STRING.listOf(), "reveal_any", List.of()).forGetter(Group::revealAny),
            ProjectRequirement.field(Codec.STRING.listOf(), "conditions", List.of()).forGetter(Group::conditions),
            ProjectRequirement.field(Codec.STRING, "unknown_hint", "").forGetter(Group::unknownHint)
        ).apply(i, Group::new));
        public Group { revealAny = List.copyOf(revealAny); conditions = List.copyOf(conditions); }
    }
    public ProjectAccess {
        // Normalize identical predicates once, keeping the lexically first author's wording.
        Map<String, KnowledgeCondition> normalized = new LinkedHashMap<>();
        Map<String, String> aliases = new HashMap<>();
        Map<String, KnowledgeCondition> original = conditions;
        original.keySet().stream().sorted().forEach(name -> {
            var condition = original.get(name);
            String canonical = normalized.entrySet().stream().filter(e -> e.getValue().samePredicate(condition))
                .map(Map.Entry::getKey).findFirst().orElse(name);
            if (!canonical.equals(name) && (!normalized.get(canonical).title().equals(condition.title()) || !normalized.get(canonical).hint().equals(condition.hint())))
                com.teammoeg.frostedresearch.FRMain.LOGGER.warn("access conditions {} and {} share a predicate; using wording from {}", canonical, name, canonical);
            aliases.put(name, canonical);
            normalized.putIfAbsent(canonical, condition);
        });
        startAll = refs(startAll, aliases);
        discoverAny = refs(discoverAny, aliases);
        List<Group> next = new ArrayList<>();
        for (Group g : groups) next.add(new Group(g.id(), refs(g.revealAny(), aliases), refs(g.conditions(), aliases), g.unknownHint()));
        List<String> discovery = new ArrayList<>(discoverAny);
        for (String name : startAll) if (normalized.containsKey(name) && normalized.get(name).kind() == KnowledgeKey.Kind.IDEA) discovery.add(name);
        Set<String> covered = new HashSet<>();
        next.forEach(g -> covered.addAll(g.conditions()));
        List<String> missing = startAll.stream().filter(n -> !covered.contains(n)).toList();
        if (!missing.isEmpty()) {
            Set<String> used = new HashSet<>(); next.forEach(g -> used.add(g.id()));
            String generated = "default";
            for (int suffix = 1; used.contains(generated); suffix++) generated = "default_" + suffix;
            com.teammoeg.frostedresearch.FRMain.LOGGER.warn("access.start_all conditions {} lack group coverage; adding {}", missing, generated);
            next.add(new Group(generated, discovery.stream().distinct().toList(), missing, ""));
        }
        conditions = Collections.unmodifiableMap(normalized);
        groups = List.copyOf(next);
    }
    private static List<String> refs(List<String> refs, Map<String, String> aliases) {
        return refs.stream().map(n -> aliases.getOrDefault(n, n)).distinct().toList();
    }
    public List<String> discovery() {
        Set<String> names = new LinkedHashSet<>(discoverAny);
        for (String name : startAll) if (conditions.containsKey(name) && conditions.get(name).kind() == KnowledgeKey.Kind.IDEA) names.add(name);
        return List.copyOf(names);
    }
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        conditions.forEach((n, c) -> { List<String> local = new ArrayList<>(); c.validate(local); local.forEach(e -> errors.add("conditions." + n + ": " + e)); });
        List<String> refs = new ArrayList<>(startAll); refs.addAll(discoverAny);
        Set<String> ids = new HashSet<>();
        for (Group g : groups) { if (!ids.add(g.id())) errors.add("duplicate group " + g.id()); refs.addAll(g.revealAny()); refs.addAll(g.conditions()); }
        refs.stream().filter(n -> !conditions.containsKey(n)).distinct().forEach(n -> errors.add("unknown access condition " + n));
        return errors;
    }
}
