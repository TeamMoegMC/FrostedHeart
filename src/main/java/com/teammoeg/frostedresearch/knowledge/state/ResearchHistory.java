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
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 研究的一次启动及其进度；需求是否满足按当前定义判断。 / One research run and its progress, judged by current definitions.
 * <p>
 * {@code calculations} keeps the level of each won calculation game and {@code evidence} the submitted copies of the
 * observations bound to each slot, both keyed by requirement id. An abandoned run keeps its record but no progress.
 */
public record ResearchHistory(UUID id, ResourceLocation project, KnowledgeKey idea, long time, boolean completed,
                              List<KnowledgeKey> results, boolean abandoned, Map<String, List<Integer>> calculations,
                              Map<String, Map<String, Observation>> evidence) {
    public static final Codec<ResearchHistory> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(ResearchHistory::id),
            ResourceLocation.CODEC.fieldOf("project").forGetter(ResearchHistory::project),
            KnowledgeKey.CODEC.fieldOf("idea").forGetter(ResearchHistory::idea),
            Codec.LONG.fieldOf("time").forGetter(ResearchHistory::time),
            Codec.BOOL.fieldOf("completed").forGetter(ResearchHistory::completed),
            KnowledgeKey.CODEC.listOf().fieldOf("results").forGetter(ResearchHistory::results),
            Codec.BOOL.optionalFieldOf("abandoned", false).forGetter(ResearchHistory::abandoned),
            progress(Codec.INT.listOf(), "calculations").forGetter(ResearchHistory::calculations),
            progress(Codec.unboundedMap(Codec.STRING, Observation.CODEC), "evidence").forGetter(ResearchHistory::evidence)
    ).apply(i, ResearchHistory::new));

    /**
     * Empty progress is not written. Present progress that fails to decode makes the whole run unreadable, so
     * KnowledgeState sets it aside unchanged instead of loading the run without its evidence.
     */
    private static <V> MapCodec<Map<String, V>> progress(Codec<V> value, String name) {
        return KnowledgeKey.strictOptionalField(Codec.unboundedMap(Codec.STRING, value), name)
                .xmap(stored -> stored.orElse(Map.of()), map -> map.isEmpty() ? Optional.empty() : Optional.of(map));
    }

    public ResearchHistory {
        results = List.copyOf(results);
        calculations = calculations.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        evidence = evidence.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
    }

    public ResearchHistory(UUID id, ResourceLocation project, KnowledgeKey idea, long time, boolean completed,
                           List<KnowledgeKey> results) {
        this(id, project, idea, time, completed, results, false, Map.of(), Map.of());
    }

    public boolean open() {
        return !completed && !abandoned;
    }

    public ResearchHistory withCalculation(String requirement, int level) {
        Map<String, List<Integer>> next = new HashMap<>(calculations);
        List<Integer> levels = new ArrayList<>(next.getOrDefault(requirement, List.of()));
        levels.add(level);
        next.put(requirement, levels);
        return new ResearchHistory(id, project, idea, time, completed, results, abandoned, next, evidence);
    }

    public ResearchHistory withEvidence(String requirement, Map<String, Observation> binding) {
        Map<String, Map<String, Observation>> next = new HashMap<>(evidence);
        next.put(requirement, binding);
        return new ResearchHistory(id, project, idea, time, completed, results, abandoned, calculations, next);
    }

    public ResearchHistory completedWith(List<KnowledgeKey> outputs) {
        return new ResearchHistory(id, project, idea, time, true, outputs, abandoned, calculations, evidence);
    }

    public ResearchHistory abandon() {
        return new ResearchHistory(id, project, idea, time, completed, results, true, Map.of(), Map.of());
    }
}
