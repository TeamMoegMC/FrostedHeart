/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_heat_buffer")
@PrefixGameTestTemplate(false)
public final class SpatialHeatBufferGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void scopedCommitAndCancellationPreserveAnotherDomainsOriginalAndDeferredHeat(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1 << 20); var store = new SpatialHeatStore();
        var a = new SpatialHeatInput.Target(BlockPos.asLong(1, 64, 1), (byte) 2, -1, 0, 0, 0, 0, 0);
        var b = new SpatialHeatInput.Target(BlockPos.asLong(65, 64, 1), (byte) 2, -1, 0, 0, 0, 0, 0);
        try (var heat = new SpatialHeatBuffer(budget, 4, 16, 0)) {
            heat.add(new SpatialHeatBuffer.Address(a.position(), -1, 2), 50);
            heat.add(new SpatialHeatBuffer.Address(b.position(), -1, 2), 80);
            var initial = heat.seal(0, 20); initial.defer(0, 10); initial.defer(1, 30); initial.commit();
            store.receive(1, a, 20, 40, 100); store.receive(1, b, 20, 60, 200);
            SpatialHeatStore.RecordConsumer copy = (target, from, to, joules) -> helper.assertTrue(
                    heat.add(new SpatialHeatBuffer.Address(target.position(), -1, 2), joules), "Copy one scoped claim");
            var cancelled = store.claim(1, 20, 40, target -> target.equals(a), copy); cancelled.prepare(Long.MAX_VALUE);
            var trial = heat.seal(20, 40, position -> position == a.position());
            helper.assertTrue(trial.size == 1, "Only the selected domain borrows input slots");
            trial.defer(0, 999); trial.cancel(); cancelled.close(); store.advanceCleanup(32);
            near(helper, 300, store.pendingEnergyJ(), "Cancelling the selected domain retains both originals");
            var committed = store.claim(1, 20, 40, target -> target.equals(a), copy); committed.prepare(Long.MAX_VALUE);
            var batch = heat.seal(20, 40, position -> position == a.position()); batch.commit(); committed.accepted(); store.advanceCleanup(32);
            near(helper, 200, store.pendingEnergyJ(), "Committing A never consumes B's prefix");
            near(helper, 30, heat.committedPendingJoules(), "Committing A preserves B's independent deferred balance");
            var remaining = heat.seal(20, 60, position -> position == b.position());
            helper.assertTrue(remaining.size == 1 && remaining.pendingFromTick(0) == 0, "B retains the actual age of its own heat");
            near(helper, 30, remaining.pendingJoules(0), "B retains the same deferred J after another domain commits");
            remaining.cancel();
        } finally { store.clear(); }
        helper.assertTrue(budget.usedBytes() == 0, "Scoped input capacities release"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void cancellationAndSaveKeepOriginalClaimAndCommittedDebtSeparate(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1 << 20);
        var store = new SpatialHeatStore();
        var a = new SpatialHeatInput.Target(BlockPos.asLong(1, 64, 1), (byte) 2, -1, 0, 0, 0, 0, 0);
        var b = new SpatialHeatInput.Target(BlockPos.asLong(33, 64, 1), (byte) 2, -1, 0, 0, 0, 0, 0);
        long chunk = new net.minecraft.world.level.ChunkPos(0, 0).toLong();
        try (var heat = new SpatialHeatBuffer(budget, 4, 16, 0)) {
            SpatialHeatStore.RecordConsumer prepare = (target, from, to, joules) -> helper.assertTrue(
                    heat.add(new SpatialHeatBuffer.Address(target.position(), -1, target.face()), joules), "Prepare the Store claim");
            store.receive(1, a, 0, 20, 100);
            var first = store.claim(1, 0, 20, prepare); first.prepare(Long.MAX_VALUE);
            var initial = heat.seal(0, 20); initial.defer(0, 40); initial.commit(); first.accepted(); store.advanceCleanup(32);
            store.receive(1, a, 20, 40, 25); store.receive(1, b, 20, 40, 60);
            var claimed = store.claim(1, 20, 40, prepare); claimed.prepare(Long.MAX_VALUE);
            var trial = heat.seal(20, 40); trial.defer(0, 999);
            double[] saved = {0};
            store.collectChunk(chunk, (target, from, to, joules) -> saved[0] += joules);
            heat.committedPendingInChunk(chunk, (address, joules, since) -> {
                helper.assertTrue(since == 0, "Saved debt keeps its original interval"); saved[0] += joules;
            });
            near(helper, 65, saved[0], "SAVE sees 25J original input plus 40J committed debt, excluding trial output/copies");
            trial.cancel(); claimed.close(); store.advanceCleanup(32);
            near(helper, 85, store.pendingEnergyJ(), "Cancellation leaves all original input in the Store");
            near(helper, 40, heat.committedPendingJoules(), "Cancellation cannot publish the 999J trial balance");
            double exported = heat.exportCommittedPendingInChunk(chunk);
            store.retireChunk(chunk, (target, joules) -> saved[0] = joules);
            near(helper, 65, exported + saved[0], "Successful UNLOAD transfers original input and committed debt once");
            near(helper, 0, heat.exportCommittedPendingInChunk(chunk), "The same debt cannot be exported twice");
            near(helper, 60, store.pendingEnergyJ(), "Unrelated Chunk input remains available");
        } finally { store.clear(); }
        helper.assertTrue(budget.usedBytes() == 0, "Input storage releases"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void debtAgeAndPrepaidGrowthSurvivePartialCancellation(GameTestHelper helper) {
        var budget = new ThermalMemoryBudget(1 << 20);
        var a = new SpatialHeatBuffer.Address(BlockPos.asLong(1, 64, 1), -1, 2);
        var b = new SpatialHeatBuffer.Address(BlockPos.asLong(33, 64, 1), -1, 2);
        try (var heat = new SpatialHeatBuffer(budget, 1, 4, 0)) {
            helper.assertTrue(heat.add(a, 100) && heat.add(a, -40), "Signed input shares one canonical receiver");
            var initial = heat.seal(0, 20);
            near(helper, 60, initial.joules(0), "Signs are combined before applying material laws");
            initial.defer(0, 30); initial.commit();
            helper.assertTrue(heat.add(a, 25), "Prepare new input beside an older committed balance");
            long reserved = heat.reservedBytes();
            try (var pressure = budget.tryReserve(budget.limitBytes() - budget.usedBytes())) {
                helper.assertTrue(heat.prepareTargets(4) == 1 && heat.reservedBytes() == reserved,
                        "Optional cohort preparation retains the paid capacity when growth cannot fit");
                helper.assertTrue(pressure != null && !heat.add(b, 60) && heat.targetCount() == 1 && heat.reservedBytes() == reserved,
                        "Refused growth does not install a receiver or replace its borrowed storage");
            }
            heat.discardPreparation();
            var zeroTime = heat.seal(20, 20);
            helper.assertTrue(zeroTime.fromTick == 20 && zeroTime.toTick == 20 && zeroTime.pendingFromTick(0) == 0,
                    "Cancelling preparation keeps both Accepted time and oldest debt interval");
            near(helper, 0, zeroTime.joules(0), "The unconfirmed 25J copy was discarded");
            zeroTime.defer(0, 30); zeroTime.commit();
            heat.committedPendingInChunk(a.chunk(), (address, joules, since) -> helper.assertTrue(joules == 30 && since == 0,
                    "Zero-time geometry does not relabel old heating debt"));
            var settled = heat.seal(20, 40); settled.commit();
            helper.assertTrue(heat.targetCount() == 0 && !heat.hasRetainedInputInChunk(a.chunk()), "Consumed debt releases its spatial membership");
            heat.prepareInterval(40);
            helper.assertTrue(heat.add(b, 5), "A later address can reuse the released capacity");
            heat.pendingInChunk(b.chunk(), (address, joules, since) -> helper.assertTrue(joules == 5 && since == 40,
                    "The new address cannot inherit the retired debt's age"));
        }
        helper.assertTrue(budget.usedBytes() == 0, "Growth and retirement release their credit"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void fixedFieldMatchesPreviousInputAcrossSignsCancellationAndFreeze(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(16 << 20);
        var base = new BlockPos(557056, 192, 557056);
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            for (var origin : new BlockPos[] {base, base.east(16)}) helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            captured[x | z << 4 | y << 8] = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        }
        long[] positions = {base.offset(1, 1, 1).asLong(), base.offset(2, 1, 1).asLong(), base.offset(17, 1, 1).asLong()};
        long[] oldHandles = new long[3]; var addresses = new SpatialHeatBuffer.Address[3];
        double maximumC = 0, maximumJ = 0; int samples = 0;
        try (var old = new SpatialEnergyBanks(budget, 4, 16, 0); var heat = new SpatialHeatBuffer(budget, 4, 16, 0);
                var reference = field(budget, profiles, 0); var current = field(budget, profiles, 0)) {
            for (var origin : new BlockPos[] {base, base.east(16)}) helper.assertTrue(
                    reference.admit(SectionPos.asLong(origin), captured, 1, 0) && current.admit(SectionPos.asLong(origin), captured, 1, 0), "Capture identical real Air cells");
            for (int i = 0; i < 3; i++) {
                oldHandles[i] = old.retain(new SpatialEnergyBanks.Address(positions[i], -1, 2));
                addresses[i] = new SpatialHeatBuffer.Address(positions[i], -1, 2);
            }
            var a = new TiledThermalField.Sample(); var b = new TiledThermalField.Sample();
            for (int round = 1; round <= 40; round++) {
                if (round == 12) { reference.freeze(SectionPos.asLong(base)); current.freeze(SectionPos.asLong(base)); }
                if (round == 16) { reference.thaw(SectionPos.asLong(base), 0, Double.POSITIVE_INFINITY); current.thaw(SectionPos.asLong(base), 0, Double.POSITIVE_INFINITY); }
                double[] joules = {round % 3 == 0 ? -300 : 6400, -100, 800};
                for (int i = 0; i < 3; i++) {
                    old.add(oldHandles[i], joules[i] + 200); old.add(oldHandles[i], -200);
                    helper.assertTrue(heat.add(addresses[i], joules[i] + 200) && heat.add(addresses[i], -200), "Prepare identical signed input");
                }
                helper.assertTrue(reference.begin(old.seal(round * 20L)) && current.begin(heat.seal((round - 1) * 20L, round * 20L)), "Borrow the same physical interval");
                finish(reference); finish(current);
                if (round == 7 || round == 18) {
                    double accepted = current.totalEnthalpyJ();
                    reference.cancel(); current.cancel();
                    near(helper, accepted, current.totalEnthalpyJ(), "Completed private work cannot enter Accepted on cancellation");
                    for (int i = 0; i < 3; i++) helper.assertTrue(heat.add(addresses[i], joules[i]), "The Store would lend the original J again");
                    helper.assertTrue(reference.begin(old.seal(round * 20L)) && current.begin(heat.seal((round - 1) * 20L, round * 20L)), "Retry the uncommitted interval");
                    finish(reference); finish(current);
                }
                helper.assertTrue(reference.commit() && current.commit(), "Commit each interval's own input receipt");
                near(helper, old.acceptedPendingJoules(), heat.committedPendingJoules(), "Both transactions retain the same deferred J");
                for (var origin : new BlockPos[] {base, base.east(16)}) for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                    long position = origin.offset(x, y, z).asLong();
                    helper.assertTrue(reference.read(position, false, a) && current.read(position, false, b), "Compare the same canonical cell");
                    maximumJ = Math.max(maximumJ, Math.abs(a.enthalpyJ() - b.enthalpyJ()));
                    near(helper, a.enthalpyJ(), b.enthalpyJ(), "Changing input ownership preserves canonical energy");
                    helper.assertTrue(a.sampleTick() == b.sampleTick(), "Frozen and live sample clocks match");
                    for (double offset : new double[] {.125, .875}) {
                        helper.assertTrue(reference.sampleAir(origin.getX() + x + offset, origin.getY() + y + .25, origin.getZ() + z + .75, a)
                                && current.sampleAir(origin.getX() + x + offset, origin.getY() + y + .25, origin.getZ() + z + .75, b), "Compare actual fractional queries");
                        maximumC = Math.max(maximumC, Math.abs(a.temperatureC() - b.temperatureC())); samples++;
                        helper.assertTrue(Math.abs(a.temperatureC() - b.temperatureC()) <= 1e-9, "Input refactoring does not change the temperature algorithm");
                    }
                }
                near(helper, reference.externalEnergyJ(), current.externalEnergyJ(), "Geometry/freeze exchange has the same accounting");
            }
        }
        FHMain.LOGGER.info("THERMAL_INPUT_BUFFER_EQUIVALENCE,intervals=40,cancellations=2,freezeThaw=true,positionSamples={},maximumC={},maximumJ={}", samples, maximumC, maximumJ);
        helper.assertTrue(budget.usedBytes() == 0, "Both live numerical/control paths release"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void phaseDebtKeepsMaterialIdentityThroughZeroTimeCommit(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4 << 20);
        var ice = new BlockPos(557120, 192, 557056); helper.getLevel().setBlockAndUpdate(ice, Blocks.ICE.defaultBlockState());
        int signature = profiles.states().signatureId(helper.getLevel().getBlockState(ice));
        var law = profiles.materials().profileOrNull(profiles.signatures().materialProfileId(signature)).thermalLaw(); var edge = law.heating();
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED); captured[0] = signature;
        try (var heat = new SpatialHeatBuffer(budget, 4, 16, 0); var field = field(budget, profiles, 0)) {
            helper.assertTrue(field.admit(SectionPos.asLong(ice), captured, 1, edge.transitionTemperatureC() - 1), "Admit actual configured ice");
            var sample = new TiledThermalField.Sample(); field.read(ice.asLong(), true, sample);
            int generation = sample.materialGeneration(); double initial = sample.enthalpyJ();
            double energy = edge.targetEnthalpyJ() - initial + 1000;
            var address = new SpatialHeatBuffer.Address(ice.asLong(), generation);
            helper.assertTrue(heat.add(address, energy + 2000) && heat.add(address, -2000), "Net signed heat precedes the nonlinear phase boundary");
            helper.assertTrue(field.begin(heat.seal(0, 20)), "Begin the original material input"); finish(field); helper.assertTrue(field.commit(), "Commit latent heat and its excess together");
            near(helper, 1000, heat.committedPendingJoules(), "Only true excess remains deferred");
            var next = Block.BLOCK_STATE_REGISTRY.byId(edge.targetStateId()); helper.getLevel().setBlockAndUpdate(ice, next);
            helper.assertTrue(field.changeCell(ice.asLong(), profiles.states().signatureId(next), TiledThermalField.Change.PHASE), "Install the actual next phase");
            helper.assertTrue(field.begin(heat.seal(20, 20)), "Geometry injects only the already committed debt at the same tick"); finish(field); helper.assertTrue(field.commit(), "Commit the zero-time material handoff");
            helper.assertTrue(field.read(ice.asLong(), true, sample) && sample.materialGeneration() == generation, "Deferred input follows the physical body through its phase");
            near(helper, initial + energy, field.totalEnthalpyJ() + heat.committedPendingJoules() + field.externalEnergyJ(), "The handoff preserves latent and sensible heat once");
            helper.assertTrue(heat.committedPendingJoules() < 1000, "The new phase can consume the old validated debt");
            FHMain.LOGGER.info("THERMAL_INPUT_BUFFER_PHASE,generation={},savedDebtJ=1000,remainingDebtJ={},sampleTick={}", generation, heat.committedPendingJoules(), sample.sampleTick());
        }
        helper.assertTrue(budget.usedBytes() == 0, "Material and input owners release"); helper.succeed();
    }

    private static TiledThermalField field(ThermalMemoryBudget budget, MinecraftThermalProfiles.Snapshot profiles, long tick) {
        return new TiledThermalField(budget, profiles.signatures(), profiles.materials(), 1200, 96, 0, 4, tick);
    }
    private static void finish(TiledThermalField field) {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L)) if (System.nanoTime() > deadline) throw new AssertionError("Numerical step failed to yield/finish");
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= Math.max(1e-6, Math.abs(expected) * 1e-12), message + ": expected=" + expected + ",actual=" + actual);
    }
}
