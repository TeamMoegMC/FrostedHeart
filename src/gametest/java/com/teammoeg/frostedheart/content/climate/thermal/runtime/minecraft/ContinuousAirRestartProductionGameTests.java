/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Run in two separate server JVMs against the same named world, with cleanup disabled. */
@GameTestHolder("frostedheart_restart_production")
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class ContinuousAirRestartProductionGameTests {
    private ContinuousAirRestartProductionGameTests() {}
    private record SavedWrite(GameTestHelper helper, BlockPos fire, RestartObservation observation) { }
    private static SavedWrite observingWrite;

    /** Observe the exact outgoing tag after the production save listener, including shutdown saves. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void saved(ChunkDataEvent.Save event) {
        var write = observingWrite;
        if (write == null || event.getLevel() != write.helper.getLevel()
                || !event.getChunk().getPos().equals(new net.minecraft.world.level.ChunkPos(write.fire))) return;
        var chunk = event.getChunk();
        var state = com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.decode(
                event.getData(), chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
        write.helper.assertTrue(state != null, "The serialized chunk must contain its thermal checkpoint");
        var material = state.materials(write.fire.below().getY() >> 4);
        write.helper.assertTrue(material != null, "The outgoing tag must contain the actual floor");
        int index = floorIndex(write.helper, material, write.fire.below());
        var observed = write.observation;
        observed.materialH = material.enthalpyJ(index);
        observed.branch = material.branch(index);
        observed.materialTick = material.savedTick(index);
        observed.setDirty();
        // Save only observations. Re-saving the chunk would produce a new state.
        write.helper.getLevel().getDataStorage().save();
        FHMain.LOGGER.info("THERMAL_RESTART_SERIALIZED,gameTick={},materialTick={},materialH={},branch={}",
                write.helper.getLevel().getGameTime(), observed.materialTick, observed.materialH, observed.branch);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 1800)
    public static void diskRestartPreservesAirAndMaterialThenHeatsNewPassage(GameTestHelper helper) {
        var level = helper.getLevel();
        var saved = level.getDataStorage().computeIfAbsent(RestartObservation::load, RestartObservation::new,
                "frostedheart_continuous_air_restart_observation");
        BlockPos origin = new BlockPos(2048, 80, 2048);
        BlockPos fire = origin.offset(8, 1, 8), near = fire.above(), far = near.west(2);
        BlockPos beyond = origin.offset(12, 2, 8);
        level.setChunkForced(fire.getX() >> 4, fire.getZ() >> 4, true);
        var nearAir = new ThermalEnvironmentSample();
        var farAir = new ThermalEnvironmentSample();
        var material = new MaterialSample();
        if (!saved.recorded) {
            ContinuousAirProductionGameTests.room(helper, origin);
            for (int y = 1; y < 8; y++) for (int z = 1; z < 16; z++)
                level.setBlockAndUpdate(origin.offset(10, y, z), Blocks.STONE.defaultBlockState());
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
            helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
                sample(helper, near, nearAir);
                sample(helper, far, farAir);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(nearAir)
                                && nearAir.airTemperatureC() > farAir.airTemperatureC() + .01,
                        "First JVM must establish real spatial heat");
            }).thenExecute(() -> ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL))
                    .thenIdle(60).thenWaitUntil(() -> {
                        sample(helper, near, nearAir);
                        sample(helper, far, farAir);
                        helper.assertTrue(ContinuousAirProductionTiming.sourceField(nearAir)
                                        && level.getGameTime() - nearAir.airSampleTick() <= 40,
                                "First JVM must publish extinguished heat before saving");
                        helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, fire.below(), material)
                                        && material.source() == MaterialSample.Source.LIVE,
                                "First JVM must publish the actual floor material");
                    }).thenExecute(() -> {
                        saved.recorded = true;
                        saved.nearC = nearAir.airTemperatureC();
                        saved.farC = farAir.airTemperatureC();
                        saved.materialC = material.temperatureC();
                        saved.materialH = material.enthalpyJ();
                        saved.branch = material.branch();
                        saved.tick = level.getGameTime();
                        saved.setDirty();
                        observingWrite = new SavedWrite(helper, fire, saved);
                        level.getServer().saveEverything(true, true, true);
                        FHMain.LOGGER.info("THERMAL_RESTART_SAVED,tick={},nearC={},farC={},materialC={},materialH={},branch={}",
                                saved.tick, saved.nearC, saved.farC, saved.materialC, saved.materialH, saved.branch);
                    }).thenSucceed();
            return;
        }
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            helper.assertTrue(level.getBlockState(fire).is(Blocks.CAMPFIRE)
                            && !level.getBlockState(fire).getValue(CampfireBlock.LIT),
                    "Second JVM must load the actual extinguished campfire from disk");
            var checkpoint = storedFloor(helper, fire);
            int floorIndex = floorIndex(helper, checkpoint, fire.below());
            helper.assertTrue(Double.doubleToLongBits(checkpoint.enthalpyJ(floorIndex)) == Double.doubleToLongBits(saved.materialH)
                            && checkpoint.branch(floorIndex) == saved.branch
                            && checkpoint.savedTick(floorIndex) == saved.materialTick,
                    "Actual disk material checkpoint must preserve its recorded H, branch and real sample tick");
            FHMain.LOGGER.info("THERMAL_RESTART_MATERIAL_TIME,savedMaterialTick={},loadedMaterialTick={}",
                    saved.materialTick, checkpoint.savedTick(floorIndex));
            sample(helper, near, nearAir);
            sample(helper, far, farAir);
            helper.assertTrue(Math.abs(nearAir.airTemperatureC() - saved.nearC) < .25
                            && Math.abs(farAir.airTemperatureC() - saved.farC) < .25
                            && nearAir.airTemperatureC() > farAir.airTemperatureC() + .01,
                    "Second JVM must restore the real near/far heat history: "
                            + nearAir.airTemperatureC() + "," + farAir.airTemperatureC());
            helper.assertTrue(MinecraftThermalInput.sampleMaterial(level, fire.below(), material)
                            && Math.abs(material.temperatureC() - saved.materialC) < .25
                            && material.branch() == saved.branch,
                    "Second JVM must restore the actual floor material and its branch");
            FHMain.LOGGER.info("THERMAL_RESTART_LOADED,savedTick={},tick={},nearC={},farC={},materialC={},materialH={},branch={},source={}",
                    saved.tick, level.getGameTime(), nearAir.airTemperatureC(), farAir.airTemperatureC(),
                    material.temperatureC(), material.enthalpyJ(), material.branch(), material.source());
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        }).thenIdle(200).thenWaitUntil(() -> {
            sample(helper, near, nearAir);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(nearAir) && level.getGameTime() - nearAir.airSampleTick() <= 40,
                    "Actual ignition after JVM restart must republish spatial Air");
        }).thenExecute(() -> {
            for (int y = 1; y <= 3; y++) for (int z = 7; z <= 9; z++)
                level.setBlockAndUpdate(origin.offset(10, y, z), Blocks.AIR.defaultBlockState());
        }).thenIdle(300).thenWaitUntil(() -> {
            sample(helper, beyond, farAir);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(farAir) && level.getGameTime() - farAir.airSampleTick() <= 40
                            && farAir.airTemperatureC() > material.temperatureC() + .005,
                    "The restarted source must discover local detail in the actual newly opened passage");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_RESTART_PASS,tick={},openedC={},sampleTick={}",
                    level.getGameTime(), farAir.airTemperatureC(), farAir.airSampleTick());
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
        }).thenSucceed();
    }

    private static void sample(GameTestHelper helper, BlockPos pos, ThermalEnvironmentSample out) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), pos,
                WorldTemperature.naturalAir(helper.getLevel(), pos), out);
    }

    private static com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState storedFloor(
            GameTestHelper helper, BlockPos fire) {
        var checkpoint = MinecraftThermalInput.dormantState(helper.getLevel().getChunkAt(fire));
        helper.assertTrue(checkpoint != null, "Actual world save must create a thermal chunk checkpoint");
        var floor = checkpoint.materials(fire.below().getY() >> 4);
        helper.assertTrue(floor != null, "Actual world save must include the floor material");
        return floor;
    }

    private static int floorIndex(GameTestHelper helper,
            com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState checkpoint, BlockPos floor) {
        int position = floor.getX() & 15 | (floor.getZ() & 15) << 4 | (floor.getY() & 15) << 8;
        for (int index = 0; index < checkpoint.size(); index++) if (checkpoint.position(index) == position) return index;
        helper.fail("Actual world save must contain the observed floor block");
        return -1;
    }

    /** Stores observations only; all thermal state is saved and restored by the production chunk lifecycle. */
    private static final class RestartObservation extends SavedData {
        boolean recorded;
        double nearC, farC, materialC, materialH;
        byte branch;
        long tick, materialTick;

        static RestartObservation load(CompoundTag tag) {
            var observation = new RestartObservation();
            observation.recorded = tag.getBoolean("recorded");
            observation.nearC = tag.getDouble("nearC");
            observation.farC = tag.getDouble("farC");
            observation.materialC = tag.getDouble("materialC");
            observation.materialH = tag.getDouble("materialH");
            observation.branch = tag.getByte("branch");
            observation.tick = tag.getLong("tick");
            observation.materialTick = tag.getLong("materialTick");
            return observation;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("recorded", recorded);
            tag.putDouble("nearC", nearC);
            tag.putDouble("farC", farC);
            tag.putDouble("materialC", materialC);
            tag.putDouble("materialH", materialH);
            tag.putByte("branch", branch);
            tag.putLong("tick", tick);
            tag.putLong("materialTick", materialTick);
            return tag;
        }
    }
}
