/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.TiledThermalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialPowerTimeline;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.NbtIo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Run after tiled_persistence in a separate JVM with the same saved world. */
@GameTestHolder("frostedheart_tiled_restart")
@PrefixGameTestTemplate(false)
public final class TiledThermalRestartGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 400)
    public static void aNewJvmRestoresActualDiskValuesAndReplaysPendingEnergyOnce(GameTestHelper helper) throws Exception {
        var directory = helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("tiled-checkpoint-values");
        var metadata = NbtIo.readCompressed(directory.resolve("metadata.nbt").toFile());
        long pid = ProcessHandle.current().pid(), started = java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        helper.assertTrue(metadata.getLong("writer_pid") != pid || metadata.getLong("writer_started") != started,
                "This gate requires a different JVM, not another engine in the writer process");
        var a = DormantChunkThermalState.decode(NbtIo.readCompressed(directory.resolve("left.nbt").toFile()), 12, 1);
        var b = DormantChunkThermalState.decode(NbtIo.readCompressed(directory.resolve("right.nbt").toFile()), 12, 1);
        var source = BlockPos.of(metadata.getLong("source"));
        long left = metadata.getLong("left"), right = metadata.getLong("right"), tick = helper.getLevel().getGameTime();
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(32 * 1024 * 1024);
        try (var banks = new SpatialEnergyBanks(budget, 8, 64, tick);
                var power = new SpatialPowerTimeline(budget, banks, 32, 64, tick);
                var field = new TiledThermalField(budget, profiles.signatures(), profiles.materials(),
                        profiles.tuning().airHeatCapacityJPerBlockK(), profiles.tuning().airMixingWPerBlockK(), 0, 16, tick);
                var checkpoints = new TiledThermalCheckpoint(budget, profiles)) {
            helper.assertTrue(field.admit(left, capture(helper, profiles, left, 1L << 3), 1L << 3, 0)
                    && field.admit(right, capture(helper, profiles, right, 1), 1, 0), "Capture the actual saved world's BlockStates");
            helper.assertTrue(checkpoints.restoreSection(left, a, field, tick, Double.POSITIVE_INFINITY)
                    && checkpoints.restoreSection(right, b, field, tick, Double.POSITIVE_INFINITY), "Restore values through the existing disk decoder");
            double accepted = field.totalEnthalpyJ();
            near(helper, metadata.getDouble("accepted_j"), accepted, "Restore Accepted without the writer's unfinished trial");
            // Match original-engine admission: both restored Sections become
            // visible in one zero-time publication before querying their interface.
            helper.assertTrue(field.begin(power.seal(tick)), "Prepare the restored geometry at its existing physical tick");
            long publishDeadline = System.nanoTime() + 10_000_000_000L;
            while (!field.advance(System.nanoTime() + 1_000_000L))
                helper.assertTrue(System.nanoTime() < publishDeadline, "Finish the zero-time restored publication");
            helper.assertTrue(field.commit(), "Publish both restored Sections without exchanging heat or consuming replay");
            near(helper, accepted, field.totalEnthalpyJ(), "Restored publication does not change total energy");
            long[] probes = metadata.getLongArray("probe_c"); var probe = new TiledThermalField.Sample();
            helper.assertTrue(probes.length == TiledThermalPersistenceGameTests.CHECKPOINT_PROBES.length,
                    "The first JVM must have saved the current fractional-gradient fixture");
            for (int index = 0; index < probes.length; index++) {
                var offset = TiledThermalPersistenceGameTests.CHECKPOINT_PROBES[index];
                helper.assertTrue(field.sampleAir(source.getX() + offset[0], source.getY() + offset[1], source.getZ() + offset[2], probe),
                        "The new JVM reads the same physical position from restored cell energy");
                near(helper, Double.longBitsToDouble(probes[index]), probe.temperatureC(), "The restored means reconstruct the same Accepted fractional queries");
            }
            var restored = checkpoints.pendingInputs(a.coldPending(), tick, Double.POSITIVE_INFINITY);
            for (int index = 0; index < restored.heat().size(); index++) {
                var heat = restored.heat();
                helper.assertTrue(heat.isAir(index) && power.tryReplay(new SpatialEnergyBanks.Address(heat.position(index), -1, heat.face(index)),
                        heat.fromTick(index), heat.toTick(index), heat.energyJ(index)), "Admit the disk's finite Air replay interval");
            }
            for (long target = tick + 20; target <= tick + 100; target += 20) {
                helper.assertTrue(field.begin(power.seal(target)), "Begin a restored numerical interval");
                long deadline = System.nanoTime() + 10_000_000_000L;
                while (!field.advance(System.nanoTime() + 1_000_000L)) helper.assertTrue(System.nanoTime() < deadline, "Finish the restored interval");
                helper.assertTrue(field.commit(), "Commit and acknowledge restored J");
            }
            near(helper, metadata.getDouble("pending_j"), power.replayedJ(), "Replay the archive once, including later passive intervals");
            near(helper, accepted + metadata.getDouble("pending_j"), field.totalEnthalpyJ(), "Accepted plus archived J equals final conserved energy");
            helper.assertTrue(power.sourceCount() == 0 && power.replayCount() == 0, "No offline source power or historical replay remains active");
            FHMain.LOGGER.info("THERMAL_TILED_RESTART_RESULT,writerPid={},readerPid={},acceptedJ={},replayedJ={},finalJ={},sameJvm=false,gradientSamples={},sourceX={}",
                    metadata.getLong("writer_pid"), pid, accepted, power.replayedJ(), field.totalEnthalpyJ(), probes.length, source.getX());
        }
        helper.assertTrue(budget.usedBytes() == 0, "New-JVM owners release");
        helper.succeed();
    }

    private static int[] capture(GameTestHelper helper, MinecraftThermalProfiles.Snapshot profiles, long section, long mask) {
        int[] signatures = new int[4096]; Arrays.fill(signatures, ThermalSignatureTable.UNRESOLVED);
        for (int brick = 0; brick < 64; brick++) if ((mask & 1L << brick) != 0) for (int cell = 0; cell < 64; cell++) {
            int local = com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout.pageBlock(brick, cell);
            var position = new BlockPos((SectionPos.x(section) << 4) + (local & 15), (SectionPos.y(section) << 4) + (local >>> 8),
                    (SectionPos.z(section) << 4) + (local >>> 4 & 15));
            helper.getLevel().getChunkAt(position);
            signatures[local] = profiles.states().signatureId(helper.getLevel().getBlockState(position));
            helper.assertTrue(profiles.signatures().isAir(signatures[local]),
                    "The saved Air fixture's actual world geometry was replaced at " + position);
        }
        return signatures;
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + "; expected=" + expected + ",actual=" + actual);
    }
}
