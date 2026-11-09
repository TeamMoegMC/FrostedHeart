/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Three independent ticket paths exercise actual vanilla saves, unloads and disk reloads for ten minutes. */
@GameTestHolder("frostedheart_chunk_churn")
@PrefixGameTestTemplate(false)
public final class ThermalChunkChurnGameTests {
    private static final int CYCLE_TICKS = 600, OBSERVATION_TICKS = 12_000;

    @GameTest(template = "phase0a_empty", timeoutTicks = 14_000)
    public static void threeTicketPathsReleaseTheirChunksAndHeapAfterRepeatedDiskReloads(GameTestHelper helper) {
        var scene = new Scene(helper);
        scene.move(0);
        helper.startSequence().thenIdle(400).thenWaitUntil(scene::assertCurrentFields)
        .thenExecute(() -> ContinuousAirProductionTiming.eachTick(helper, OBSERVATION_TICKS, scene::tick))
        .thenIdle(OBSERVATION_TICKS + 400).thenExecute(() -> {
            scene.assertAllUnloaded();
            helper.getLevel().getChunkSource().chunkMap.flushWorker();
            MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "All thermal ownership returns after real chunk churn"))
        .thenExecute(System::gc).thenIdle(40).thenExecute(System::gc).thenIdle(40).thenExecute(() -> {
            long retained = scene.retired.stream().filter(reference -> reference.get() != null).count();
            if (retained != 0) {
                try {
                    var diagnostics = java.lang.management.ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class);
                    diagnostics.dumpHeap(helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                            .resolve("thermal-churn-retained-" + System.nanoTime() + ".hprof").toAbsolutePath().toString(), true);
                } catch (java.io.IOException failure) { throw new AssertionError("Capture retained-chunk heap for analysis", failure); }
            }
            helper.assertTrue(retained == 0, "Unloaded LevelChunks must be collectable; retained=" + retained);
            helper.assertTrue(scene.reloads >= 48 && scene.unloads >= 48, "Observe repeated real disk reload and unload on all three paths");
            FHMain.LOGGER.info("THERMAL_CHURN_RESULT,observationTicks={},paths=3,reloads={},checkedUnloads={},retiredChunks={},retainedChunks={},baselineHeapBytes={},maximumCollectedHeapBytes={},maximumNativeCheckpoints={},rootBytes={}",
                    OBSERVATION_TICKS, scene.reloads, scene.unloads, scene.retired.size(), retained,
                    scene.heapBaseline, scene.maximumCollectedHeap, scene.maximumNativeCheckpoints, MinecraftThermalInput.thermalBudgetUsedBytes());
        }).thenSucceed();
    }

    private static final class Scene {
        final GameTestHelper helper;
        final BlockPos[][] origins = new BlockPos[2][3], fires = new BlockPos[2][3];
        final LevelChunk[] current = new LevelChunk[3];
        final ArrayList<WeakReference<LevelChunk>> retired = new ArrayList<>();
        final ThermalEnvironmentSample sample = new ThermalEnvironmentSample();
        int group, reloads, unloads;
        long heapBaseline, maximumCollectedHeap, maximumNativeCheckpoints;

        Scene(GameTestHelper helper) {
            this.helper = helper;
            for (int site = 0; site < 2; site++) for (int lane = 0; lane < 3; lane++)
                origins[site][lane] = helper.absolutePos(new BlockPos(520 + 512 * lane, 0, 128 * site));
        }
        void force(BlockPos origin, boolean value) {
            for (int x = (origin.getX() >> 4) - 1; x <= (origin.getX() >> 4) + 2; x++)
                for (int z = (origin.getZ() >> 4) - 1; z <= (origin.getZ() >> 4) + 2; z++) helper.getLevel().setChunkForced(x, z, value);
        }
        void move(int next) {
            for (int lane = 0; lane < 3; lane++) {
                if (current[lane] != null) {
                    retired.add(new WeakReference<>(current[lane])); current[lane] = null;
                    force(origins[group][lane], false);
                }
                force(origins[next][lane], true);
                if (fires[next][lane] == null) fires[next][lane] = ContinuousAirProductionGameTests.room(helper, origins[next][lane]);
                else {
                    helper.assertTrue(helper.getLevel().getBlockState(fires[next][lane]).is(Blocks.CAMPFIRE), "Actual chunk reload preserves its saved source block");
                    ThermalConvergenceScenes.fuel(helper, fires[next][lane], 1); reloads++;
                }
                if (!CampfireBlock.isLitCampfire(helper.getLevel().getBlockState(fires[next][lane])))
                    ContinuousAirProductionGameTests.useItem(helper, fires[next][lane], Items.FLINT_AND_STEEL);
                current[lane] = helper.getLevel().getChunkAt(fires[next][lane]);
            }
            group = next;
        }
        void assertCurrentFields() {
            for (var fire : fires[group]) {
                ThermalConvergenceScenes.read(helper, fire.above(), sample);
                helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                        && helper.getLevel().getGameTime() - sample.airSampleTick() <= 40, "The currently loaded real source keeps publishing after reload: " + fire);
            }
        }
        void assertAllUnloaded() {
            for (var site : fires) for (var fire : site) helper.assertTrue(
                    helper.getLevel().getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) == null,
                    "Vanilla completes the actual unload: " + fire);
        }
        void tick(int elapsed) {
            var nativeState = MinecraftThermalInput.observedNativeCheckpoints(helper.getLevel());
            maximumNativeCheckpoints = Math.max(maximumNativeCheckpoints, nativeState.pending());
            helper.assertTrue(nativeState.pending() <= 128, "Thermal unload ownership remains bounded: " + nativeState);
            if (elapsed % CYCLE_TICKS == 500) {
                assertCurrentFields();
                if (elapsed > CYCLE_TICKS) for (var fire : fires[1 - group]) {
                    helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) == null,
                            "The previous ticket path really unloads before the next move: " + fire);
                    unloads++;
                }
            }
            if (elapsed % CYCLE_TICKS == 0) {
                if (elapsed == OBSERVATION_TICKS) {
                    for (int lane = 0; lane < 3; lane++) {
                        retired.add(new WeakReference<>(current[lane])); current[lane] = null;
                        force(origins[group][lane], false);
                    }
                } else move(1 - group);
            }
            if (elapsed % 3000 == 0) {
                System.gc();
                long heap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                if (heapBaseline == 0) heapBaseline = heap;
                maximumCollectedHeap = Math.max(maximumCollectedHeap, heap);
                helper.assertTrue(heap <= heapBaseline + 256L * 1024 * 1024, "Retained heap must stabilize once both sets of chunks have been visited: baseline=" + heapBaseline + ",current=" + heap);
                FHMain.LOGGER.info("THERMAL_CHURN_PROGRESS,elapsedTicks={},reloads={},checkedUnloads={},collectedHeapBytes={},nativeCheckpoints={},waits={}",
                        elapsed, reloads, unloads, heap, nativeState, MinecraftThermalInput.observedMainWaits(helper.getLevel()));
            }
        }
    }
}
