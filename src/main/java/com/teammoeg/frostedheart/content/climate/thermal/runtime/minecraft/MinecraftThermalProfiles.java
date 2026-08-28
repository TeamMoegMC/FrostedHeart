/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.StateStaticThermalResolver;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.ThermalSignatureResolverDispatcher;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-wide immutable gameplay resolver/material cut. */
final class MinecraftThermalProfiles {
    static final int TOPOLOGY_MUTATION = 1;
    private static final int SOURCE_MUTATION = 1 << 1;
    private static final double PHASE_FACE_CONDUCTANCE_W_PER_K = 20.0D;
    private static final double PHASE_BASE_ENERGY_J = 38_000.0D;
    private static volatile Snapshot snapshot;

    private MinecraftThermalProfiles() {
    }

    static synchronized Snapshot prepare() {
        if (snapshot != null) {
            return snapshot;
        }
        List<Block> blocks = new ArrayList<>(
                ForgeRegistries.BLOCKS.getValues());
        blocks.sort(Comparator.comparing(
                block -> String.valueOf(
                        ForgeRegistries.BLOCKS.getKey(block))));
        StateStaticThermalResolver geometryResolver =
                StateStaticThermalResolver.geometryOnly(64);
        Map<BlockState, Integer> phaseIdsByState = new IdentityHashMap<>();
        Map<BlockState, Integer> signatureIdsByState =
                new IdentityHashMap<>();
        Map<PhaseKey, Integer> phaseIds = new LinkedHashMap<>();
        Map<Long, Integer> patternIds = new LinkedHashMap<>();
        List<MaterialBoundaryRegistry.Profile> profiles =
                new ArrayList<>();
        for (GameplayMaterial material : GameplayMaterial.values()) {
            profiles.add(material.profile());
        }
        List<MaterialBoundaryRegistry.ContactPattern> patterns =
                new ArrayList<>();
        ThermalSignatureRegistry.Builder signatures =
                ThermalSignatureRegistry.builder();
        int staticStates = 0;
        int transitionStates = 0;

        for (Block block : blocks) {
            if (block.hasDynamicShape()) {
                continue;
            }
            for (BlockState state
                    : block.getStateDefinition().getPossibleStates()) {
                StateTransitionData data = StateTransitionData.getData(state);
                StateTransitionData.HeatingTransition transition =
                        data != null && data.willTransit()
                                && data.heatCapacity() > 0
                                ? data.heatingTransition(state) : null;
                ThermalResolution<ResolvedThermalSignature> resolved =
                        geometryResolver.resolve(
                                state, state.getFluidState());
                if (!resolved.isResolved()) {
                    continue;
                }
                ResolvedThermalSignature geometry =
                        resolved.value();
                long materialMask = materialMask(geometry);
                int profileId = 0;
                if (transition != null && materialMask != 0L) {
                    double energy = PHASE_BASE_ENERGY_J
                            * data.heatCapacity();
                    PhaseKey key = new PhaseKey(
                            transition.temperatureC(), energy);
                    Integer existing = phaseIds.get(key);
                    if (existing == null) {
                        existing = profiles.size() + 1;
                        phaseIds.put(key, existing);
                        profiles.add(
                                MaterialBoundaryRegistry.Profile
                                        .phaseReservoir(
                                                existing,
                                                PHASE_FACE_CONDUCTANCE_W_PER_K,
                                                transition.temperatureC(),
                                                energy,
                                                MaterialBoundaryRegistry
                                                        .TransitionMutationPolicy
                                                        .RESPECT_RANDOM_TICK_SPEED,
                                                MaterialBoundaryRegistry
                                                        .TransitionAction
                                                        .APPLY_STATE_TRANSITION_RECIPE));
                    }
                    profileId = existing;
                    phaseIdsByState.put(state, profileId);
                    transitionStates++;
                } else if (materialMask != 0L
                        && state.getFluidState().isEmpty()) {
                    GameplayMaterial material = classify(state);
                    if (material != null) {
                        profileId = material.profileId();
                        staticStates++;
                    }
                }
                int patternId = profileId == 0 ? 0 : patternId(
                        materialMask, patternIds, patterns);
                int signatureId = signatures.intern(withMaterial(
                        geometry, profileId, patternId));
                signatureIdsByState.put(state, signatureId);
            }
        }

        StateStaticThermalResolver.SignatureMetadata neutral =
                new StateStaticThermalResolver.SignatureMetadata(
                        0, 0, 0, 0, 0, 0, 0);
        Map<Long, Integer> frozenPatterns = Map.copyOf(patternIds);
        StateStaticThermalResolver resolver =
                StateStaticThermalResolver.withMaterialMask(
                        64,
                        (state, fluid, materialMask) -> {
                            Integer profileId = phaseIdsByState.get(state);
                            if (profileId == null && materialMask != 0L
                                    && fluid.isEmpty()) {
                                GameplayMaterial material = classify(state);
                                if (material != null) {
                                    profileId = material.profileId();
                                }
                            }
                            Integer patternId = profileId == null
                                    ? null : frozenPatterns.get(materialMask);
                            return patternId == null
                                    ? neutral
                                    : new StateStaticThermalResolver
                                            .SignatureMetadata(
                                                    0, profileId, patternId,
                                                    0, 0, 0, 0);
                        });
        snapshot = new Snapshot(
                signatures.build(),
                ThermalSignatureResolverDispatcher.builder(resolver).build(),
                new MaterialBoundaryRegistry(profiles, patterns),
                Collections.unmodifiableMap(
                        new IdentityHashMap<>(signatureIdsByState)),
                Collections.unmodifiableMap(
                        new IdentityHashMap<>(phaseIdsByState)));
        FHMain.LOGGER.info(
                "Compiled {} static material states, {} phase states, "
                        + "{} material profiles, and {} contact patterns",
                staticStates,
                transitionStates,
                profiles.size(),
                patterns.size());
        return snapshot;
    }

