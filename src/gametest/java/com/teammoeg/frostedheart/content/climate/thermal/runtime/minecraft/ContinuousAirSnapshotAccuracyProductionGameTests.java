/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual pending-worker disk saves compared with the same scene's subsequently completed field. */
@GameTestHolder("frostedheart_snapshot_accuracy")
@PrefixGameTestTemplate(false)
public final class ContinuousAirSnapshotAccuracyProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 1400)
    public static void pendingDenseBaseSavePreservesHeatingAndExtinguishedAir(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 2, 0));
        var base = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, origin);
        BlockPos probe = base.fires()[1].above();
        var saveChunk = ObfuscationReflectionHelper.findMethod(ChunkMap.class, "m_140258_", ChunkAccess.class);
        int[] stage = {0};
        int[] observations = {0};
        var air = new ThermalEnvironmentSample();
        for (int delay = 1; delay <= 1200; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (elapsed == 1) helper.assertTrue(true,
                        "The actual dimension must use the selected snapshot checkpoint path");
                if (stage[0] == 1 && elapsed >= 600) {
                    for (BlockPos fire : base.fires()) ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                    stage[0] = 2;
                }
                if ((stage[0] == 0 && elapsed >= 240 || stage[0] == 2 && elapsed >= 620)
                        && MinecraftThermalInput.observedWorkerPending(level)) {
                    MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, probe, WorldTemperature.naturalAir(level, probe), air);
                    if (!air.airAvailable() || air.airSampleTick() >= level.getGameTime() / 20 * 20) return;
                    var chunk = level.getChunkAt(probe);
                    var before = MinecraftThermalInput.observedCheckpointStatistics(level);
                    chunk.setUnsaved(true);
                    try { helper.assertTrue((boolean) saveChunk.invoke(level.getChunkSource().chunkMap, chunk), "Write the actual related chunk NBT"); }
                    catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
                    var after = MinecraftThermalInput.observedCheckpointStatistics(level);
                    if (after.pendingSaveSnapshots() == before.pendingSaveSnapshots()) return;
                    DormantChunkThermalState saved = MinecraftThermalInput.dormantState(chunk);
                    helper.assertTrue(saved != null, "The native save must own a complete thermal snapshot");
                    saved = saved.copyForCapture();
                    long savedTick = saved.oldestAirSavedTick();
                    // This separate correctness observation deliberately completes the real submitted work.
                    // It is never used as a latency benchmark or to advance unsent simulated input.
                    helper.assertTrue(MinecraftThermalInput.awaitSubmittedCut(level), "Complete the actual reference cut");
                    DormantChunkThermalState reference = MinecraftThermalInput.checkpointForObservation(level, chunk);
                    helper.assertTrue(reference != null, "Read the real completed reference field");
                    long referenceTick = reference.oldestAirSavedTick();
                    if (stage[0] == 0 && referenceTick <= savedTick) return;
                    double maximum = 0;
                    int points = 0;
                    var position = new BlockPos.MutableBlockPos();
                    for (int y = origin.getY(); y <= origin.getY() + 12; y++)
                        for (int z = chunk.getPos().getMinBlockZ(); z < chunk.getPos().getMinBlockZ() + 16; z++)
                            for (int x = chunk.getPos().getMinBlockX(); x < chunk.getPos().getMinBlockX() + 16; x++) {
                                if (!chunk.getBlockState(position.set(x, y, z)).isAir()) continue;
                                for (int corner = 0; corner < 8; corner++) {
                                    double px = x + ((corner & 1) == 0 ? .25 : .75);
                                    double py = y + ((corner & 2) == 0 ? .25 : .75);
                                    double pz = z + ((corner & 4) == 0 ? .25 : .75);
                                    double candidateC = saved.sampleAt(px, py, pz, level.getGameTime(), MinecraftThermalInput.dormantHalfLifeSeconds(), level, position);
                                    double referenceC = reference.sampleAt(px, py, pz, level.getGameTime(), MinecraftThermalInput.dormantHalfLifeSeconds(), level, position);
                                    if (!Double.isFinite(candidateC) && !Double.isFinite(referenceC)) continue;
                                    double naturalC = WorldTemperature.naturalAir(level, position.set(x, y, z));
                                    if (!Double.isFinite(candidateC)) candidateC = naturalC;
                                    if (!Double.isFinite(referenceC)) referenceC = naturalC;
                                    maximum = Math.max(maximum, Math.abs(candidateC - referenceC));
                                    points++;
                                }
                            }
                    helper.assertTrue(points > 0 && maximum <= 1.0, "Real stored/reference Air should remain within1C; maximum=" + maximum);
                    FHMain.LOGGER.info("THERMAL_SNAPSHOT_ACCURACY,stage={},gameTick={},savedAirTick={},referenceAirTick={},points={},maximumAirDifferenceC={},geometry={}",
                            stage[0] == 0 ? "heating" : "extinguished", level.getGameTime(), savedTick, referenceTick, points, maximum,
                            MinecraftThermalInput.describeAirRead(level, probe));
                    verifyMatchedPublishedField(helper, reference, chunk, origin);
                    observations[0]++;
                    if (stage[0] == 0) stage[0] = 1;
                    else { helper.assertTrue(observations[0] == 2, "Observe real heating and extinguished saves"); stage[0] = 3; helper.succeed(); }
                }
                if (elapsed == 1200 && stage[0] != 3) helper.fail("Pending-worker stored/reference comparison not covered; stage=" + stage[0]);
            });
        }
    }

    /** Compare the real completed checkpoint with live samples at their actual sample tick. */
    private static void verifyMatchedPublishedField(GameTestHelper helper, DormantChunkThermalState checkpoint,
            net.minecraft.world.level.chunk.LevelChunk chunk, BlockPos origin) {
        var level = helper.getLevel();
        var publication = MinecraftThermalInput.queryPublicationForObservation(level);
        helper.assertTrue(publication != null, "The real completed query publication must remain available");
        var live = new com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication.MutableSample();
        var position = new BlockPos.MutableBlockPos();
        int points = 0, pruned = 0;
        double maximum = 0;
        for (int y = origin.getY(); y <= origin.getY() + 12; y++)
            for (int z = chunk.getPos().getMinBlockZ(); z < chunk.getPos().getMinBlockZ() + 16; z++)
                for (int x = chunk.getPos().getMinBlockX(); x < chunk.getPos().getMinBlockX() + 16; x++) {
                    if (!chunk.getBlockState(position.set(x, y, z)).isAir()) continue;
                    for (int corner = 0; corner < 8; corner++) {
                        double px = x + ((corner & 1) == 0 ? .25 : .75);
                        double py = y + ((corner & 2) == 0 ? .25 : .75);
                        double pz = z + ((corner & 4) == 0 ? .25 : .75);
                        if (!publication.tryReadAir(px, py, pz, live)) continue;
                        double restored = checkpoint.sampleAt(px, py, pz, live.sampleTick(),
                                MinecraftThermalInput.dormantHalfLifeSeconds(), level, position);
                        if (!Double.isFinite(restored)) {
                            restored = WorldTemperature.naturalAir(level, position.set(x, y, z));
                            pruned++;
                        }
                        maximum = Math.max(maximum, Math.abs(restored - live.temperatureC()));
                        points++;
                    }
                }
        // The existing codec omits components no more than 1/16C from natural.
        helper.assertTrue(points > 0 && maximum <= .063,
                "The real same-tick checkpoint must preserve the published field; maximum=" + maximum);
        FHMain.LOGGER.info("THERMAL_CHECKPOINT_MATCHED_FIELD,gameTick={},points={},prunedPoints={},maximumDifferenceC={}",
                level.getGameTime(), points, pruned, maximum);
    }
}
