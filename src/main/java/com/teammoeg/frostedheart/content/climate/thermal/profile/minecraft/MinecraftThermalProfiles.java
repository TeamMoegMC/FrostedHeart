/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.MinecraftRadiationOcclusion;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final double STEFAN_BOLTZMANN_W_PER_M2_K4 = 5.670_374_419e-8D;
    private static volatile Snapshot snapshot;
    private static volatile int profileEpoch;
    private static final double WATER_CAPACITY_J_PER_K = 6_600;
    // An effective reference amount, not a volume inferred from ventilation.
    private static final double WATERLOGGED_CAPACITY_J_PER_K = WATER_CAPACITY_J_PER_K / 4;

    private MinecraftThermalProfiles() {}

    public static synchronized Snapshot prepare() {
        if (snapshot != null) {
            return snapshot;
        }
        FHConfig.Common.ThermalRuntime config = FHConfig.COMMON.THERMAL_RUNTIME;
        boolean staticRadiationEnabled = config.enableStaticBlockRadiation.get();
        Tuning tuning =
                new Tuning(
                        config.airHeatCapacityJPerBlockK.get(),
                        config.airMixingWPerBlockK.get(),
                        config.farFieldConductanceWPerK.get(),
                        config.dormantTemperatureHalfLifeSeconds.get(),
                        MinecraftPhysicalSourceProfile.campfire(
                                config.campfirePowerW.get(), config.campfireRadiationShare.get()));
        List<Block> blocks = new ArrayList<>(ForgeRegistries.BLOCKS.getValues());
        blocks.sort(
                Comparator.comparing(
                        block -> String.valueOf(ForgeRegistries.BLOCKS.getKey(block))));
        Map<BodyKey, Integer> bodyIds = new LinkedHashMap<>();
        List<MaterialBoundaryRegistry.Profile> profiles = new ArrayList<>();
        List<StateTransitionData.Edge> heatingData = new ArrayList<>(),
                coolingData = new ArrayList<>();
        heatingData.add(null);
        coolingData.add(null);
        MinecraftMaterialLawCompiler lawCompiler =
                new MinecraftMaterialLawCompiler(blocks, MinecraftThermalProfiles::bodyCapacity);
        ThermalSignatureTable.Builder signatures = ThermalSignatureTable.builder();
        MinecraftStateThermalTable.Builder states =
                MinecraftStateThermalTable.builder(
                        Block.BLOCK_STATE_REGISTRY.size(), staticRadiationEnabled);
        int fireRadiationProfile =
                states.addRadiationProfile(
                        MinecraftStateThermalTable.RADIATION_FIXED, config.fireRadiantPowerW.get());
        double lavaSurfacePowerWPerM2 = lavaSurfacePowerWPerM2(config);
        int lavaRadiationProfile =
                states.addRadiationProfile(
                        MinecraftStateThermalTable.RADIATION_LAVA_SURFACE, lavaSurfacePowerWPerM2);
        int staticStates = 0;
        int transitionStates = 0;

        for (Block block : blocks) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                int ventilation = StateStaticThermalResolver.ventilation(state);
                int profileId = 0;
                MaterialThermalLaw law = lawCompiler.law(state);
                if (law != null) {
                    boolean phase = law.heating() != null || law.cooling() != null;
                    GameplayMaterial material = classify(state);
                    double conductance = material.conductance;
                    var data = StateTransitionData.getData(state);
                    if (data != null && Double.isFinite(data.conductanceWPerK()))
                        conductance = data.conductanceWPerK();
                    var heating = data == null ? null : data.heating();
                    var cooling = data == null ? null : data.cooling();
                    BodyKey key = new BodyKey(conductance, law, heating, cooling);
                    Integer existing = bodyIds.get(key);
                    if (existing == null) {
                        existing = profiles.size() + 1;
                        bodyIds.put(key, existing);
                        profiles.add(
                                new MaterialBoundaryRegistry.Profile(existing, conductance, law));
                        heatingData.add(heating);
                        coolingData.add(cooling);
                    }
                    profileId = existing;
                    if (phase) transitionStates++;
                    else staticStates++;
                }
                int signatureId =
                        signatures.intern(
                                new ResolvedThermalSignature(
                                        ventilation,
                                        profileId,
                                        profileId == 0
                                                ? -1
                                                : Block.BLOCK_STATE_REGISTRY.getId(state),
                                        profileId == 0
                                                ? 0
                                                : StateStaticThermalResolver.fullContactFaces(
                                                        state),
                                        state.isAir(),
                                        profileId == 0
                                                ? -1
                                                : net.minecraft.core.registries.BuiltInRegistries
                                                        .BLOCK
                                                        .getId(block)));
                int radiationProfileId = 0;
                if (staticRadiationEnabled && !isCampfire(state)) {
                    if (state.getFluidState().is(FluidTags.LAVA)) {
                        radiationProfileId = lavaRadiationProfile;
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
        snapshot =
                new Snapshot(
                        signatures.build(),
                        states.build(),
                        new MaterialBoundaryRegistry(profiles),
                        tuning,
                        heatingData.toArray(StateTransitionData.Edge[]::new),
                        coolingData.toArray(StateTransitionData.Edge[]::new));
        FHMain.LOGGER.info(
                "Compiled {} static material states, {} phase states, " + "{} material profiles",
                staticStates,
                transitionStates,
                profiles.size());
        return snapshot;
    }

    public static double dormantTemperatureHalfLifeSeconds() {
        Snapshot current = snapshot;
        return (current == null ? prepare() : current).tuning().dormantTemperatureHalfLifeSeconds();
    }

    public static synchronized void invalidate() {
        snapshot = null;
        profileEpoch++;
    }

    public static int profileEpoch() {
        return profileEpoch;
    }

    public static int mutationFlags(BlockState oldState, BlockState newState) {
        Snapshot current = snapshot;
        return current == null
                ? TOPOLOGY_MUTATION
                : current.states.mutationFlags(oldState, newState);
    }

    public static boolean lavaBlockRadiationEnabled() {
        Snapshot current = snapshot;
        return current != null
                && current.states.hasRadiationMode(
                        MinecraftStateThermalTable.RADIATION_LAVA_SURFACE);
    }

    private static boolean isCampfire(BlockState state) {
        return state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private static byte campfireFlags(BlockState state) {
        if (!isCampfire(state)) {
            return 0;
        }
        int flags = MinecraftStateThermalTable.CAMPFIRE_PRESENT;
        if (state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
            flags |= MinecraftStateThermalTable.CAMPFIRE_LIT;
        }
        return (byte) flags;
    }

    private static double lavaSurfacePowerWPerM2(FHConfig.Common.ThermalRuntime config) {
        double lavaK = config.lavaRadiationTemperatureC.get() + 273.15D;
        double referenceK = config.radiationReferenceTemperatureC.get() + 273.15D;
        return Math.max(
                0.0D,
                config.effectiveLavaEmissivity.get()
                        * STEFAN_BOLTZMANN_W_PER_M2_K4
                        * (Math.pow(lavaK, 4.0D) - Math.pow(referenceK, 4.0D)));
    }

    private static GameplayMaterial classify(BlockState state) {
        if (state.isAir()
                || state.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.EndGatewayBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.StructureVoidBlock
                || state.getBlock() instanceof net.minecraft.world.level.block.LightBlock)
            return null;
        if (state.getBlock() instanceof LeavesBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.REPLACEABLE)) {
            return GameplayMaterial.INSULATING_FABRIC;
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.WOOL_CARPETS)) {
            return GameplayMaterial.INSULATING_FABRIC;
        }
        if (state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES)) {
            return GameplayMaterial.GLASS;
        }
        if (isMetal(state)) return GameplayMaterial.METAL;
        if (isWood(state)) return GameplayMaterial.WOOD;
        if (isEarth(state)) return GameplayMaterial.EARTH;
        if (isMasonry(state)) return GameplayMaterial.MASONRY;
        return GameplayMaterial.GENERIC_SOLID;
    }

    public static MaterialThermalLaw materialLaw(BlockState state) {
        Snapshot current = snapshot;
        if (current == null) current = prepare();
        var profile =
                current.materials.profileOrNull(
                        current.signatures.materialProfileId(current.states.signatureId(state)));
        return profile == null ? null : profile.thermalLaw();
    }

    private static double bodyCapacity(BlockState state) {
        GameplayMaterial material = classify(state);
        if (material == null) return 0;
        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            return WATER_CAPACITY_J_PER_K * state.getFluidState().getAmount() / 8.0;
        }
        return 6 * material.capacity
                + (state.getFluidState().is(FluidTags.WATER) ? WATERLOGGED_CAPACITY_J_PER_K : 0);
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
            Tuning tuning,
            StateTransitionData.Edge[] heatingData,
            StateTransitionData.Edge[] coolingData) {
        public StateTransitionData.Edge transitionData(int profileId, boolean heating) {
            return (heating ? heatingData : coolingData)[profileId];
        }
    }

    public record Tuning(
            double airHeatCapacityJPerBlockK,
            double airMixingWPerBlockK,
            double farFieldConductanceWPerK,
            double dormantTemperatureHalfLifeSeconds,
            MinecraftPhysicalSourceProfile campfire) {}

    private record BodyKey(
            double conductance,
            MaterialThermalLaw law,
            StateTransitionData.Edge heating,
            StateTransitionData.Edge cooling) {}

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
    }
}
