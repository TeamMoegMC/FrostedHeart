/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalGridSwitchObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Source-free coarse retirement, actual chunk unload and persisted heat recovery. */
@GameTestHolder("frostedheart_coarse_cooling")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ContinuousAirCoarseCoolingProductionGameTests {
    private static LevelChunk watchedChunk;
    private static boolean unloaded;

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload event) {
        if (event.getChunk() == watchedChunk) unloaded = true;
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void extinguishedCoarseRegionRetiresSavesUnloadsAndRelights(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(1024, 80, 1024);
        var controlOrigin = origin.east(256);
        ThermalConvergenceScenes.force(helper, origin, 16, true);
        ThermalConvergenceScenes.force(helper, controlOrigin, 16, true);
        var fire = ContinuousAirProductionGameTests.room(helper, origin);
        var control = ContinuousAirProductionGameTests.room(helper, controlOrigin);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        ContinuousAirProductionGameTests.useItem(helper, control, Items.FLINT_AND_STEEL);
        var probe = fire.above();
        var sample = new ThermalEnvironmentSample();
        long[] extinguished = {0}, coldSample = {0};
        double[] temperature = new double[2];
        LevelChunk[] original = {null};

        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample)
                            && !sample.airMinimumQuality(), "Establish a real detailed field before coarsening");
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, control.above(), sample),
                    "The independent real control room must also be ready");
        }).thenExecute(() -> {
            RegionalGridSwitchObservation.arm(fire, control);
            RegionalGridSwitchObservation.request(4);
        }).thenWaitUntil(() -> {
            helper.assertTrue(RegionalGridSwitchObservation.failure() == null, RegionalGridSwitchObservation.summary());
            helper.assertTrue(RegionalGridSwitchObservation.completed() >= 1
                            && ThermalConvergenceScenes.readable(helper, probe, sample) && sample.airMinimumQuality(),
                    "Publish the actual coarse field before extinguishing its only source");
        }).thenExecute(() -> {
            temperature[0] = sample.airTemperatureC();
            RegionalGridSwitchObservation.end();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            ContinuousAirProductionGameTests.useItem(helper, control, Items.IRON_SHOVEL);
            extinguished[0] = level.getGameTime();
        }).thenIdle(160).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 0,
                    "No maintaining fire may supply heat during the source-free observation");
            helper.assertTrue(!MinecraftThermalInput.describeAirRead(level, probe).startsWith("geometry_current"),
                    "The low-residual, source-free coarse region must finish its ordinary retirement");
            var stored = MinecraftThermalInput.dormantState(level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4));
            helper.assertTrue(stored != null && stored.oldestAirSavedTick() >= extinguished[0],
                    "Retirement must retain a temperature field captured after extinguishing");
        }).thenExecute(() -> {
            var stored = MinecraftThermalInput.dormantState(level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4));
            coldSample[0] = stored.oldestAirSavedTick();
            temperature[1] = MinecraftThermalInput.gameplayPassiveEnvironment(level, probe,
                    WorldTemperature.naturalAir(level, probe));
            helper.assertTrue(Double.isFinite(temperature[1]) && Math.abs(temperature[1] - temperature[0]) < .25,
                    "Source-free retirement must preserve finite coarse heat for gameplay queries");
            original[0] = level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4);
            watchedChunk = original[0]; unloaded = false;
            level.getServer().saveEverything(true, true, true);
            FHMain.LOGGER.info("THERMAL_COARSE_COOLING,extinguishedTick={},sampleTick={},hotC={},coolingC={},enabledSources={}",
                    extinguished[0], coldSample[0], temperature[0], temperature[1],
                    MinecraftThermalInput.enabledPhysicalSourceCount(level));
            ThermalConvergenceScenes.force(helper, origin, 16, false);
        }).thenWaitUntil(() -> helper.assertTrue(
                unloaded && level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) == null
                        && MinecraftThermalInput.observedNativeCheckpoints(level).pending() == 0,
                "The extinguished coarse region must complete its actual native save and chunk unload"))
        .thenExecute(() -> {
            var state = MinecraftThermalInput.dormantState(original[0]);
            helper.assertTrue(state != null && state.oldestAirSavedTick() >= 0,
                    "Actual retirement must retain temperature history");
            var tag = new CompoundTag();
            state.encode(tag);
            helper.assertTrue(tag.getCompound("FrostedHeartThermal").getInt("version") == 6,
                    "The real source-free coarse save must use the current system format");
            long savedTick = -1;
            var sections = tag.getCompound("FrostedHeartThermal").getList("sections", Tag.TAG_COMPOUND);
            for (int index = 0; index < sections.size(); index++) {
                var section = sections.getCompound(index);
                if (section.getInt("y") != (probe.getY() >> 4) || !section.contains("air", Tag.TAG_COMPOUND)) continue;
                var air = AirFieldCheckpoint.decode(section.getCompound("air"));
                if (air != null && Double.isFinite(air.sampleC(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5)))
                    savedTick = air.sampleTickAt(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5, section.getLong("tick"));
            }
            helper.assertTrue(savedTick >= coldSample[0], "The actual saved probe must retain its source-free retirement field");
            ThermalConvergenceScenes.force(helper, origin, 16, true);
        }).thenWaitUntil(() -> {
            var loaded = level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4);
            helper.assertTrue(loaded != null && loaded != original[0], "Reload into a new real chunk instance");
        }).thenExecute(() -> {
            double restored = MinecraftThermalInput.gameplayPassiveEnvironment(level, probe,
                    WorldTemperature.naturalAir(level, probe));
            helper.assertTrue(Double.isFinite(restored) && Math.abs(restored - temperature[1]) < .25,
                    "Real reload must preserve source-free coarse heat: before=" + temperature[1] + ",after=" + restored);
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        }).thenWaitUntil(() -> helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample)
                        && sample.airSampleTick() > coldSample[0] && Double.isFinite(sample.airTemperatureC()),
                "The reloaded real source must wake and advance the region again"))
        .thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_COARSE_COOLING_RESULT,coldSampleTick={},reloadedSampleTick={},rootPeakBytes={}",
                    coldSample[0], sample.airSampleTick(), MinecraftThermalInput.thermalBudgetPeakBytes());
            watchedChunk = null;
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            ThermalConvergenceScenes.force(helper, origin, 16, false);
            ThermalConvergenceScenes.force(helper, controlOrigin, 16, false);
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "Source-free retirement, restore and close must release all thermal ownership"))
        .thenSucceed();
    }
}
