/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.link;

import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * 完整无序匹配；先绑定候选最少的槽位，每一步及时判定可求值的跨输入条件。
 */
public final class LinkMatcher {
    private LinkMatcher() {
    }

    public record Match(ResourceLocation ruleId, LinkRule rule, Map<String, KnowledgeKey> bindings) {
        public Match {
            bindings = Map.copyOf(bindings);
        }
    }

    public static List<Match> match(List<KnowledgeElement> selected, Map<ResourceLocation, LinkRule> rules) {
        return match(selected, rules, TagLookup.SERVER);
    }

    public static List<Match> match(List<KnowledgeElement> selected, Map<ResourceLocation, LinkRule> rules, TagLookup tags) {
        if (selected.size() < 2 || selected.size() > 5 || selected.stream().map(KnowledgeElement::key).distinct().count() != selected.size())
            return List.of();
        return search(selected, rules, tags, true, Integer.MAX_VALUE);
    }

    /**
     * 为居民思考寻找每条规则的一个实际组合，直接剪枝槽位候选而非遍历全档案组合。
     */
    public static List<Match> candidates(List<KnowledgeElement> archive, Map<ResourceLocation, LinkRule> rules, int limit) {
        return candidates(archive, rules, limit, TagLookup.SERVER);
    }

    public static List<Match> candidates(List<KnowledgeElement> archive, Map<ResourceLocation, LinkRule> rules, int limit, TagLookup tags) {
        if (limit <= 0) return List.of();
        Map<KnowledgeKey, KnowledgeElement> unique = new LinkedHashMap<>();
        archive.forEach(element -> unique.putIfAbsent(element.key(), element));
        return search(List.copyOf(unique.values()), rules, tags, false, limit);
    }

    private static List<Match> search(List<KnowledgeElement> selected, Map<ResourceLocation, LinkRule> rules, TagLookup tags, boolean exact, int limit) {
        List<Match> matches = new ArrayList<>();
        for (var e : rules.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            if (matches.size() >= limit) break;
            LinkRule rule = e.getValue();
            if (exact && rule.inputs().size() != selected.size() || rule.inputs().size() > selected.size()) continue;
            Map<String, List<KnowledgeElement>> candidates = new LinkedHashMap<>();
            for (LinkRule.Slot slot : rule.inputs()) {
                List<KnowledgeElement> eligible = selected.stream().filter(k -> slot.matches(k, tags))
                        .sorted(Comparator.comparing(k -> k.key().id())).toList();
                candidates.put(slot.name(), eligible);
            }
            if (candidates.values().stream().anyMatch(List::isEmpty)) continue;
            List<String> order = candidates.keySet().stream().sorted(Comparator.comparingInt(name -> candidates.get(name).size())).toList();
            Map<String, KnowledgeElement> bindings = new LinkedHashMap<>();
            if (bind(0, order, candidates, bindings, new HashSet<>(), rule)) {
                Map<String, KnowledgeKey> keys = new LinkedHashMap<>();
                rule.inputs().forEach(s -> keys.put(s.name(), bindings.get(s.name()).key()));
                matches.add(new Match(e.getKey(), rule, keys));
            }
        }
        return List.copyOf(matches);
    }

    private static boolean bind(int index, List<String> order, Map<String, List<KnowledgeElement>> candidates,
                                Map<String, KnowledgeElement> bindings, Set<KnowledgeKey> used, LinkRule rule) {
        if (index == order.size()) return true;
        String slot = order.get(index);
        for (KnowledgeElement candidate : candidates.get(slot)) {
            if (!used.add(candidate.key())) continue;
            bindings.put(slot, candidate);
            if (rule.conditions().stream().allMatch(c -> c.matches(bindings)) && bind(index + 1, order, candidates, bindings, used, rule))
                return true;
            bindings.remove(slot);
            used.remove(candidate.key());
        }
        return false;
    }
}
