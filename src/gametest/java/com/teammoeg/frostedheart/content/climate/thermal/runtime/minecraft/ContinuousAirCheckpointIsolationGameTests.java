/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual source work and ordinary chunk lifecycle, without delaying or modifying the worker. */
@GameTestHolder("frostedheart_checkpoint_isolation")
@PrefixGameTestTemplate(false)
public final class ContinuousAirCheckpointIsolationGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 1000)
    public static void actualUnrelatedChunkSaveDoesNotJoinDenseBase(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 2, 0));
        ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origin);
        int farX = (origin.getX() >> 4) + 64, farZ = origin.getZ() >> 4;
        level.setChunkForced(farX, farZ, true);
        var unrelated = level.getChunk(farX, farZ);
        // ChunkMap's actual single-chunk save posts Forge's event and writes the resulting NBT.
        var saveChunk = ObfuscationReflectionHelper.findMethod(ChunkMap.class, "m_140258_", ChunkAccess.class);
        boolean[] savedDuringWork = {false};
        for (int delay = 1; delay <= 800; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (!savedDuringWork[0] && MinecraftThermalInput.observedWorkerPending(level)) {
                    long[] before = MinecraftThermalInput.observedCheckpointCalls(level);
                    unrelated.setUnsaved(true);
                    try {
                        helper.assertTrue((boolean) saveChunk.invoke(level.getChunkSource().chunkMap, unrelated),
                                "The actual unrelated chunk must be saved");
                    } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                    long[] after = MinecraftThermalInput.observedCheckpointCalls(level);
                    helper.assertTrue(after[0] == before[0] && after[2] == before[2] + 1,
                            "Saving one unrelated chunk must skip its thermal checkpoint barrier");
                    if (after[4] == before[4] && elapsed < 800) return;
                    helper.assertTrue(after[4] > before[4], "Observe the actual save callback while the worker remains pending");
                    savedDuringWork[0] = true;
                    level.setChunkForced(farX, farZ, false);
                    FHMain.LOGGER.info("THERMAL_UNRELATED_SAVE,elapsedTicks={},workerPending=true,actualChunkSaved=true,unrelatedSaveBarrierAdded=false,{}",
                            elapsed, MinecraftThermalInput.observedMainWaits(level));
                    helper.succeed();
                    return;
                }
                if (elapsed == 800) helper.fail("Actual source/save isolation was not observed; "
                        + MinecraftThermalInput.observedMainWaits(level));
            });
        }
    }
}
