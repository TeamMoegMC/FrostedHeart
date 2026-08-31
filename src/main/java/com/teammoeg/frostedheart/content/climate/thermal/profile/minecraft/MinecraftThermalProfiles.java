/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.MinecraftRadiationOcclusion;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-wide immutable gameplay resolver/material cut. */
public final class MinecraftThermalProfiles {
    public static final int TOPOLOGY_MUTATION = 1;
    public static final int SOURCE_MUTATION = 1 << 1;
    public static final int RADIATION_MUTATION = 1 << 2;
    public static final int OCCLUSION_MUTATION = 1 << 3;
    private static final double STEFAN_BOLTZMANN_W_PER_M2_K4 =
            5.670_374_419e-8D;
    private static volatile Snapshot snapshot;

    private MinecraftThermalProfiles() {
    }

    public static synchronized Snapshot prepare() {
        if (snapshot != null) {
            return snapshot;
        }
        FHConfig.Common.ThermalRuntime config =
                FHConfig.COMMON.THERMAL_RUNTIME;
        double phaseFaceConductanceWPerK =
                config.phaseFaceConductanceWPerK.get();
        double phaseBaseEnergyJPerHeatCapacity =
                config.phaseBaseEnergyJPerHeatCapacity.get();
        boolean staticRadiationEnabled =
                config.enableStaticBlockRadiation.get();
        Tuning tuning = new Tuning(
                config.airHeatCapacityJPerBlockK.get(),
                config.airMixingWPerBlockK.get(),
                config.farFieldConductanceWPerK.get(),
                config.dormantTemperatureHalfLifeSeconds.get(),
                MinecraftPhysicalSourceProfile.campfire(
                        config.campfirePowerW.get(),
                        config.campfireRadiationShare.get()));
        List<Block> blocks = new ArrayList<>(
                ForgeRegistries.BLOCKS.getValues());
        blocks.sort(Comparator.comparing(
                block -> String.valueOf(
                        ForgeRegistries.BLOCKS.getKey(block))));
        StateStaticThermalResolver geometryResolver =
                StateStaticThermalResolver.geometryOnly(64);
        Map<PhaseKey, Integer> phaseIds = new LinkedHashMap<>();
        Map<Long, Integer> patternIds = new LinkedHashMap<>();
        List<MaterialBoundaryRegistry.Profile> profiles =
                new ArrayList<>();
        for (GameplayMaterial material : GameplayMaterial.values()) {
            profiles.add(material.profile());
        }
        List<MaterialBoundaryRegistry.ContactPattern> patterns =
                new ArrayList<>();
        ThermalSignatureTable.Builder signatures =
                ThermalSignatureTable.builder();
        MinecraftStateThermalTable.Builder states =
                MinecraftStateThermalTable.builder(
                        Block.BLOCK_STATE_REGISTRY.size(),
                        staticRadiationEnabled);
        int fireRadiationProfile = states.addRadiationProfile(
                MinecraftStateThermalTable.RADIATION_FIXED,
                config.fireRadiantPowerW.get());
        double lavaSurfacePowerWPerM2 = lavaSurfacePowerWPerM2(config);
        int staticStates = 0;
        int transitionStates = 0;

        for (Block block : blocks) {
            for (BlockState state
                    : block.getStateDefinition().getPossibleStates()) {
                StateTransitionData data = StateTransitionData.getData(state);
                StateTransitionData.HeatingTransition transition =
                        data != null && data.willTransit()
                                && data.heatCapacity() > 0
                                ? data.heatingTransition(state) : null;
                ResolvedThermalSignature geometry = block.hasDynamicShape()
                        ? null : geometryResolver.resolve(
                                state, state.getFluidState());
                long materialMask = geometry == null
                        ? 0L : materialMask(geometry);
                int profileId = 0;
                if (transition != null && materialMask != 0L) {
                    double energy = phaseBaseEnergyJPerHeatCapacity
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
                                                phaseFaceConductanceWPerK,
                                                transition.temperatureC(),
                                                energy));
                    }
                    profileId = existing;
                    transitionStates++;
                } else if (materialMask != 0L
                        && state.getFluidState().isEmpty()) {
                    GameplayMaterial material = classify(state);
                    if (material != null) {
                        profileId = material.profileId();
                        staticStates++;
                    }
                }
                int signatureId = ThermalSignatureTable.UNRESOLVED;
                if (geometry != null) {
                    int patternId = profileId == 0 ? 0 : patternId(
                            materialMask, patternIds, patterns);
                    signatureId = signatures.intern(withMaterial(
                            geometry, profileId, patternId));
                }
                int radiationProfileId = 0;
                if (staticRadiationEnabled && !isCampfire(state)) {
                    if (state.getFluidState().is(FluidTags.LAVA)) {
                        radiationProfileId = states.addRadiationProfile(
                                MinecraftStateThermalTable.RADIATION_LAVA_SURFACE,
                                lavaSurfacePowerWPerM2);
                    } else if (FHTags.Blocks.STATIC_FIRE_RADIATORS.matches(state)) {
                        radiationProfileId = fireRadiationProfile;
                    }
                }
                states.put(
                        state,
                        signatureId,
                        radiationProfileId,
                        campfireFlags(state),
                        MinecraftRadiationOcclusion.blocksRadiation(state));
            }
        }
        snapshot = new Snapshot(
                signatures.build(),
                states.build(),
                new MaterialBoundaryRegistry(profiles, patterns),
                tuning);
        FHMain.LOGGER.info(
                "Compiled {} static material states, {} phase states, "
                        + "{} material profiles, and {} contact patterns",
                staticStates,
                transitionStates,
                profiles.size(),
                patterns.size());
        return snapshot;
    }

    public static double dormantTemperatureHalfLifeSeconds() {
        Snapshot current = snapshot;
        return (current == null ? prepare() : current).tuning()
                .dormantTemperatureHalfLifeSeconds();
    }

    public static synchronized void invalidate() {
        snapshot = null;
    }

    public static int mutationFlags(
            BlockState oldState,
            BlockState newState
    ) {
        Snapshot current = snapshot;
        return current == null
                ? TOPOLOGY_MUTATION
                : current.states.mutationFlags(oldState, newState);
    }

    public static Integer phaseProfileId(BlockState state) {
        Snapshot current = snapshot;
        if (current == null) {
            return null;
        }
        int signatureId = current.states.signatureId(state);
        int profileId = current.signatures.materialProfileId(signatureId);
        MaterialBoundaryRegistry.Profile profile =
                current.materials.profileOrNull(profileId);
        return profile != null
                && profile.model() == MaterialBoundaryRegistry.Model.PHASE_RESERVOIR
                ? profileId : null;
    }

    public static boolean lavaBlockRadiationEnabled() {
        Snapshot current = snapshot;
        return current != null && current.states.hasRadiationMode(
                MinecraftStateThermalTable.RADIATION_LAVA_SURFACE);
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
        return ~signature.airGeometry().provenAirMicrocellMask();
    }

    private static ResolvedThermalSignature withMaterial(
            ResolvedThermalSignature geometry,
            int profileId,
            int patternId
    ) {
        return new ResolvedThermalSignature(
                geometry.airGeometry(), profileId, patternId);
    }

    private static boolean isCampfire(BlockState state) {
        return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private static byte campfireFlags(BlockState state) {
        if (!isCampfire(state)) {
            return 0;
        }
        int flags = MinecraftStateThermalTable.CAMPFIRE_PRESENT;
        if (state.hasProperty(CampfireBlock.LIT)
                && state.getValue(CampfireBlock.LIT)) {
            flags |= MinecraftStateThermalTable.CAMPFIRE_LIT;
        }
        return (byte) flags;
    }

    private static double lavaSurfacePowerWPerM2(
            FHConfig.Common.ThermalRuntime config
    ) {
        double lavaK = config.lavaRadiationTemperatureC.get() + 273.15D;
        double referenceK =
                config.radiationReferenceTemperatureC.get() + 273.15D;
        return Math.max(0.0D, config.effectiveLavaEmissivity.get()
                * STEFAN_BOLTZMANN_W_PER_M2_K4
                * (Math.pow(lavaK, 4.0D) - Math.pow(referenceK, 4.0D)));
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

    public record Snapshot(
            ThermalSignatureTable signatures,
            MinecraftStateThermalTable states,
            MaterialBoundaryRegistry materials,
            Tuning tuning
    ) {
    }

    public record Tuning(
            double airHeatCapacityJPerBlockK,
            double airMixingWPerBlockK,
            double farFieldConductanceWPerK,
            double dormantTemperatureHalfLifeSeconds,
            MinecraftPhysicalSourceProfile campfire
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
