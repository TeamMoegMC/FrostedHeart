/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalGridSwitchObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real sources and accepted fine/coarse owners deliver a funded phase through the server ACK path. */
@GameTestHolder("frostedheart_completed_phase_online")
@PrefixGameTestTemplate(false)
public final class CompletedPhaseOnlineGameTests {
    @GameTest(template = "phase0a_empty", batch = "completed_phase_fine", timeoutTicks = 1600)
    public static void fineInteriorWaterCommitsFundedPhaseAfterRuleResumes(GameTestHelper helper) {
        run(helper, false);
    }

    @GameTest(template = "phase0a_empty", batch = "completed_phase_coarse", timeoutTicks = 1600)
    public static void coarseInteriorWaterCommitsFundedPhaseAfterRuleResumes(GameTestHelper helper) {
        run(helper, true);
    }

    private static void run(GameTestHelper helper, boolean coarse) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        boolean daylight = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        int randomTicks = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
        var water = new BlockPos(coarse ? 8714 : 8202, 200, 8202);
        var fire = water.offset(-2, 1, -2);
        var control = fire.east(64);
        ThermalConvergenceScenes.force(helper, water.offset(-4, -2, -4), 12, true);
        ThermalConvergenceScenes.force(helper, control, 4, true);
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FROZEN_RIVER);
        level.getChunkAt(water).fillBiomesFromNoise((x, y, z, sampler) -> biome,
                level.getChunkSource().randomState().sampler());
        for (int y = -1; y <= 3; y++) for (int z = -3; z <= 3; z++) for (int x = -3; x <= 3; x++)
            level.setBlockAndUpdate(water.offset(x, y, z),
                    (y < 0 || y == 0 && (Math.abs(x) == 3 || Math.abs(z) == 3) ? Blocks.STONE
                            : y == 0 ? Blocks.WATER : Blocks.AIR).defaultBlockState());
        level.setBlockAndUpdate(fire.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(control.below(), Blocks.STONE.defaultBlockState());
        for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(control.above(y), Blocks.AIR.defaultBlockState());
        ThermalConvergenceScenes.fire(helper, fire);
        ThermalConvergenceScenes.fire(helper, control);
        var material = new MaterialSample();
        var air = new ThermalEnvironmentSample();
        var edge = MinecraftThermalProfiles.materialLaw(Blocks.WATER.defaultBlockState()).cooling();
        long[] requested = {0};
        Runnable cleanup = () -> {
            RegionalGridSwitchObservation.end();
            extinguish(helper, fire);
            extinguish(helper, control);
            MinecraftThermalInput.closeActiveLevel(level);
            ThermalConvergenceScenes.force(helper, water.offset(-4, -2, -4), 12, false);
            ThermalConvergenceScenes.force(helper, control, 4, false);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, level.getServer());
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(randomTicks, level.getServer());
        };
        helper.runAfterDelay(1500, cleanup);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(
                        MinecraftThermalInput.sampleMaterial(level, water, material)
                                && material.source() == MaterialSample.Source.LIVE
                                && ThermalConvergenceScenes.fresh(helper, fire.above(), air),
                        "Real fuelled sources establish the live water and Air publications"))
                .thenExecute(() -> {
                    if (coarse) {
                        RegionalGridSwitchObservation.arm(fire, control);
                        RegionalGridSwitchObservation.request(4);
                    }
                }).thenWaitUntil(() -> {
                    if (!coarse) return;
                    helper.assertTrue(RegionalGridSwitchObservation.failure() == null,
                            RegionalGridSwitchObservation.summary());
                    helper.assertTrue(RegionalGridSwitchObservation.completed() >= 1
                                    && ThermalConvergenceScenes.readable(helper, fire.above(), air) && air.airMinimumQuality(),
                            "Use an actually published coarse region, not a substituted material law");
                }).thenExecute(() -> {
                    RegionalGridSwitchObservation.end();
                    helper.assertTrue(interior(helper, water), "The target is still surrounded by actual water");
                    helper.assertTrue(CampfireBlock.isLitCampfire(level.getBlockState(fire))
                                    && CampfireBlock.isLitCampfire(level.getBlockState(control)),
                            "Both actual fuelled sources remain lit on dry foundations during the phase test");
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
                    requested[0] = level.getGameTime();
                    helper.assertTrue(MinecraftThermalInput.requestGameplayPhase(level, water,
                                    Blocks.WATER.defaultBlockState(), MaterialThermalLaw.COOLING),
                            "The production explicit phase control records the finite cooling needed for completion");
                }).thenWaitUntil(() -> helper.assertTrue(
                        MinecraftThermalInput.sampleMaterial(level, water, material)
                                && edge.complete(material.enthalpyJ()),
                        "The real regional material owner reaches the completed energy before a world ACK"))
                .thenIdle(40).thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(water).is(Blocks.WATER),
                            "The explicit random-tick rule still pauses world conversion");
                    level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(3, level.getServer());
                }).thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(water).is(Blocks.WATER),
                        "An energy-complete interior material must commit after the hard rule resumes"))
                .thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, water, material)
                                && material.source() == MaterialSample.Source.LIVE,
                        "The converted material is republished through the real geometry/ACK handoff"))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(water).is(com.teammoeg.frostedheart.bootstrap.common.FHBlocks.THIN_ICE_BLOCK.get()),
                            "The actual target state is thin ice");
                    helper.assertTrue(interior(helper, water), "No bank construction is needed for the completed physical phase");
                    helper.assertTrue(Math.abs(material.enthalpyJ() - edge.targetEnthalpyJ()) < 10000,
                            "Ordinary exchange may continue, but the 363200J latent interval must not be charged twice");
                    FHMain.LOGGER.info("THERMAL_COMPLETED_PHASE_ONLINE,coarse={},elapsedTicks={},state={},enthalpyJ={},targetH={},temperatureC={},sampleTick={}",
                            coarse, level.getGameTime() - requested[0], level.getBlockState(water), material.enthalpyJ(),
                            edge.targetEnthalpyJ(), material.temperatureC(), material.sampleTick());
                    cleanup.run();
                }).thenSucceed();
    }

    private static boolean interior(GameTestHelper helper, BlockPos p) {
        var level = helper.getLevel();
        return level.getFluidState(p.west()).is(FluidTags.WATER) && level.getFluidState(p.east()).is(FluidTags.WATER)
                && level.getFluidState(p.north()).is(FluidTags.WATER) && level.getFluidState(p.south()).is(FluidTags.WATER);
    }

    private static void extinguish(GameTestHelper helper, BlockPos p) {
        if (CampfireBlock.isLitCampfire(helper.getLevel().getBlockState(p)))
            ContinuousAirProductionGameTests.useItem(helper, p, Items.IRON_SHOVEL);
    }
}