    static synchronized void invalidate() {
        snapshot = null;
    }

    private static boolean mutationSemanticsUnchanged(
            BlockState oldState,
            BlockState newState
    ) {
        Snapshot current = snapshot;
        if (current == null) {
            return false;
        }
        Integer oldSignature = current.signatureIdsByState.get(oldState);
        return oldSignature != null
                && oldSignature.equals(
                        current.signatureIdsByState.get(newState));
    }

    static int mutationFlags(
            BlockState oldState,
            BlockState newState
    ) {
        int flags = mutationSemanticsUnchanged(oldState, newState)
                ? 0 : TOPOLOGY_MUTATION;
        boolean source = oldState.is(Blocks.CAMPFIRE)
                || oldState.is(Blocks.SOUL_CAMPFIRE)
                || newState.is(Blocks.CAMPFIRE)
                || newState.is(Blocks.SOUL_CAMPFIRE);
        return source ? flags | SOURCE_MUTATION : flags;
    }

    static Integer phaseProfileId(BlockState state) {
        Snapshot current = snapshot;
        return current == null ? null : current.phaseProfileIds.get(state);
    }

    private static int patternId(
            long mask,
            Map<Long, Integer> ids,
            List<MaterialBoundaryRegistry.ContactPattern> patterns
    ) {
        Integer existing = ids.get(mask);
        if (existing != null) {
            return existing;
        }
        int id = patterns.size() + 1;
        ids.put(mask, id);
        patterns.add(new MaterialBoundaryRegistry.ContactPattern(id, mask));
        return id;
    }

    private static long materialMask(ResolvedThermalSignature signature) {
        long air = 0L;
        for (var region : signature.airRegions()) {
            air |= region.provenAirMicrocellMask();
        }
        return ~air;
    }

    private static ResolvedThermalSignature withMaterial(
            ResolvedThermalSignature geometry,
            int profileId,
            int patternId
    ) {
        return new ResolvedThermalSignature(
                geometry.mediumId(), profileId, geometry.airRegions(),
                patternId, geometry.radiationOcclusionPatternId(),
                geometry.sourceProfileId(), geometry.gateKind(),
                geometry.flags());
    }

    private static GameplayMaterial classify(BlockState state) {
        if (state.getBlock() instanceof LeavesBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.REPLACEABLE)) {
            return null;
        }
        if (state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)) {
            return GameplayMaterial.INSULATING_FABRIC;
        }
        if (state.is(Tags.Blocks.GLASS)
                || state.is(Tags.Blocks.GLASS_PANES)) {
            return GameplayMaterial.GLASS;
        }
        if (isMetal(state)) return GameplayMaterial.METAL;
        if (isWood(state)) return GameplayMaterial.WOOD;
        if (isEarth(state)) return GameplayMaterial.EARTH;
        if (isMasonry(state)) return GameplayMaterial.MASONRY;
        return state.blocksMotion()
                ? GameplayMaterial.GENERIC_SOLID : null;
    }

    private static boolean isMetal(BlockState state) {
        return FHTags.Blocks.METAL_MACHINES.matches(state)
                || state.is(BlockTags.ANVIL)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_GOLD)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_COPPER)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_NETHERITE)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER);
    }

    private static boolean isWood(BlockState state) {
        return FHTags.Blocks.WOODEN_MACHINES.matches(state)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    private static boolean isEarth(BlockState state) {
        return FHTags.Blocks.SOIL.matches(state)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Tags.Blocks.GRAVEL)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    private static boolean isMasonry(BlockState state) {
        return FHTags.Blocks.STONE.matches(state)
                || state.is(BlockTags.STONE_BRICKS)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Tags.Blocks.STONE)
                || state.is(Tags.Blocks.COBBLESTONE)
                || state.is(Tags.Blocks.END_STONES)
                || state.is(Tags.Blocks.NETHERRACK)
                || state.is(Tags.Blocks.OBSIDIAN)
                || state.is(Tags.Blocks.SANDSTONE)
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    record Snapshot(
            ThermalSignatureRegistry signatures,
            ThermalSignatureResolverDispatcher dispatcher,
            MaterialBoundaryRegistry materials,
            Map<BlockState, Integer> signatureIdsByState,
            Map<BlockState, Integer> phaseProfileIds
    ) {
    }

    private record PhaseKey(
            double transitionTemperatureC,
            double transitionEnergyJ
    ) {
    }

    private enum GameplayMaterial {
        INSULATING_FABRIC(0.12D, 120.0D),
        WOOD(0.45D, 450.0D),
        EARTH(1.0D, 1_100.0D),
        MASONRY(1.4D, 900.0D),
        GLASS(0.8D, 250.0D),
        METAL(6.0D, 700.0D),
        GENERIC_SOLID(1.0D, 650.0D);

        private final double conductance;
        private final double capacity;

        GameplayMaterial(double conductance, double capacity) {
            this.conductance = conductance;
            this.capacity = capacity;
        }

        int profileId() { return ordinal() + 1; }
        MaterialBoundaryRegistry.Profile profile() {
            return MaterialBoundaryRegistry.Profile
                    .capacitiveSurfaceAtNaturalTemperature(
                            profileId(), conductance, capacity);
        }
    }
}
