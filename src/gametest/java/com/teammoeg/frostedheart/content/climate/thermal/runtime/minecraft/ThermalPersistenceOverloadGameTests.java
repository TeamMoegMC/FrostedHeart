/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_persistence_overload")
@PrefixGameTestTemplate(false)
public final class ThermalPersistenceOverloadGameTests {
    @GameTest(template = "phase0a_empty", batch = "persistence_worker_stalled", timeoutTicks = 1600)
    public static void actualDiskSaveSucceedsWhileEveryThermalWorkerIsStalled(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        var pool = ThermalWorkerPool.shared();
        var started = new CountDownLatch(pool.parallelism());
        var release = new CountDownLatch(1);
        helper.startSequence().thenIdle(300).thenWaitUntil(() -> {
            ThermalConvergenceScenes.read(helper, fire.above(), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample), "Establish a real committed field before the stall");
        }).thenExecute(() -> {
            for (int worker = 0; worker < pool.parallelism(); worker++) helper.assertTrue(pool.tryExecute(() -> {
                started.countDown();
                try { release.await(20, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }), "Queue a real worker stall");
        }).thenWaitUntil(() -> helper.assertTrue(started.getCount() == 0, "All actual thermal workers are deliberately stalled"))
        .thenIdle(60).thenExecute(() -> {
            try {
                var chunk = level.getChunkAt(fire);
                level.setBlockAndUpdate(fire.below(), Blocks.IRON_BLOCK.defaultBlockState());
                chunk.setUnsaved(true);
                var save = ObfuscationReflectionHelper.findMethod(ChunkMap.class, "m_140258_", ChunkAccess.class);
                long begin = System.nanoTime();
                helper.assertTrue((boolean) save.invoke(level.getChunkSource().chunkMap, chunk), "Vanilla must really serialize the dirty chunk");
                level.getChunkSource().chunkMap.flushWorker();
                long elapsed = System.nanoTime() - begin;
                helper.assertTrue(release.getCount() == 1, "The save completes before any thermal worker resumes");
                var disk = level.getChunkSource().chunkMap.read(chunk.getPos()).join().orElseThrow();
                var pending = disk.getCompound("FrostedHeartThermal").getCompound("pending");
                double pendingJ = 0;
                for (long value : pending.getLongArray("joules")) pendingJ += Double.longBitsToDouble(value);
                helper.assertTrue(pendingJ > 0, "The actual disk NBT retains heat produced while every thermal worker is stalled");
                var saved = com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.decode(
                        disk, chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
                var floor = fire.below();
                int position = (floor.getX() & 15) | (floor.getZ() & 15) << 4 | (floor.getY() & 15) << 8;
                var material = new com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample();
                boolean ironSaved = false;
                for (var section : disk.getList("sections", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    var palette = ((net.minecraft.nbt.CompoundTag) section).getCompound("block_states")
                            .getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
                    for (var state : palette) if (((net.minecraft.nbt.CompoundTag) state).getString("Name").equals("minecraft:iron_block")) ironSaved = true;
                }
                helper.assertTrue(ironSaved, "The real disk chunk includes the newly placed iron block");
                // An unchanged ambient temperature has no explicit thermal residual.
                // A missing entry initializes from the real block; an old stone entry must not survive.
                helper.assertTrue(saved != null && saved.readMaterial(floor.getY() >> 4, position,
                        net.minecraft.world.level.block.Block.getId(Blocks.IRON_BLOCK.defaultBlockState()), material)
                        != com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.MaterialRead.MISMATCH,
                        "Saved thermal residuals must not retain the replaced stone identity");
                helper.assertTrue(elapsed < 2_000_000_000L, "Persistence must not wait for the stalled thermal pool: ns=" + elapsed);
                FHMain.LOGGER.info("THERMAL_STALLED_POOL_SAVE,workers={},stallTicks=60,actualVanillaWrite=true,flushed=true,pendingHeatJ={},materialMutationSaved=true,saveNs={}",
                        pool.parallelism(), pendingJ, elapsed);
            } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            finally { release.countDown(); }
        }).thenIdle(100).thenExecute(() -> {
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0, "Stall recovery releases all thermal ownership"))
        .thenSucceed();
    }

}
