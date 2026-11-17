/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Query work stays bounded, and readers see actual Accepted/visibility changes. */
@GameTestHolder("frostedheart_tiled_queries")
@PrefixGameTestTemplate(false)
public final class TiledThermalQueryGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void oneTransactionForTheSameIntervalMatchesIndependentDomainCommits(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(16L << 20);
        var base = new BlockPos(276480, 192, 274432); long a = SectionPos.asLong(base), b = SectionPos.asLong(base.east(64));
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) captured[x | z << 4 | y << 8] = air;
        var first = it.unimi.dsi.fastutil.longs.LongSets.singleton(a); var second = it.unimi.dsi.fastutil.longs.LongSets.singleton(b);
        var both = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {a, b});
        double maximum = 0; int samples = 0;
        try (var reference = field(budget, profiles, 2); var batched = field(budget, profiles, 2);
                var inputs = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer(budget, 4, 16, 0);
                var combined = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer(budget, 4, 16, 0)) {
            for (var field : new TiledThermalField[] {reference, batched})
                helper.assertTrue(field.admit(a, captured, 1, 0) && field.admit(b, captured, 1, 0), "Capture two disconnected identical domains");
            step(helper, reference, inputs, 0, 0, both); step(helper, batched, combined, 0, 0, both);
            var left = new TiledThermalField.Sample(); var right = new TiledThermalField.Sample();
            for (int interval = 0; interval < 20; interval++) {
                var firstInput = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer.Address(base.offset(1, 1, 1).asLong(), -1);
                var secondInput = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer.Address(base.offset(65, 1, 1).asLong(), -1);
                for (var heat : new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer[] {inputs, combined}) {
                    helper.assertTrue(heat.add(firstInput, 6400) && heat.add(secondInput, -300), "Borrow the same signed source input");
                }
                long from = interval * 20L, through = from + 20;
                step(helper, reference, inputs, from, through, first); step(helper, reference, inputs, from, through, second);
                step(helper, batched, combined, from, through, both);
                for (var origin : new BlockPos[] {base, base.east(64)}) for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                    helper.assertTrue(reference.sampleAir(origin.getX() + x + .125, origin.getY() + y + .5, origin.getZ() + z + .875, left)
                            && batched.sampleAir(origin.getX() + x + .125, origin.getY() + y + .5, origin.getZ() + z + .875, right)
                            && left.sampleTick() == through && right.sampleTick() == through, "Compare actual fractional samples at the same physical time");
                    maximum = Math.max(maximum, Math.abs(left.temperatureC() - right.temperatureC())); samples++;
                    near(helper, left.temperatureC(), right.temperatureC(), "Transaction batching preserves the independent physical solution");
                }
                near(helper, reference.totalEnthalpyJ() + reference.externalEnergyJ(), batched.totalEnthalpyJ() + batched.externalEnergyJ(),
                        "Batching preserves the complete signed energy balance");
            }
        }
        FHMain.LOGGER.info("THERMAL_SAME_INTERVAL_BATCH,intervals=20,domains=2,samples={},maximumC={}", samples, maximum);
        helper.assertTrue(budget.usedBytes() == 0, "Both numerical paths release their capacities"); helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void geometryMembershipUsesCommittedTileTimesAcrossIndependentProgressMergeAndSplit(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(8L << 20);
        var base = new BlockPos(275456, 192, 274432);
        long a = SectionPos.asLong(base), bridge = SectionPos.asLong(base.east(16)), b = SectionPos.asLong(base.east(32));
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) captured[x | z << 4 | y << 8] = air;
        var first = it.unimi.dsi.fastutil.longs.LongSets.singleton(a);
        var second = it.unimi.dsi.fastutil.longs.LongSets.singleton(b);
        var both = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {a, b});
        try (var heat = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer(budget, 4, 16, 0);
                var field = field(budget, profiles, 3)) {
            helper.assertTrue(field.admit(a, captured, 1, 10) && field.admit(b, captured, 1, 20), "Capture separated actual frames");
            step(helper, field, heat, 0, 0, both);
            step(helper, field, heat, 0, 20, first); step(helper, field, heat, 0, 40, second);
            helper.assertTrue(field.domains().size() == 2 && field.sectionSampleTick(a) == 20 && field.sectionSampleTick(b) == 40,
                    "Disconnected geometry has different actual numerical times");
            var invalid = heat.seal(20, 40);
            helper.assertTrue(!field.begin(invalid, both), "An unequal connected transaction cannot mislabel the ahead receiver"); invalid.cancel();
            step(helper, field, heat, 20, 40, first);
            helper.assertTrue(field.aligned(both) && field.atBoundary(both, 40), "Both recipients genuinely reach the geometry event");
            helper.assertTrue(field.admit(bridge, captured, 1, 15, 40, null), "Admit a connection at the reached physical cut");
            var joined = field.domains(); helper.assertTrue(joined.size() == 1, "Geometry derives one domain without a new clock");
            step(helper, field, heat, 40, 40, joined.get(0).sections()); step(helper, field, heat, 40, 60, joined.get(0).sections());
            helper.assertTrue(field.sectionSampleTick(a) == 60 && field.sectionSampleTick(b) == 60 && field.sectionSampleTick(bridge) == 60,
                    "Connected captured values commit the same real interval");
            helper.assertTrue(field.retire(bridge) && field.domains().size() == 2, "Retirement splits membership while preserving committed frames");
            step(helper, field, heat, 60, 80, first); step(helper, field, heat, 60, 100, second);
            helper.assertTrue(field.sectionSampleTick(a) == 80 && field.sectionSampleTick(b) == 100,
                    "The resulting domains continue directly from their own committed times");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Membership and numerical capacities release"); helper.succeed();
    }
    private static void step(GameTestHelper helper, TiledThermalField field,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer heat, long from, long to,
            it.unimi.dsi.fastutil.longs.LongSet sections) {
        helper.assertTrue(field.begin(heat.seal(from, to, position -> sections.contains(SectionPos.asLong(
                BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4))), sections), "Borrow an interval from its actual recipients");
        while (!field.advance(System.nanoTime() + 1_000_000L)) { }
        helper.assertTrue(field.commit(), "Commit values and their own physical time together");
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void distinctPositionChurnStaysBoundedAndRestoredValuesReplaceOldReads(GameTestHelper helper) throws Exception {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4L << 20);
        var base = new BlockPos(266240, 192, 266240); long section = SectionPos.asLong(base);
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        for (int x = 0; x < 16; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            captured[x | z << 4 | y << 8] = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        }
        try (var field = field(budget, profiles, 1)) {
            helper.assertTrue(field.admit(section, captured, 15, 0), "Admit 256 actual Air cells");
            double[] h = new double[4096]; byte[] branches = new byte[4096];
            for (int cell = 0; cell < 4096; cell++) if (profiles.signatures().isAir(captured[cell])) h[cell] = 10 * field.airCapacityJPerK();
            helper.assertTrue(field.restoreValues(section, h, branches), "Install uniform initial energy");
            long reserved = budget.usedBytes(); var sample = new TiledThermalField.Sample();
            for (int query = 0; query < 16384; query++) {
                int cell = query & 255, x = cell & 15, y = cell >>> 6, z = cell >>> 4 & 3;
                helper.assertTrue(field.sampleAir(base.getX() + x + .25, base.getY() + y + .5, base.getZ() + z + .75, sample), "Read every changing actual position");
                near(helper, 10, sample.temperatureC(), "Position churn cannot alias another value");
            }
            var cache = cache(field);
            helper.assertTrue(cache == null || cache.size() <= 128, "Derived query storage cannot grow with query history");
            helper.assertTrue(budget.usedBytes() == reserved, "Position churn creates no additional owned storage");
            for (int cell = 0; cell < h.length; cell++) h[cell] *= 2;
            helper.assertTrue(field.restoreValues(section, h, branches)
                            && field.sampleAir(base.getX() + 15.25, base.getY() + 3.5, base.getZ() + 3.75, sample), "Read a recently queried cell after restoring new Accepted energy");
            near(helper, 20, sample.temperatureC(), "An unchanged world address cannot return old query data after restoration");
            FHMain.LOGGER.info("THERMAL_TILED_QUERY_CHURN,positions=256,queries=16384,queryEntries={},reservationGrowth=0,restoreInvalidates=true", cache == null ? 0 : cache.size());
        }
        helper.assertTrue(budget.usedBytes() == 0, "Queries release the field's owned storage"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void freezingThawingAndRetiringANeighbourChangeAlreadyQueriedPositions(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4L << 20);
        var base = new BlockPos(270336, 192, 270336); long left = SectionPos.asLong(base), right = SectionPos.asLong(base.east(16));
        int[] a = new int[4096], b = new int[4096]; Arrays.fill(a, ThermalSignatureTable.UNRESOLVED); Arrays.fill(b, ThermalSignatureTable.UNRESOLVED);
        for (int x = 12; x < 20; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            (x < 16 ? a : b)[(x & 15) | z << 4 | y << 8] = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        }
        try (var banks = new SpatialEnergyBanks(budget, 4, 8, 0); var field = field(budget, profiles, 2)) {
            helper.assertTrue(field.admit(left, a, 8, 0) && field.admit(right, b, 1, 0), "Admit actual adjacent captured Bricks");
            double[] h = new double[4096];
            for (int cell = 0; cell < h.length; cell++) if (profiles.signatures().isAir(a[cell])) h[cell] = 50 * field.airCapacityJPerK();
            helper.assertTrue(field.restoreValues(left, h, new byte[4096]), "Heat only the left side");
            helper.assertTrue(field.begin(banks.seal(0)), "Publish at the same physical time");
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (!field.advance(System.nanoTime() + 1_000_000L)) helper.assertTrue(System.nanoTime() < deadline, "Finish initial publication");
            helper.assertTrue(field.commit(), "Publish both sides together");
            var sample = new TiledThermalField.Sample(); double x = base.getX() + 15.875, y = base.getY() + 1.5, z = base.getZ() + 1.5;
            helper.assertTrue(field.sampleAir(x, y, z, sample), "Read the shared interface"); double joined = sample.temperatureC();
            helper.assertTrue(joined > 0 && joined < 50 && field.freeze(right) && field.sampleAir(x, y, z, sample), "A neighbour enters writeback ownership");
            near(helper, 50, sample.temperatureC(), "The old shared read cannot include a frozen neighbour");
            helper.assertTrue(field.thaw(right, 0, Double.POSITIVE_INFINITY) && field.sampleAir(x, y, z, sample), "The neighbour rejoins without cooling or advancing time");
            near(helper, joined, sample.temperatureC(), "Re-entry invalidates the isolated view");
            helper.assertTrue(field.retire(right) && field.sampleAir(x, y, z, sample), "Retire the already queried neighbouring owner");
            near(helper, 50, sample.temperatureC(), "A retired owner cannot survive in derived query data");
            FHMain.LOGGER.info("THERMAL_TILED_QUERY_VISIBILITY,joinedC={},isolatedC=50,thawedC={},retiredC=50,physicalTimeUnchanged=true", joined, joined);
        }
        helper.assertTrue(budget.usedBytes() == 0, "Neighbour visibility owners release"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void nativeReentryWaitsForTheRealBoundaryWithoutRetaggingAnotherBase(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4L << 20);
        var base = new BlockPos(274432, 192, 274432);
        long first = SectionPos.asLong(base), second = SectionPos.asLong(base.east(64));
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) captured[x | z << 4 | y << 8] = air;
        try (var banks = new SpatialEnergyBanks(budget, 4, 8, 0); var field = field(budget, profiles, 2)) {
            helper.assertTrue(field.admit(first, captured, 1, 10) && field.admit(second, captured, 1, 20), "Admit two separate bases");
            publish(helper, field, banks, 0);
            long prepaid = budget.usedBytes(); double energy = field.totalEnthalpyJ();
            helper.assertTrue(field.freeze(first), "Native unload freezes the actual committed frame");
            publish(helper, field, banks, 20);
            var influence = it.unimi.dsi.fastutil.longs.LongSets.singleton(second);
            helper.assertTrue(field.thaw(first, 10, Double.POSITIVE_INFINITY, 40, influence) && field.frozen(first),
                    "Reentry at tick 40 must wait for the influencing receiver's actual tick 20");
            helper.assertTrue(field.sectionSampleTick(first) == 0 && field.sectionSampleTick(second) == 20,
                    "Staging activation does not retag either committed temperature");
            publish(helper, field, banks, 40);
            helper.assertTrue(field.activate(first, influence) && field.sectionSampleTick(first) == 40,
                    "Only the explicit dormant-cooling activation rejoins the reached boundary");
            publish(helper, field, banks, 60);
            near(helper, energy, field.totalEnthalpyJ(), "Infinite half-life preserves actual energies on both sides of reentry");
            helper.assertTrue(budget.usedBytes() == prepaid, "Activation reuses the two paid frames");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Reentry ownership releases with the field"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void committedShrinkReturnsStorageEvenWhenNoOptionalCapacityRemains(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4L << 20);
        var base = new BlockPos(303104, 192, 303104); long section = SectionPos.asLong(base);
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) captured[x | z << 4 | y << 8] = air;
        try (var banks = new SpatialEnergyBanks(budget, 4, 8, 0); var field = field(budget, profiles, 1)) {
            helper.assertTrue(field.admit(section, captured, 3, 10), "Admit the actual two-Brick field");
            publish(helper, field, banks, 0);
            double energy = field.totalEnthalpyJ() + field.externalEnergyJ();
            for (int x = 4; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                helper.assertTrue(field.changeCell(base.offset(x, y, z).asLong(), ThermalSignatureTable.UNRESOLVED, TiledThermalField.Change.PHYSICAL), "Remove a committed Brick");
            publish(helper, field, banks, 0);
            try (var occupied = budget.tryReserve(budget.limitBytes() - budget.usedBytes())) {
                helper.assertTrue(occupied != null && field.compact(section), "Finishing a real shrink cannot wait for the optional capacity it will free");
                helper.assertTrue(field.sectionCellCount(section) == 64 && budget.usedBytes() < budget.limitBytes(), "The smaller arrays actually release capacity at the admission target");
                near(helper, energy, field.totalEnthalpyJ() + field.externalEnergyJ(), "Compaction does not remove or recreate physical energy");
                FHMain.LOGGER.info("THERMAL_FIXED_FULL_COMPACTION,beforeCells=128,afterCells=64,optionalBytesBefore=0,freedBytes={},chargedPeakOverlapBytes={}",
                        budget.limitBytes() - budget.usedBytes(), budget.peakUsedBytes() - budget.limitBytes());
            }
        }
        helper.assertTrue(budget.usedBytes() == 0, "Completion overlap and old/new arrays release on close"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 300)
    public static void freezeAfterCancelledPhysicalWorkKeepsGeometryAndExcludesUncommittedHeat(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4L << 20);
        var base = new BlockPos(315392, 192, 315392); long section = SectionPos.asLong(base);
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) captured[x | z << 4 | y << 8] = air;
        try (var banks = new SpatialEnergyBanks(budget, 4, 8, 0); var field = field(budget, profiles, 1)) {
            helper.assertTrue(field.admit(section, captured, 1, 0), "Admit actual Air cells");
            double[] h = new double[4096];
            for (int cell = 0; cell < h.length; cell++) if (profiles.signatures().isAir(captured[cell])) h[cell] = 10 * field.airCapacityJPerK();
            helper.assertTrue(field.restoreValues(section, h, new byte[4096]), "Set the Accepted initial heat");
            publish(helper, field, banks, 0);
            double accepted = field.totalEnthalpyJ() + field.externalEnergyJ(); var body = base.offset(3, 1, 2);
            helper.assertTrue(field.changeCell(body.asLong(), profiles.states().signatureId(Blocks.STONE.defaultBlockState()), TiledThermalField.Change.REPLACEMENT), "Prepare real replacement geometry");
            long receiver = banks.retain(new SpatialEnergyBanks.Address(base.offset(1, 1, 1).asLong(), -1, (byte) -1));
            helper.assertTrue(receiver != SpatialEnergyBanks.NO_RECEIVER && banks.add(receiver, 6400), "Give the private physical step a real finite input");
            banks.release(receiver);
            helper.assertTrue(field.begin(banks.seal(20)) && field.advance(Long.MAX_VALUE), "Actually finish private numerical work before its Accepted publication");
            field.cancel();
            helper.assertTrue(field.freeze(section), "Freeze after cancellation without accepting the private heat");
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(field.read(body.asLong(), true, sample), "The final material identity must survive");
            near(helper, 0, sample.temperatureC(), "The replacement starts at its physical natural temperature");
            near(helper, accepted, field.totalEnthalpyJ() + field.externalEnergyJ(), "The uncommitted 6400J cannot enter the frozen Accepted state");
            FHMain.LOGGER.info("THERMAL_FIXED_FREEZE_CANCELLED_WORK,uncommittedJ=6400,preservedGeometry=true,acceptedHeatOnly=true");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Cancelled numerical work and freeze release their owners"); helper.succeed();
    }

    private static void publish(GameTestHelper helper, TiledThermalField field, SpatialEnergyBanks banks, long tick) {
        helper.assertTrue(field.begin(banks.seal(tick)), "Start the actual fixed field step");
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L)) helper.assertTrue(System.nanoTime() < deadline, "Finish the bounded fixed step");
        helper.assertTrue(field.commit(), "Publish the actual Accepted bank/field step");
    }

    private static TiledThermalField field(ThermalMemoryBudget budget, MinecraftThermalProfiles.Snapshot profiles, int maximumTiles) {
        return new TiledThermalField(budget, profiles.signatures(), profiles.materials(), 1200, 0, 0, maximumTiles, 0);
    }
    private static Long2IntOpenHashMap cache(TiledThermalField field) throws Exception {
        try { var member = field.getClass().getDeclaredField("queryCells"); member.setAccessible(true); return (Long2IntOpenHashMap) member.get(field); }
        catch (NoSuchFieldException noCache) { return null; }
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)), message + ";expected=" + expected + ",actual=" + actual);
    }
}
