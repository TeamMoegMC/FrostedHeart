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
package com.teammoeg.frostedresearch.knowledge.labor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 研究所日报：一天的研究人力、分配给各项需求的人力、剩余人力与联想结果。只保留最近一天。
 * <p>
 * The research institutes' daily report: one day's labor by education level, what each requirement received, the
 * labor left over and the outcome of that day's association. Only the latest day is kept.
 *
 * @param day         城镇结算日 / town settlement day
 * @param labor       分配前各教育等级的人力 / labor by education level before allocation
 * @param allocations 各项需求的投入 / what each requirement asked for and received
 * @param idle        分配后剩余的人力 / labor left over after allocation
 * @param association 当天的联想 / that day's association
 */
public record LaborReport(long day, List<Double> labor, List<Allocation> allocations, List<Double> idle,
                          Association association) {
    public static final Codec<LaborReport> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("day").forGetter(LaborReport::day),
            Codec.DOUBLE.listOf().optionalFieldOf("labor", List.of()).forGetter(LaborReport::labor),
            Allocation.CODEC.listOf().optionalFieldOf("allocations", List.of()).forGetter(LaborReport::allocations),
            Codec.DOUBLE.listOf().optionalFieldOf("idle", List.of()).forGetter(LaborReport::idle),
            Association.CODEC.optionalFieldOf("association", Association.NONE).forGetter(LaborReport::association)
    ).apply(i, LaborReport::new));

    public LaborReport {
        labor = List.copyOf(labor);
        allocations = List.copyOf(allocations);
        idle = List.copyOf(idle);
        association = association == null ? Association.NONE : association;
    }

    public double totalLabor() {
        return labor.stream().mapToDouble(Double::doubleValue).sum();
    }

    public double totalIdle() {
        return idle.stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * 一项需求当天想要与实际得到的人·天。{@code itemsShort} 表示仓库物品限制了投入。
     * What one requirement asked for and received that day, in worker-days; {@code itemsShort} means the warehouse held
     * too few of its daily items.
     */
    public record Allocation(UUID run, String requirement, double wanted, double given, boolean itemsShort) {
        public static final Codec<Allocation> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("run").forGetter(Allocation::run),
                Codec.STRING.fieldOf("requirement").forGetter(Allocation::requirement),
                Codec.DOUBLE.optionalFieldOf("wanted", 0.0).forGetter(Allocation::wanted),
                Codec.DOUBLE.optionalFieldOf("given", 0.0).forGetter(Allocation::given),
                Codec.BOOL.optionalFieldOf("items_short", false).forGetter(Allocation::itemsShort)
        ).apply(i, Allocation::new));
    }

    public enum Outcome {
        /** No labor was qualified to associate. */
        NONE,
        /** Researchers thought it over without result. */
        QUIET,
        /** A new idea came from a link; {@code subject} is the idea. */
        IDEA,
        /** A hint about a topic; {@code subject} is the topic. */
        HINT,
        /** The researchers were ready, but nothing in the archive led anywhere new. */
        NOTHING
    }

    /**
     * 联想：可联想的人力、当天的成功概率与结果。 / Association: the labor that took part, that day's chance and the outcome.
     */
    public record Association(Outcome outcome, double labor, double chance, Optional<KnowledgeKey> subject) {
        public static final Association NONE = new Association(Outcome.NONE, 0, 0, Optional.empty());
        public static final Codec<Association> CODEC = RecordCodecBuilder.create(i -> i.group(
                KnowledgeKey.enumCodec(Outcome.class).optionalFieldOf("outcome", Outcome.NONE).forGetter(Association::outcome),
                Codec.DOUBLE.optionalFieldOf("labor", 0.0).forGetter(Association::labor),
                Codec.DOUBLE.optionalFieldOf("chance", 0.0).forGetter(Association::chance),
                KnowledgeKey.CODEC.optionalFieldOf("subject").forGetter(Association::subject)
        ).apply(i, Association::new));

        public Association {
            outcome = outcome == null ? Outcome.NONE : outcome;
            subject = subject == null ? Optional.empty() : subject;
        }

        public Association with(Outcome outcome, Optional<KnowledgeKey> subject) {
            return new Association(outcome, labor, chance, subject);
        }
    }
}
