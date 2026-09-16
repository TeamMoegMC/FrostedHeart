/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.RegistryObject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Content declares two independent directions; simulation never infers a material's phase family. */
public record StateTransitionData(
        BlockState block,
        boolean allStates,
        double capacityJPerK,
        double offsetJ,
        double conductanceWPerK,
        Edge heating,
        Edge cooling) {
    public static final Codec<StateTransitionData> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            BlockState.CODEC
                                                    .fieldOf("block")
                                                    .forGetter(StateTransitionData::block),
                                            Codec.BOOL
                                                    .optionalFieldOf("all_states", false)
                                                    .forGetter(StateTransitionData::allStates),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("capacity_j_per_k", Double.NaN)
                                                    .forGetter(StateTransitionData::capacityJPerK),
                                            Codec.DOUBLE
                                                    .optionalFieldOf("enthalpy_offset_j", 0.0)
                                                    .forGetter(StateTransitionData::offsetJ),
                                            Codec.DOUBLE
                                                    .optionalFieldOf(
                                                            "conductance_w_per_k", Double.NaN)
                                                    .forGetter(
                                                            StateTransitionData::conductanceWPerK),
                                            Edge.CODEC
                                                    .optionalFieldOf("heating")
                                                    .forGetter(
                                                            data ->
                                                                    Optional.ofNullable(
                                                                            data.heating)),
                                            Edge.CODEC
                                                    .optionalFieldOf("cooling")
                                                    .forGetter(
                                                            data ->
                                                                    Optional.ofNullable(
                                                                            data.cooling)))
                                    .apply(
                                            instance,
                                            (block,
                                                    all,
                                                    capacity,
                                                    offset,
                                                    conductance,
                                                    hot,
                                                    cold) ->
                                                    new StateTransitionData(
                                                            block,
                                                            all,
                                                            capacity,
                                                            offset,
                                                            conductance,
                                                            hot.orElse(null),
                                                            cold.orElse(null))));

    public enum Effect implements StringRepresentable {
        NONE,
        MELTING,
        SUBLIMATION,
        FREEZING,
        EVAPORATION,
        CONDENSATION,
        DEPOSITION;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Edge(
            BlockState target,
            double temperatureC,
            double latentHeatJ,
            WorldConditions worldConditions,
            Effect effect) {
        public static final Codec<Edge> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                BlockState.CODEC
                                                        .fieldOf("target")
                                                        .forGetter(Edge::target),
                                                Codec.DOUBLE
                                                        .fieldOf("temperature_c")
                                                        .forGetter(Edge::temperatureC),
                                                Codec.DOUBLE
                                                        .optionalFieldOf("latent_heat_j", 38_000.0)
                                                        .forGetter(Edge::latentHeatJ),
                                                WorldConditions.CODEC
                                                        .optionalFieldOf(
                                                                "world_conditions",
                                                                WorldConditions.NONE)
                                                        .forGetter(Edge::worldConditions),
                                                StringRepresentable.fromEnum(Effect::values)
                                                        .optionalFieldOf("effect", Effect.NONE)
                                                        .forGetter(Edge::effect))
                                        .apply(instance, Edge::new));
    }

    /** Fixed submission constraints, shared by profiles; never stored on energy nodes. */
    public record WorldConditions(
            int minYExclusive, TagKey<Fluid> fluidBoundary, TagKey<Biome> excludedBiome) {
        public static final WorldConditions NONE =
                new WorldConditions(Integer.MIN_VALUE, null, null);
        public static final Codec<WorldConditions> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                Codec.INT
                                                        .optionalFieldOf(
                                                                "min_y_exclusive",
                                                                Integer.MIN_VALUE)
                                                        .forGetter(WorldConditions::minYExclusive),
                                                TagKey.codec(Registries.FLUID)
                                                        .optionalFieldOf("fluid_boundary_tag")
                                                        .forGetter(
                                                                rules ->
                                                                        Optional.ofNullable(
                                                                                rules.fluidBoundary)),
                                                TagKey.codec(Registries.BIOME)
                                                        .optionalFieldOf("excluded_biome_tag")
                                                        .forGetter(
                                                                rules ->
                                                                        Optional.ofNullable(
                                                                                rules.excludedBiome)))
                                        .apply(
                                                instance,
                                                (minY, fluid, biome) ->
                                                        new WorldConditions(
                                                                minY,
                                                                fluid.orElse(null),
                                                                biome.orElse(null))));
    }

    public static RegistryObject<CodecRecipeSerializer<StateTransitionData>> TYPE;
    private static Map<BlockState, StateTransitionData> cache = Map.of();

    public boolean hasTransitions() {
        return heating != null || cooling != null;
    }

    /** Water uses the chunk surface sample; a phase law must not make entire oceans randomly tick. */
    public boolean hasRandomTransitions() {
        return hasTransitions() && !block.is(Blocks.WATER);
    }

    public static StateTransitionData getData(BlockState state) {
        return cache.get(state);
    }

    private Stream<BlockState> states() {
        return allStates
                ? block.getBlock().getStateDefinition().getPossibleStates().stream()
                : Stream.of(block);
    }

    public static void updateCache(RecipeManager manager) {
        var definitions =
                TYPE.get()
                        .filterRecipes(manager.getRecipes())
                        .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                        .map(recipe -> recipe.getData())
                        .toList();
        Map<BlockState, StateTransitionData> next = new HashMap<>();
        // Exact state definitions override broad declarations, independently of recipe iteration
        // order.
        for (boolean broad : new boolean[] {true, false}) {
            for (var data : definitions) {
                if (data.allStates == broad) data.states().forEach(state -> next.put(state, data));
            }
        }
        Set<BlockState> changed = new HashSet<>();
        cache.forEach(
                (state, old) -> {
                    var value = next.get(state);
                    if (old.hasRandomTransitions()
                            != (value != null && value.hasRandomTransitions())) changed.add(state);
                });
        next.forEach(
                (state, value) -> {
                    var old = cache.get(state);
                    if (value.hasRandomTransitions() != (old != null && old.hasRandomTransitions()))
                        changed.add(state);
                });
        cache = Map.copyOf(next);
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || changed.isEmpty()) return;
        Runnable recount =
                () -> {
                    for (var level : server.getAllLevels()) {
                        for (var holder : level.getChunkSource().chunkMap.getChunks()) {
                            var pos = holder.getPos();
                            var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                            if (chunk == null) continue;
                            for (var section : chunk.getSections()) {
                                if (section.getStates().maybeHas(changed::contains))
                                    section.recalcBlockCounts();
                            }
                        }
                    }
                };
        if (server.isSameThread()) recount.run();
        else server.execute(recount);
    }

    public FinishedRecipe toFinished(ResourceLocation name) {
        return TYPE.get().toFinished(name, this);
    }
}
