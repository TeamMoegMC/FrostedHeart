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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * 研究项目的一项需求；id 在项目内唯一，作为进度身份。证据复用联络规则的槽位与跨条件语言；实验在实验台上进行。
 * 计算与实验可以写人力协作 {@code labor}，由城镇研究所的研究员完成。
 * One project requirement; its id is the progress identity. Evidence reuses the link slot and cross-condition language;
 * experiments run on an experiment table. Calculations and experiments may ask for {@code labor} from town researchers.
 */
public record ProjectRequirement(String id, Type type, String title, int level, int count,
                                 List<LinkRule.Slot> inputs, List<LinkRule.CrossCondition> conditions,
                                 List<Distinct> distinct, int duration, List<Material> materials, Space space,
                                 Optional<Labor> labor) {
    public enum Type {EVIDENCE, CALCULATION, EXPERIMENT}

    public static final int MAX_EVIDENCE_SLOTS = 8;
    public static final int MAX_EVIDENCE_CONDITIONS = 10;
    public static final int MAX_CALCULATION_LEVEL = 3;
    public static final int MAX_CALCULATION_COUNT = 16;
    /** Experiment table material slots. */
    public static final int MAX_MATERIALS = 3;
    public static final int MIN_DURATION = 20;
    public static final int MAX_DURATION = 72_000;
    /** Largest room the experiment table scans, in air blocks. */
    public static final int MAX_VOLUME = 4096;
    public static final int MAX_SPACE_BLOCKS = 8;
    public static final int MAX_LABOR_WORKERS = 32;
    public static final int MAX_LABOR_DAYS = 100;
    /** Highest resident education level. */
    public static final int MAX_EDUCATION = 5;
    public static final int MAX_LABOR_ITEMS = 4;

    public static final Codec<ProjectRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ProjectRequirement::id),
            KnowledgeKey.enumCodec(Type.class).fieldOf("type").forGetter(ProjectRequirement::type),
            field(Codec.STRING, "title", "").forGetter(ProjectRequirement::title),
            field(Codec.INT, "level", 0).forGetter(ProjectRequirement::level),
            field(Codec.INT, "count", 1).forGetter(ProjectRequirement::count),
            field(LinkRule.Slot.CODEC.listOf(), "inputs", List.of()).forGetter(ProjectRequirement::inputs),
            field(LinkRule.CrossCondition.CODEC.listOf(), "conditions", List.of()).forGetter(ProjectRequirement::conditions),
            field(Distinct.CODEC.listOf(), "distinct", List.of()).forGetter(ProjectRequirement::distinct),
            field(Codec.INT, "duration", 0).forGetter(ProjectRequirement::duration),
            field(Material.CODEC.listOf(), "materials", List.of()).forGetter(ProjectRequirement::materials),
            field(Space.CODEC, "space", Space.NONE).forGetter(ProjectRequirement::space),
            KnowledgeKey.strictOptionalField(Labor.CODEC, "labor").forGetter(ProjectRequirement::labor)
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

    /**
     * 实验开始时一次投入的材料：物品或物品标签（两者都写时都要满足）。 / Material consumed when an experiment starts.
     */
    public record Material(Optional<ResourceLocation> item, Optional<ResourceLocation> tag, int count) {
        public static final Codec<Material> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("item").forGetter(Material::item),
                ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(Material::tag),
                field(Codec.INT, "count", 1).forGetter(Material::count)
        ).apply(i, Material::new));

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && item.map(id -> id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))).orElse(true)
                    && tag.map(id -> stack.is(TagKey.create(Registries.ITEM, id))).orElse(true);
        }
    }

    /**
     * 空间中需要的方块：方块或方块标签与数量。 / Blocks the experiment space must contain.
     */
    public record BlockCount(Optional<ResourceLocation> block, Optional<ResourceLocation> tag, int count) {
        public static final Codec<BlockCount> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.optionalFieldOf("block").forGetter(BlockCount::block),
                ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(BlockCount::tag),
                field(Codec.INT, "count", 1).forGetter(BlockCount::count)
        ).apply(i, BlockCount::new));

        public boolean matches(BlockState state) {
            return block.map(id -> id.equals(ForgeRegistries.BLOCKS.getKey(state.getBlock()))).orElse(true)
                    && tag.map(id -> state.is(TagKey.create(Registries.BLOCK, id))).orElse(true);
        }
    }

    /**
     * 实验空间。封闭时从实验台上方的空气开始扫描房间，体积以空气格计，方块计入与房间空气相邻的全部方块；
     * 温度取实验台所在位置。省略整个对象表示不要求空间。
     * The experiment space. An enclosed room is scanned from the air beside the table: its volume counts air blocks and
     * its blocks are every block touching that air. Temperature is read at the table. Omitting it requires no space.
     */
    public record Space(boolean enclosed, int minVolume, int maxVolume, Optional<Double> minTemperature,
                        Optional<Double> maxTemperature, List<BlockCount> blocks) {
        public static final Space NONE = new Space(false, 1, MAX_VOLUME, Optional.empty(), Optional.empty(), List.of());
        public static final Codec<Space> CODEC = RecordCodecBuilder.create(i -> i.group(
                field(Codec.BOOL, "enclosed", true).forGetter(Space::enclosed),
                field(Codec.INT, "min_volume", 1).forGetter(Space::minVolume),
                field(Codec.INT, "max_volume", MAX_VOLUME).forGetter(Space::maxVolume),
                KnowledgeKey.strictOptionalField(Codec.DOUBLE, "min_temperature").forGetter(Space::minTemperature),
                KnowledgeKey.strictOptionalField(Codec.DOUBLE, "max_temperature").forGetter(Space::maxTemperature),
                field(BlockCount.CODEC.listOf(), "blocks", List.of()).forGetter(Space::blocks)
        ).apply(i, Space::new));

        public Space {
            blocks = List.copyOf(blocks);
        }
    }

    /**
     * 人力协作：每天至多投入 {@code workers} 名标准研究员，教育等级不低于 {@code education}；一份协作需要
     * {@code workers × days} 人·天，物品按投入比例每天从城镇仓库扣除。{@code required} 为假时每份协作抵一次玩家的演算或实验，
     * 为真时玩家的次数与协作都要完成。
     * Labor from town researchers. Each day at most {@code workers} standard researchers of at least {@code education}
     * work on it; one share takes {@code workers × days} worker-days and the daily {@code items} are drawn from the town
     * warehouse in proportion to the labor given. When not {@code required} each share stands in for one of the
     * player's games or experiments; when required both the player's count and one share must be done.
     */
    public record Labor(boolean required, int workers, int days, int education, List<Material> items) {
        public static final Codec<Labor> CODEC = RecordCodecBuilder.create(i -> i.group(
                field(Codec.BOOL, "required", false).forGetter(Labor::required),
                field(Codec.INT, "workers", 1).forGetter(Labor::workers),
                field(Codec.INT, "days", 1).forGetter(Labor::days),
                field(Codec.INT, "education", 0).forGetter(Labor::education),
                field(Material.CODEC.listOf(), "items", List.of()).forGetter(Labor::items)
        ).apply(i, Labor::new));

        public Labor {
            items = List.copyOf(items);
        }

        /** Worker-days one share of this labor takes. */
        public double share() {
            return (double) workers * days;
        }
    }

    public ProjectRequirement {
        inputs = List.copyOf(inputs);
        conditions = List.copyOf(conditions);
        distinct = List.copyOf(distinct);
        materials = List.copyOf(materials);
        labor = labor == null ? Optional.empty() : labor;
    }

    public static ProjectRequirement calculation(String id, String title, int level, int count) {
        return new ProjectRequirement(id, Type.CALCULATION, title, level, count, List.of(), List.of(), List.of(), 0, List.of(), Space.NONE, Optional.empty());
    }

    public static ProjectRequirement evidence(String id, String title, List<LinkRule.Slot> inputs,
                                              List<LinkRule.CrossCondition> conditions, List<Distinct> distinct) {
        return new ProjectRequirement(id, Type.EVIDENCE, title, 0, 1, inputs, conditions, distinct, 0, List.of(), Space.NONE, Optional.empty());
    }

    public static ProjectRequirement experiment(String id, String title, int count, int duration, List<Material> materials, Space space) {
        return new ProjectRequirement(id, Type.EXPERIMENT, title, 0, count, List.of(), List.of(), List.of(), duration, materials, space, Optional.empty());
    }

    /** The same requirement asking for this labor. */
    public ProjectRequirement withLabor(Labor labor) {
        return new ProjectRequirement(id, type, title, level, count, inputs, conditions, distinct, duration, materials, space, Optional.of(labor));
    }

    /** Whether residents must also work on this requirement. */
    public boolean laborRequired() {
        return labor.map(Labor::required).orElse(false);
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
        if (type != Type.EXPERIMENT && (duration != 0 || !materials.isEmpty() || !space.equals(Space.NONE)))
            errors.add(id + ": only experiments take duration, materials or space");
        // Required labor may carry the whole requirement, so the player's own count may then be zero.
        int minimumCount = laborRequired() ? 0 : 1;
        labor.ifPresent(value -> validate(value, errors));
        switch (type) {
            case CALCULATION -> {
                if (level < 0 || level > MAX_CALCULATION_LEVEL)
                    errors.add(id + ": calculation level must be 0 to " + MAX_CALCULATION_LEVEL);
                if (count < minimumCount || count > MAX_CALCULATION_COUNT)
                    errors.add(id + ": calculation count must be " + minimumCount + " to " + MAX_CALCULATION_COUNT);
                if (!inputs.isEmpty() || !conditions.isEmpty() || !distinct.isEmpty())
                    errors.add(id + ": calculation requirements read no observations");
            }
            case EVIDENCE -> {
                if (title.isBlank()) errors.add(id + ": evidence needs an author-written title");
                if (labor.isPresent()) errors.add(id + ": only calculations and experiments take labor");
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
            case EXPERIMENT -> {
                if (title.isBlank()) errors.add(id + ": experiments need an author-written title");
                if (level != 0) errors.add(id + ": experiments take no level");
                if (count < minimumCount || count > MAX_CALCULATION_COUNT)
                    errors.add(id + ": experiment count must be " + minimumCount + " to " + MAX_CALCULATION_COUNT);
                if (!inputs.isEmpty() || !conditions.isEmpty() || !distinct.isEmpty())
                    errors.add(id + ": experiments read no observations");
                if (count == 0) {
                    // Labor alone carries it: nothing runs on an experiment table.
                    if (duration != 0 || !materials.isEmpty() || !space.equals(Space.NONE))
                        errors.add(id + ": an experiment done by labor alone takes no duration, materials or space");
                } else if (duration < MIN_DURATION || duration > MAX_DURATION)
                    errors.add(id + ": experiment duration must be " + MIN_DURATION + " to " + MAX_DURATION + " ticks");
                if (materials.size() > MAX_MATERIALS) errors.add(id + ": experiments take at most " + MAX_MATERIALS + " materials");
                for (Material material : materials) {
                    if (material.item().isEmpty() && material.tag().isEmpty()) errors.add(id + ": a material needs an item or a tag");
                    material.item().filter(item -> !ForgeRegistries.ITEMS.containsKey(item))
                            .ifPresent(item -> errors.add(id + ": unknown material item " + item));
                    if (material.count() < 1 || material.count() > 64) errors.add(id + ": material count must be 1 to 64");
                }
                validate(space, errors);
            }
        }
    }

    private void validate(Labor labor, List<String> errors) {
        if (labor.workers() < 1 || labor.workers() > MAX_LABOR_WORKERS)
            errors.add(id + ": labor workers must be 1 to " + MAX_LABOR_WORKERS);
        if (labor.days() < 1 || labor.days() > MAX_LABOR_DAYS)
            errors.add(id + ": labor days must be 1 to " + MAX_LABOR_DAYS);
        if (labor.education() < 0 || labor.education() > MAX_EDUCATION)
            errors.add(id + ": labor education must be 0 to " + MAX_EDUCATION);
        if (labor.items().size() > MAX_LABOR_ITEMS) errors.add(id + ": labor takes at most " + MAX_LABOR_ITEMS + " daily items");
        for (Material item : labor.items()) {
            if (item.item().isEmpty() && item.tag().isEmpty()) errors.add(id + ": a labor item needs an item or a tag");
            item.item().filter(name -> !ForgeRegistries.ITEMS.containsKey(name))
                    .ifPresent(name -> errors.add(id + ": unknown labor item " + name));
            if (item.count() < 1 || item.count() > 64) errors.add(id + ": labor item count must be 1 to 64");
        }
    }

    private void validate(Space space, List<String> errors) {
        if (space.minTemperature().isPresent() && space.maxTemperature().isPresent()
                && space.minTemperature().get() > space.maxTemperature().get())
            errors.add(id + ": minimum temperature exceeds maximum temperature");
        if (!space.enclosed()) {
            if (space.minVolume() != 1 || space.maxVolume() != MAX_VOLUME || !space.blocks().isEmpty())
                errors.add(id + ": volume and blocks need an enclosed space");
            return;
        }
        if (space.minVolume() < 1 || space.maxVolume() > MAX_VOLUME || space.minVolume() > space.maxVolume())
            errors.add(id + ": space volume must lie within 1 to " + MAX_VOLUME);
        if (space.blocks().size() > MAX_SPACE_BLOCKS) errors.add(id + ": a space lists at most " + MAX_SPACE_BLOCKS + " blocks");
        for (BlockCount block : space.blocks()) {
            if (block.block().isEmpty() && block.tag().isEmpty()) errors.add(id + ": a space block needs a block or a tag");
            block.block().filter(name -> !ForgeRegistries.BLOCKS.containsKey(name))
                    .ifPresent(name -> errors.add(id + ": unknown space block " + name));
            if (block.count() < 1 || block.count() > 64) errors.add(id + ": space block count must be 1 to 64");
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
