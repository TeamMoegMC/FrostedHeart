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
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.TiledSectionInput;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real v6 disk values and world BlockState identity, independent of numerical catch-up. */
@GameTestHolder("frostedheart_tiled_persistence")
@PrefixGameTestTemplate(false)
public final class TiledThermalPersistenceGameTests {
    static final double[][] CHECKPOINT_PROBES = {{.125, .125, .125}, {.5, .5, .5}, {.875, .875, .875}};
    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 400)
    public static void newSourceAndSaveProceedWhileTheNumericalWriterIsPaused(GameTestHelper helper) throws Exception {
        var profiles = MinecraftThermalProfiles.prepare();
        for (boolean cancel : new boolean[] {false, true}) {
            var budget = new ThermalMemoryBudget(32 * 1024 * 1024);
            var base = new BlockPos(cancel ? 90176 : 90112, 192, 90112);
            var firstPosition = base.offset(1, 1, 1); var addedPosition = firstPosition.east(16);
            var first = new SpatialEnergyBanks.Address(firstPosition.asLong(), -1, 2);
            var added = new SpatialEnergyBanks.Address(addedPosition.asLong(), -1, 2);
            int[] firstAir = capturedAir(helper, profiles, base, 1), addedAir = capturedAir(helper, profiles, base.east(16), 1);
            try (var banks = new SpatialEnergyBanks(budget, 4, 32, 0);
                    var power = new SpatialPowerTimeline(budget, banks, 8, 8, 0);
                    var field = field(budget, profiles, 0);
                    var checkpoints = new TiledThermalCheckpoint(budget, profiles)) {
                helper.assertTrue(field.admit(SectionPos.asLong(firstPosition), firstAir, 1, 0)
                                && field.admit(SectionPos.asLong(addedPosition), addedAir, 1, 0)
                                && power.tryRegister(1, 1, new SpatialEnergyBanks.Address[] {first}, new double[] {1}, 1000, true, 0),
                        "Capture both actual world targets and start the original producer");
                helper.assertTrue(field.begin(power.seal(20)), "Claim the first source interval");
                var started = new java.util.concurrent.CountDownLatch(1); var resume = new java.util.concurrent.CountDownLatch(1);
                var finished = new java.util.concurrent.CompletableFuture<Void>();
                helper.assertTrue(ThermalWorkerPool.shared().tryExecute(() -> {
                    started.countDown();
                    try {
                        if (!resume.await(15, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("New-source test did not resume its writer");
                        while (!field.advance(System.nanoTime() + 1_000_000L)) { }
                        finished.complete(null);
                    } catch (Throwable failure) { finished.completeExceptionally(failure); }
                }), "Start the actual shared-pool numerical writer");
                try {
                    helper.assertTrue(started.await(10, java.util.concurrent.TimeUnit.SECONDS), "The writer reached its controlled pause");
                    long bankCredit = banks.reservedBytes();
                    helper.assertTrue(power.tryRegister(2, 1, new SpatialEnergyBanks.Address[] {added}, new double[] {1}, 600, true, 20)
                                    && banks.reservedBytes() == bankCredit,
                            "The new source claims prepaid capacity while the old input range remains borrowed");
                    power.update(1, 1, 0, false, 30); power.integrate(40);
                    near(helper, 0, field.totalEnthalpyJ(), "Pending production never changes the field's Accepted state");
                    try (var left = checkpoints.tryCapture(first.chunk(), 12, 1, 40, field, banks, power);
                            var right = checkpoints.tryCapture(added.chunk(), 12, 1, 40, field, banks, power)) {
                        helper.assertTrue(left != null && right != null, "Capture both Chunks without resuming the writer");
                        for (var saved : new TiledThermalCheckpoint.Payload[] {left, right}) {
                            var encoded = new CompoundTag(); saved.encode(encoded);
                            var restored = DormantChunkThermalState.decode(encoded, 12, 1);
                            var pending = checkpoints.pendingInputs(restored.coldPending(), 40, Double.POSITIVE_INFINITY);
                            double joules = 0;
                            for (int index = 0; index < pending.heat().size(); index++) joules += pending.heat().energyJ(index);
                            near(helper, saved.chunk == first.chunk() ? 1500 : 600, joules,
                                    "The existing v6 capture includes the old claimed bank and new source's live bank exactly once");
                        }
                    }
                } finally { resume.countDown(); finished.get(15, java.util.concurrent.TimeUnit.SECONDS); }
                if (cancel) {
                    field.cancel();
                    near(helper, 0, field.totalEnthalpyJ(), "Cancellation preserves the earlier Accepted field");
                } else {
                    helper.assertTrue(field.commit(), "Commit only the original sealed source interval");
                    near(helper, 1000, field.totalEnthalpyJ(), "The new source cannot enter the earlier numerical interval");
                }
                helper.assertTrue(field.begin(power.seal(40)), "Seal the next complete producer interval");
                finishStep(helper, field);
                helper.assertTrue(field.commit(), "Commit the next field and both input receipts");
                near(helper, 2100, field.totalEnthalpyJ(), "Commit and cancellation both deliver total declared J once");
                near(helper, 2100, power.producedJ(), "The source ledger retains exact production across registration and extinction");
                near(helper, 0, power.unavailableReceiverJ(), "Admitted source input never loses its receiver");
                FHMain.LOGGER.info("THERMAL_NEW_SOURCE_DURING_SOLVE,cancel={},newSourceAdmittedWhilePaused=true,saveAcceptedJ=0,savePendingJ=2100,finalJ={},receiverGrowth=false",
                        cancel, field.totalEnthalpyJ());
            }
            helper.assertTrue(budget.usedBytes() == 0, "Source, field, captured values and banks release");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 200)
    public static void pendingReplacementSurvivesWithoutAnyWorldReferenceInTheInbox(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        var base = new BlockPos(25472, 192, 24576); var matter = base.offset(1, 1, 1);
        long section = SectionPos.asLong(matter);
        int[] geometry = capturedAir(helper, profiles, base, 1), states = new int[4096];
        helper.getLevel().setBlockAndUpdate(matter, Blocks.ICE.defaultBlockState());
        int local = (matter.getX() & 15) | (matter.getZ() & 15) << 4 | (matter.getY() & 15) << 8;
        geometry[local] = profiles.states().signatureId(helper.getLevel().getBlockState(matter));
        var world = helper.getLevel().getChunkAt(matter).getSection(helper.getLevel().getSectionIndex(matter.getY()));
        for (int cell = 0; cell < 4096; cell++) states[cell] = net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY.getId(world.getBlockState(cell & 15, cell >>> 8, cell >>> 4 & 15));
        var credit = budget.tryReserve(TiledSectionInput.reservedBytes(1));
        helper.assertTrue(credit != null, "Pre-admit sparse world-state storage alongside its mutation bits");
        var inbox = new TiledSectionInput(section, 1, states, null, credit);
        try (var banks = new SpatialEnergyBanks(budget, 8, 32, 0); var field = field(budget, profiles, 0)) {
            helper.assertTrue(field.admit(section, geometry, 1, -10), "Capture actual ice at a declared cold bath");
            var sample = new TiledThermalField.Sample(); field.read(matter.asLong(), true, sample);
            int generation = sample.materialGeneration();
            long input = banks.retain(new SpatialEnergyBanks.Address(matter.asLong(), generation)); banks.add(input, 1000);
            step(helper, field, banks, 20); field.read(matter.asLong(), true, sample); double warm = sample.enthalpyJ();
            var before = helper.getLevel().getBlockState(matter);
            helper.getLevel().setBlockAndUpdate(matter, Blocks.AIR.defaultBlockState());
            inbox.blockStateChanged(matter.getX() & 15, matter.getY() & 15, matter.getZ() & 15, before, helper.getLevel().getBlockState(matter));
            before = helper.getLevel().getBlockState(matter);
            helper.getLevel().setBlockAndUpdate(matter, Blocks.ICE.defaultBlockState());
            inbox.blockStateChanged(matter.getX() & 15, matter.getY() & 15, matter.getZ() & 15, before, helper.getLevel().getBlockState(matter));
            inbox.retain(world);
            var values = new TiledThermalField.SectionSnapshot(); field.snapshot(section, values);
            inbox.project(null, profiles, field, values);
            var law = profiles.materials().profileOrNull(profiles.signatures().materialProfileId(geometry[local])).thermalLaw();
            near(helper, law.enthalpyAtTemperature(-10), values.enthalpyJ[local], "A value-only save can project a pending same-state replacement after the world is unavailable");
            field.read(matter.asLong(), true, sample);
            near(helper, warm, sample.enthalpyJ(), "Projecting a save did not mutate Accepted or Working");
            helper.assertTrue(inbox.capture(null, profiles, field) == 1 && field.currentMaterialStateId(matter.asLong(), generation) < 0,
                    "The next epoch installs cached replacement ownership without reading a Chunk");
        } finally { inbox.invalidate(); }
        helper.assertTrue(budget.usedBytes() == 0, "Value inbox clears arrays before returning its own reservation");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 400)
    public static void pausedWriterSavesAcceptedAndBothUnconfirmedBanks(GameTestHelper helper) throws Exception {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(32 * 1024 * 1024);
        // Keep the cross-JVM world geometry outside the large reference room's construction range.
        var base = new BlockPos(86016, 192, 86016);
        var source = base.offset(15, 1, 1);
        long left = SectionPos.asLong(source), right = SectionPos.asLong(source.east());
        int[] first = capturedAir(helper, profiles, base.offset(12, 0, 0), 1L << 3);
        int[] second = capturedAir(helper, profiles, base.offset(16, 0, 0), 1);
        try (var banks = new SpatialEnergyBanks(budget, 8, 64, 0);
                var field = field(budget, profiles, 0);
                var checkpoints = new TiledThermalCheckpoint(budget, profiles)) {
            helper.assertTrue(field.admit(left, first, 1L << 3, 0) && field.admit(right, second, 1, 0), "Admit actual Air across a Chunk boundary");
            long receiver = banks.retain(new SpatialEnergyBanks.Address(source.asLong(), -1, 2));
            banks.add(receiver, 6400); step(helper, field, banks, 20);
            double committed = field.totalEnthalpyJ();
            banks.add(receiver, 3200);
            helper.assertTrue(field.begin(banks.seal(40)), "Claim the next input but pause before executing any numerical work");
            banks.add(receiver, 1600);
            var started = new java.util.concurrent.CountDownLatch(1);
            var resume = new java.util.concurrent.CountDownLatch(1);
            var finished = new java.util.concurrent.CompletableFuture<Void>();
            helper.assertTrue(ThermalWorkerPool.shared().tryExecute(() -> {
                started.countDown();
                try {
                    if (!resume.await(15, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Checkpoint test did not resume its writer");
                    while (!field.advance(System.nanoTime() + 1_000_000L)) { }
                    finished.complete(null);
                } catch (Throwable failure) { finished.completeExceptionally(failure); }
            }), "Start a real shared-pool writer with a bounded test pause");
            helper.assertTrue(started.await(10, java.util.concurrent.TimeUnit.SECONDS), "The numerical writer actually reached its pause");
            try (var a = checkpoints.tryCapture(new ChunkPos(source).toLong(), 12, 1, 60, field, banks);
                    var b = checkpoints.tryCapture(new ChunkPos(source.east()).toLong(), 12, 1, 60, field, banks)) {
                helper.assertTrue(a != null && b != null, "Pre-admitted checkpoint values do not wait for the paused numerical writer");
                var tagA = new CompoundTag(); var tagB = new CompoundTag(); a.encode(tagA); b.encode(tagB);
                helper.assertTrue(tagA.getCompound("FrostedHeartThermal").getInt("version") == 6, "Use the real existing v6 reader");
                var directory = helper.getLevel().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("tiled-checkpoint-values");
                java.nio.file.Files.createDirectories(directory);
                NbtIo.writeCompressed(tagA, directory.resolve("left.nbt").toFile());
                NbtIo.writeCompressed(tagB, directory.resolve("right.nbt").toFile());
                var metadata = new CompoundTag();
                metadata.putLong("writer_pid", ProcessHandle.current().pid());
                metadata.putLong("writer_started", java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime());
                metadata.putLong("source", source.asLong()); metadata.putLong("left", left); metadata.putLong("right", right);
                metadata.putDouble("accepted_j", committed); metadata.putDouble("pending_j", 4800);
                long[] probes = new long[CHECKPOINT_PROBES.length]; var probe = new TiledThermalField.Sample();
                for (int index = 0; index < probes.length; index++) {
                    var offset = CHECKPOINT_PROBES[index];
                    helper.assertTrue(field.sampleAir(source.getX() + offset[0], source.getY() + offset[1], source.getZ() + offset[2], probe),
                            "Read Accepted gradients while the numerical writer is paused");
                    probes[index] = Double.doubleToLongBits(probe.temperatureC());
                }
                helper.assertTrue(Math.abs(Double.longBitsToDouble(probes[0]) - Double.longBitsToDouble(probes[2])) > 1e-4,
                        "The restart fixture must preserve a spatial gradient, not a uniform cell value");
                metadata.putLongArray("probe_c", probes);
                NbtIo.writeCompressed(metadata, directory.resolve("metadata.nbt").toFile());
                var loadedA = DormantChunkThermalState.decode(NbtIo.readCompressed(directory.resolve("left.nbt").toFile()), 12, 1);
                var loadedB = DormantChunkThermalState.decode(NbtIo.readCompressed(directory.resolve("right.nbt").toFile()), 12, 1);
                var pending = checkpoints.pendingInputs(loadedA.coldPending(), 60, Double.POSITIVE_INFINITY);
                double pendingJ = 0;
                for (int index = 0; index < pending.heat().size(); index++) pendingJ += pending.heat().energyJ(index);
                near(helper, 4800, pendingJ, "Disk contains the claimed and later unclaimed input once");
                helper.assertTrue(field.acceptedTick() == 20 && banks.hasSealedBank(), "Saving neither runs nor acknowledges numerical work");
                try (var restored = field(budget, profiles, 60)) {
                    helper.assertTrue(restored.admit(left, first, 1L << 3, 0) && restored.admit(right, second, 1, 0), "Recreate current world geometry");
                    helper.assertTrue(checkpoints.restoreSection(left, loadedA, restored, 60, Double.POSITIVE_INFINITY)
                            && checkpoints.restoreSection(right, loadedB, restored, 60, Double.POSITIVE_INFINITY), "Read the disk values through v6 import");
                    near(helper, committed, restored.totalEnthalpyJ(), "The paused trial is absent from the restored Accepted field");
                }
                FHMain.LOGGER.info("THERMAL_TILED_CHECKPOINT_RESULT,acceptedTick={},saveTick=60,acceptedJ={},pendingJ={},workerAdvancedDuringSave=false,format=6",
                        field.acceptedTick(), committed, pendingJ);
            } finally { resume.countDown(); finished.get(15, java.util.concurrent.TimeUnit.SECONDS); }
            field.cancel();
        }
        helper.assertTrue(budget.usedBytes() == 0, "Both checkpoints, scratch and field/input owners release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 200)
    public static void checkpointIncludesUndeliveredColdReplayAsWellAsBothBanks(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(32 * 1024 * 1024);
        var base = new BlockPos(24704, 192, 24576);
        var source = base.offset(1, 1, 1);
        int[] values = capturedAir(helper, profiles, base, 1);
        var target = new SpatialEnergyBanks.Address(source.asLong(), -1, 2);
        try (var banks = new SpatialEnergyBanks(budget, 4, 32, 0);
                var power = new SpatialPowerTimeline(budget, banks, 32, 64, 0);
                var field = field(budget, profiles, 0);
                var checkpoints = new TiledThermalCheckpoint(budget, profiles)) {
            helper.assertTrue(field.admit(SectionPos.asLong(source), values, 1, 0), "Admit actual captured Air");
            helper.assertTrue(power.tryReplay(target, 0, 80, 4000), "Schedule an already produced cold input over four seconds");
            helper.assertTrue(field.begin(power.seal(20)), "Claim the first quarter without consuming it");
            try (var saved = checkpoints.tryCapture(target.chunk(), 12, 1, 40, field, banks, power)) {
                helper.assertTrue(saved != null, "Capture input ownership while the numerical epoch is paused");
                var restored = checkpoints.pendingInputs(saved.state().coldPending(), 40, Double.POSITIVE_INFINITY);
                double energy = 0;
                for (int index = 0; index < restored.heat().size(); index++) energy += restored.heat().energyJ(index);
                near(helper, 4000, energy, "The claimed quarter, new filling quarter and still scheduled half are saved once");
                near(helper, 0, power.producedJ(), "An archive never invents live source production");
            }
            field.cancel();
        }
        helper.assertTrue(budget.usedBytes() == 0, "Replay and save owners release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 400)
    public static void sameStateReplacementBeforeCommitDoesNotInheritOldLatentHeatOrInput(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(32 * 1024 * 1024);
        var position = new BlockPos(24640, 192, 24576);
        helper.getLevel().setBlockAndUpdate(position, Blocks.ICE.defaultBlockState());
        int signature = profiles.states().signatureId(helper.getLevel().getBlockState(position));
        var law = profiles.materials().profileOrNull(profiles.signatures().materialProfileId(signature)).thermalLaw();
        double ambient = law.heating().transitionTemperatureC() - 1;
        int[] values = new int[4096]; Arrays.fill(values, ThermalSignatureTable.UNRESOLVED); values[0] = signature;
        long section = SectionPos.asLong(position), chunk = new ChunkPos(position).toLong();
        try (var banks = new SpatialEnergyBanks(budget, 4, 32, 0);
                var field = field(budget, profiles, 0);
                var checkpoints = new TiledThermalCheckpoint(budget, profiles)) {
            helper.assertTrue(field.admit(section, values, 1, ambient), "Capture actual ice");
            var sample = new TiledThermalField.Sample(); field.read(position.asLong(), true, sample);
            int oldGeneration = sample.materialGeneration();
            long receiver = banks.retain(new SpatialEnergyBanks.Address(position.asLong(), oldGeneration));
            double target = (law.heating().sourceEnthalpyJ() + law.heating().targetEnthalpyJ()) / 2;
            banks.add(receiver, target - sample.enthalpyJ()); step(helper, field, banks, 20);
            field.read(position.asLong(), true, sample);
            near(helper, target, sample.enthalpyJ(), "Reach actual latent progress before the replacement");
            banks.add(receiver, 1234);
            helper.getLevel().setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(position, Blocks.ICE.defaultBlockState());
            helper.assertTrue(field.changeCell(position.asLong(), signature, TiledThermalField.Change.REPLACEMENT), "Capture an actual same-state removal and replacement");
            try (var saved = checkpoints.tryCapture(chunk, 12, 1, 20, field, banks)) {
                helper.assertTrue(saved != null, "Capture before numerical geometry commit");
                var body = saved.state().materials(12);
                near(helper, law.enthalpyAtTemperature(ambient), body.enthalpyJ(body.find(0)), "Saved new matter starts at ambient rather than inheriting old latent H");
                near(helper, 1234, saved.removedMatterInputJ, "Old-generation pending input is explicitly settled to removed matter");
                helper.assertTrue(saved.state().coldPending().empty(), "The old input is not replayed into the replacement ice");
            }
            step(helper, field, banks, 40);
            field.read(position.asLong(), true, sample);
            helper.assertTrue(sample.materialGeneration() != oldGeneration, "The replacement has a new incarnation even though its BlockState is identical");
            near(helper, law.enthalpyAtTemperature(ambient), sample.enthalpyJ(), "Actual commit agrees with saved material initialization");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Replacement capture owners release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 200)
    public static void phaseRemovalSettlesMatterWithoutDumpingItsLatentHeatIntoAir(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        var position = new BlockPos(24672, 192, 24576);
        helper.getLevel().setBlockAndUpdate(position, Blocks.ICE.defaultBlockState());
        int signature = profiles.states().signatureId(helper.getLevel().getBlockState(position));
        var law = profiles.materials().profileOrNull(profiles.signatures().materialProfileId(signature)).thermalLaw();
        int[] values = new int[4096]; Arrays.fill(values, ThermalSignatureTable.UNRESOLVED); values[0] = signature;
        try (var banks = new SpatialEnergyBanks(budget, 4, 32, 0); var field = field(budget, profiles, 0)) {
            helper.assertTrue(field.admit(SectionPos.asLong(position), values, 1, 0), "Admit one material body");
            var sample = new TiledThermalField.Sample(); field.read(position.asLong(), true, sample);
            long receiver = banks.retain(new SpatialEnergyBanks.Address(position.asLong(), sample.materialGeneration()));
            banks.add(receiver, law.heating().targetEnthalpyJ() - sample.enthalpyJ()); step(helper, field, banks, 20);
            double before = field.totalEnthalpyJ() + field.externalEnergyJ();
            helper.getLevel().setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
            helper.assertTrue(field.changeCell(position.asLong(), profiles.states().signatureId(helper.getLevel().getBlockState(position)),
                    TiledThermalField.Change.PHASE), "A world phase effect removed material from this position");
            step(helper, field, banks, 40);
            helper.assertTrue(field.read(position.asLong(), false, sample), "The resulting position contains actual Air");
            near(helper, 0, sample.temperatureC(), "Latent energy leaves with the removed matter instead of becoming hot Air");
            near(helper, before, field.totalEnthalpyJ() + field.externalEnergyJ(), "Matter removal remains in the explicit energy balance");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Removed matter leaves no owner");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_persistence", timeoutTicks = 300)
    public static void frozenWritebackDataKeepsItsClockWhileAnAdjacentRoomContinues(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(32 * 1024 * 1024);
        var base = new BlockPos(24768, 192, 24576);
        var a = base.offset(15, 1, 1); var b = a.east();
        long left = SectionPos.asLong(a), right = SectionPos.asLong(b);
        var first = capturedAir(helper, profiles, base.offset(12, 0, 0), 1L << 3);
        var second = capturedAir(helper, profiles, base.offset(16, 0, 0), 1);
        try (var banks = new SpatialEnergyBanks(budget, 8, 64, 0); var field = field(budget, profiles, 0)) {
            helper.assertTrue(field.admit(left, first, 1L << 3, 0) && field.admit(right, second, 1, 0), "Admit adjacent actual Sections");
            long heatB = banks.retain(new SpatialEnergyBanks.Address(b.asLong(), -1, 2));
            banks.add(heatB, 6400); step(helper, field, banks, 20); banks.release(heatB);
            var sample = new TiledThermalField.Sample(); field.read(b.asLong(), false, sample);
            double oldB = sample.temperatureC();
            helper.assertTrue(oldB > 0 && field.freeze(right), "Turn completed data into a writeback buffer without a world or Chunk reference");
            long heatA = banks.retain(new SpatialEnergyBanks.Address(a.asLong(), -1, 2));
            banks.add(heatA, 12800); step(helper, field, banks, 40);
            field.read(b.asLong(), false, sample);
            near(helper, oldB, sample.temperatureC(), "Neither face exchange nor projected source input mutates frozen data");
            helper.assertTrue(sample.sampleTick() == 20, "Frozen data does not acquire a fake fresh clock");
            field.read(a.asLong(), false, sample);
            helper.assertTrue(sample.sampleTick() == 40, "An adjacent active room continues while storage owns the frozen values");
            double balance = field.totalEnthalpyJ() + field.externalEnergyJ();
            helper.assertTrue(field.thaw(right, 0, 1), "Re-enter using the already reserved working-frame capacity");
            field.read(b.asLong(), false, sample);
            near(helper, oldB * .5, sample.temperatureC(), "Dormant cell energy cools once; rejoining does not transfer energy before a numerical step");
            helper.assertTrue(sample.sampleTick() == 40, "Re-entry reports the actual projected time");
            near(helper, balance, field.totalEnthalpyJ() + field.externalEnergyJ(), "Dormant loss remains in the explicit environmental balance");
            var wall = a.above(); helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
            helper.assertTrue(field.changeCell(wall.asLong(), profiles.states().signatureId(Blocks.STONE.defaultBlockState()), TiledThermalField.Change.REPLACEMENT),
                    "Capture a wall immediately after the cell rejoins");
            step(helper, field, banks, 40);
            near(helper, balance, field.totalEnthalpyJ() + field.externalEnergyJ(), "Immediate local replacement preserves canonical cell energy");
            banks.add(heatA, 1600); step(helper, field, banks, 60);
            field.read(b.asLong(), false, sample);
            helper.assertTrue(sample.sampleTick() == 60 && sample.temperatureC() > oldB * .5, "Re-entry restores real cross-Section transport");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Frozen and restored storage owners release");
        helper.succeed();
    }

    private static int[] capturedAir(GameTestHelper helper, MinecraftThermalProfiles.Snapshot profiles, BlockPos brick, long mask) {
        int[] values = new int[4096]; Arrays.fill(values, ThermalSignatureTable.UNRESOLVED);
        for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) for (int x = 0; x < 4; x++) {
            var p = brick.offset(x, y, z); helper.getLevel().setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            values[(p.getX() & 15) | (p.getZ() & 15) << 4 | (p.getY() & 15) << 8] = profiles.states().signatureId(helper.getLevel().getBlockState(p));
        }
        return values;
    }

    private static TiledThermalField field(ThermalMemoryBudget budget, MinecraftThermalProfiles.Snapshot profiles, long tick) {
        return new TiledThermalField(budget, profiles.signatures(), profiles.materials(), profiles.tuning().airHeatCapacityJPerBlockK(),
                profiles.tuning().airMixingWPerBlockK(), 0, 16, tick);
    }

    private static void step(GameTestHelper helper, TiledThermalField field, SpatialEnergyBanks banks, long tick) {
        helper.assertTrue(field.begin(banks.seal(tick)), "Begin one bounded numerical step");
        finishStep(helper, field);
        helper.assertTrue(field.commit(), "Publish and acknowledge the input together");
    }

    private static void finishStep(GameTestHelper helper, TiledThermalField field) {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L)) helper.assertTrue(System.nanoTime() < deadline, "Complete numerical work");
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + "; expected=" + expected + ", actual=" + actual);
    }
}
