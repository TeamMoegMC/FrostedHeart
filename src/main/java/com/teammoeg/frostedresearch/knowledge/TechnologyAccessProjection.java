/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compiled, team-specific access summary shared by all execution and display paths. */
public final class TechnologyAccessProjection {
    private static final Codec<Set<ResourceLocation>> TARGET_SET_CODEC = ResourceLocation.CODEC.listOf()
            .xmap(LinkedHashSet::new, ArrayList::new);
    public static final Codec<TechnologyAccessProjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TARGET_SET_CODEC.fieldOf("managed_recipes").forGetter(TechnologyAccessProjection::managedRecipes),
            TARGET_SET_CODEC.fieldOf("managed_multiblocks").forGetter(TechnologyAccessProjection::managedMultiblocks),
            TARGET_SET_CODEC.fieldOf("managed_blocks").forGetter(TechnologyAccessProjection::managedBlocks),
            TargetSources.CODEC.listOf().fieldOf("recipe_sources").forGetter(p -> entries(p.recipeSources)),
            TargetSources.CODEC.listOf().fieldOf("multiblock_sources").forGetter(p -> entries(p.multiblockSources)),
            TargetSources.CODEC.listOf().fieldOf("block_sources").forGetter(p -> entries(p.blockSources))
    ).apply(instance, TechnologyAccessProjection::new));
    public static final TechnologyAccessProjection EMPTY = new TechnologyAccessProjection(
            Set.of(), Set.of(), Set.of(), List.of(), List.of(), List.of());

    private final Set<ResourceLocation> managedRecipes;
    private final Set<ResourceLocation> managedMultiblocks;
    private final Set<ResourceLocation> managedBlocks;
    private final Map<ResourceLocation, List<AccessSource>> recipeSources;
    private final Map<ResourceLocation, List<AccessSource>> multiblockSources;
    private final Map<ResourceLocation, List<AccessSource>> blockSources;

    public TechnologyAccessProjection(Set<ResourceLocation> managedRecipes,
            Set<ResourceLocation> managedMultiblocks,
            Set<ResourceLocation> managedBlocks,
            List<TargetSources> recipeSources,
            List<TargetSources> multiblockSources,
            List<TargetSources> blockSources) {
        this.managedRecipes = Set.copyOf(managedRecipes);
        this.managedMultiblocks = Set.copyOf(managedMultiblocks);
        this.managedBlocks = Set.copyOf(managedBlocks);
        this.recipeSources = toMap(recipeSources);
        this.multiblockSources = toMap(multiblockSources);
        this.blockSources = toMap(blockSources);
    }

    public static TechnologyAccessProjection create(Set<ResourceLocation> managedRecipes,
            Set<ResourceLocation> managedMultiblocks,
            Set<ResourceLocation> managedBlocks,
            Map<ResourceLocation, List<AccessSource>> recipeSources,
            Map<ResourceLocation, List<AccessSource>> multiblockSources,
            Map<ResourceLocation, List<AccessSource>> blockSources) {
        return new TechnologyAccessProjection(managedRecipes, managedMultiblocks, managedBlocks,
                entries(recipeSources), entries(multiblockSources), entries(blockSources));
    }

    public AccessDecision recipe(ResourceLocation target) {
        return decision(managedRecipes, recipeSources, target);
    }

    public AccessDecision multiblock(ResourceLocation target) {
        return decision(managedMultiblocks, multiblockSources, target);
    }

    public AccessDecision block(ResourceLocation target) {
        return decision(managedBlocks, blockSources, target);
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

    public Map<ResourceLocation, List<AccessSource>> recipeSources() {
        return recipeSources;
    }

    public Map<ResourceLocation, List<AccessSource>> multiblockSources() {
        return multiblockSources;
    }

    public Map<ResourceLocation, List<AccessSource>> blockSources() {
        return blockSources;
    }

    private static AccessDecision decision(Set<ResourceLocation> managed,
            Map<ResourceLocation, List<AccessSource>> sources, ResourceLocation target) {
        if (!managed.contains(target)) return AccessDecision.unmanaged();
        List<AccessSource> valid = sources.getOrDefault(target, List.of());
        return new AccessDecision(true, !valid.isEmpty(), valid);
    }

    private static Map<ResourceLocation, List<AccessSource>> toMap(List<TargetSources> entries) {
        Map<ResourceLocation, List<AccessSource>> result = new LinkedHashMap<>();
        entries.forEach(entry -> result.put(entry.target(), List.copyOf(entry.sources())));
        return Collections.unmodifiableMap(result);
    }

    private static List<TargetSources> entries(Map<ResourceLocation, List<AccessSource>> values) {
        List<TargetSources> entries = new ArrayList<>();
        values.forEach((target, sources) -> entries.add(new TargetSources(target, sources)));
        return entries;
    }

    public record TargetSources(ResourceLocation target, List<AccessSource> sources) {
        static final Codec<TargetSources> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("target").forGetter(TargetSources::target),
                AccessSource.CODEC.listOf().fieldOf("sources").forGetter(TargetSources::sources)
        ).apply(instance, TargetSources::new));

        public TargetSources {
            sources = List.copyOf(sources);
        }
    }
}
