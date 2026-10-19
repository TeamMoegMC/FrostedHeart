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
package com.teammoeg.frostedresearch.knowledge.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.link.FieldCondition;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.link.SlotPattern;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.model.ObservationFields;

import java.util.*;

/**
 * 研究项目的一项需求；id 在项目内唯一，作为进度身份。证据复用联络规则的槽位与跨条件语言。
 * One project requirement; its id is the progress identity. Evidence reuses the link slot and cross-condition language.
 */
public record ProjectRequirement(String id, Type type, String title, int level, int count,
                                 List<LinkRule.Slot> inputs, List<LinkRule.CrossCondition> conditions,
                                 List<Distinct> distinct) {
    public enum Type {EVIDENCE, CALCULATION}

    public static final int MAX_EVIDENCE_SLOTS = 8;
    public static final int MAX_EVIDENCE_CONDITIONS = 10;
    public static final int MAX_CALCULATION_LEVEL = 3;
    public static final int MAX_CALCULATION_COUNT = 16;

    public static final Codec<ProjectRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ProjectRequirement::id),
            KnowledgeKey.enumCodec(Type.class).fieldOf("type").forGetter(ProjectRequirement::type),
            field(Codec.STRING, "title", "").forGetter(ProjectRequirement::title),
            field(Codec.INT, "level", 0).forGetter(ProjectRequirement::level),
            field(Codec.INT, "count", 1).forGetter(ProjectRequirement::count),
            field(LinkRule.Slot.CODEC.listOf(), "inputs", List.of()).forGetter(ProjectRequirement::inputs),
            field(LinkRule.CrossCondition.CODEC.listOf(), "conditions", List.of()).forGetter(ProjectRequirement::conditions),
            field(Distinct.CODEC.listOf(), "distinct", List.of()).forGetter(ProjectRequirement::distinct)
    ).apply(i, ProjectRequirement::new));

    /**
     * 列出的槽位在该字段上两两不同。 / The listed slots differ pairwise in this field.
     */
    public record Distinct(String field, List<String> slots) {
        public static final Codec<Distinct> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("field").forGetter(Distinct::field),
                Codec.STRING.listOf().fieldOf("slots").forGetter(Distinct::slots)
        ).apply(i, Distinct::new));

        public Distinct {
            slots = List.copyOf(slots);
        }
    }

    public ProjectRequirement {
        inputs = List.copyOf(inputs);
        conditions = List.copyOf(conditions);
        distinct = List.copyOf(distinct);
    }

    public static ProjectRequirement calculation(String id, String title, int level, int count) {
        return new ProjectRequirement(id, Type.CALCULATION, title, level, count, List.of(), List.of(), List.of());
    }

    public static ProjectRequirement evidence(String id, String title, List<LinkRule.Slot> inputs,
                                              List<LinkRule.CrossCondition> conditions, List<Distinct> distinct) {
        return new ProjectRequirement(id, Type.EVIDENCE, title, 0, 1, inputs, conditions, distinct);
    }

    /**
     * An optional field whose present value must decode, so an authoring mistake is reported instead of defaulted.
     */
    static <A> MapCodec<A> field(Codec<A> codec, String name, A fallback) {
        return KnowledgeKey.strictOptionalField(codec, name).xmap(value -> value.orElse(fallback), Optional::of);
    }

    /**
     * Evidence slots and conditions, with each distinct group expanded into pairwise inequality.
     */
    public SlotPattern pattern() {
        List<LinkRule.CrossCondition> all = new ArrayList<>(conditions);
        for (Distinct group : distinct)
            for (int a = 0; a < group.slots().size(); a++)
                for (int b = a + 1; b < group.slots().size(); b++)
                    all.add(new LinkRule.CrossCondition(LinkRule.CrossCondition.Kind.COMPARE, group.slots().get(a), group.field(),
                            group.slots().get(b), "", FieldCondition.Operator.NE, Optional.empty()));
        return new SlotPattern(inputs, all);
    }

    public void validate(List<String> errors) {
        if (id.isBlank()) errors.add("Requirement id must not be blank");
        switch (type) {
            case CALCULATION -> {
                if (level < 0 || level > MAX_CALCULATION_LEVEL)
                    errors.add(id + ": calculation level must be 0 to " + MAX_CALCULATION_LEVEL);
                if (count < 1 || count > MAX_CALCULATION_COUNT)
                    errors.add(id + ": calculation count must be 1 to " + MAX_CALCULATION_COUNT);
                if (!inputs.isEmpty() || !conditions.isEmpty() || !distinct.isEmpty())
                    errors.add(id + ": calculation requirements read no observations");
            }
            case EVIDENCE -> {
                if (title.isBlank()) errors.add(id + ": evidence needs an author-written title");
                if (level != 0 || count != 1)
                    errors.add(id + ": evidence takes one slot per record; repeat a slot instead of level/count");
                if (inputs.isEmpty() || inputs.size() > MAX_EVIDENCE_SLOTS)
                    errors.add(id + ": evidence requires 1 to " + MAX_EVIDENCE_SLOTS + " slots");
                if (conditions.size() > MAX_EVIDENCE_CONDITIONS)
                    errors.add(id + ": evidence allows at most " + MAX_EVIDENCE_CONDITIONS + " cross conditions");
                List<String> local = new ArrayList<>();
                Map<String, LinkRule.Slot> slots = LinkRule.validateBindings(inputs, conditions, local);
                for (LinkRule.Slot slot : inputs)
                    if (slot.kind() != KnowledgeKey.Kind.OBSERVATION) local.add("Evidence slots must be observations: " + slot.name());
                for (Distinct group : distinct) validate(group, slots, local);
                local.forEach(error -> errors.add(id + ": " + error));
            }
        }
    }

    private static void validate(Distinct group, Map<String, LinkRule.Slot> slots, List<String> errors) {
        if (ObservationFields.type(group.field()).isEmpty()) errors.add("Distinct reads an unknown field " + group.field());
        if (group.slots().size() < 2 || new HashSet<>(group.slots()).size() != group.slots().size())
            errors.add("Distinct needs at least two different slots");
        for (String name : group.slots()) {
            LinkRule.Slot slot = slots.get(name);
            if (slot == null) errors.add("Distinct references an unknown slot " + name);
            else if (slot.observationType().filter(t -> !ObservationFields.applies(group.field(), t)).isPresent())
                errors.add("Distinct reads a field that does not apply to slot " + name);
        }
    }
}
