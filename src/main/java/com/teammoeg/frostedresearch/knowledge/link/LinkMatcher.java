/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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

    /**
     * 所选元素恰好各占一个槽位时的绑定。 / The binding that places every selected element in exactly one slot.
     */
    public static Optional<Map<String, KnowledgeElement>> bindExact(List<KnowledgeElement> selected, SlotPattern pattern, TagLookup tags) {
        if (selected.size() != pattern.inputs().size() || selected.stream().map(KnowledgeElement::key).distinct().count() != selected.size())
            return Optional.empty();
        return bind(selected, pattern, tags, Integer.MAX_VALUE);
    }

    /**
     * 从候选池中找出任一组满足模式的互异元素，回溯至多尝试 steps 次；超出预算视为没有找到。
     * Finds any binding of distinct pool elements, trying at most {@code steps} candidates; an exhausted budget finds none.
     */
    public static Optional<Map<String, KnowledgeElement>> find(List<KnowledgeElement> pool, SlotPattern pattern, TagLookup tags, int steps) {
        Map<KnowledgeKey, KnowledgeElement> unique = new LinkedHashMap<>();
        pool.forEach(element -> unique.putIfAbsent(element.key(), element));
        return bind(List.copyOf(unique.values()), pattern, tags, steps);
    }

    private static List<Match> search(List<KnowledgeElement> selected, Map<ResourceLocation, LinkRule> rules, TagLookup tags, boolean exact, int limit) {
        List<Match> matches = new ArrayList<>();
        for (var e : rules.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).toList()) {
            if (matches.size() >= limit) break;
            LinkRule rule = e.getValue();
            if (exact && rule.inputs().size() != selected.size() || rule.inputs().size() > selected.size()) continue;
            bind(selected, rule.pattern(), tags, Integer.MAX_VALUE).ifPresent(bindings -> {
                Map<String, KnowledgeKey> keys = new LinkedHashMap<>();
                rule.inputs().forEach(s -> keys.put(s.name(), bindings.get(s.name()).key()));
                matches.add(new Match(e.getKey(), rule, keys));
            });
        }
        return List.copyOf(matches);
    }

    private static Optional<Map<String, KnowledgeElement>> bind(List<KnowledgeElement> pool, SlotPattern pattern, TagLookup tags, int steps) {
        Map<String, List<KnowledgeElement>> candidates = new LinkedHashMap<>();
        for (LinkRule.Slot slot : pattern.inputs()) {
            List<KnowledgeElement> eligible = pool.stream().filter(k -> slot.matches(k, tags))
                    .sorted(Comparator.comparing(k -> k.key().id())).toList();
            if (eligible.isEmpty()) return Optional.empty();
            candidates.put(slot.name(), eligible);
        }
        List<String> order = candidates.keySet().stream().sorted(Comparator.comparingInt(name -> candidates.get(name).size())).toList();
        Map<String, KnowledgeElement> bindings = new LinkedHashMap<>();
        return bind(0, order, candidates, bindings, new HashSet<>(), pattern.conditions(), new int[]{steps})
                ? Optional.of(Map.copyOf(bindings)) : Optional.empty();
    }

    private static boolean bind(int index, List<String> order, Map<String, List<KnowledgeElement>> candidates,
                                Map<String, KnowledgeElement> bindings, Set<KnowledgeKey> used,
                                List<LinkRule.CrossCondition> conditions, int[] budget) {
        if (index == order.size()) return true;
        String slot = order.get(index);
        for (KnowledgeElement candidate : candidates.get(slot)) {
            if (--budget[0] < 0) return false;
            if (!used.add(candidate.key())) continue;
            bindings.put(slot, candidate);
            if (conditions.stream().allMatch(c -> c.matches(bindings)) && bind(index + 1, order, candidates, bindings, used, conditions, budget))
                return true;
            bindings.remove(slot);
            used.remove(candidate.key());
        }
        return false;
    }
}
