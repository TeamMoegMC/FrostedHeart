/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Ownership checks for the fixed-space core under production integration. */
@GameTestHolder("frostedheart_tiled_thermal")
@PrefixGameTestTemplate(false)
public final class TiledThermalFieldGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void v7CellValuesRoundTripAndOldThermalVersionIsIgnored(GameTestHelper helper) {
        var capture = new AirFieldCheckpoint.Capture(10).atTick(20);
        capture.addCellBrick(0, 0, 0, 0, 1L | Long.MIN_VALUE, new double[] {35, 45});
        var field = capture.finish();
        var state = new com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState(0, 1);
        state.mergeAir(0, new com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.SectionEntry(20, 10, field),
                1, 20, 10, 0, new com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.CaptureScratch());
        var chunk = new net.minecraft.nbt.CompoundTag(); state.encode(chunk);
        helper.assertTrue(chunk.getCompound("FrostedHeartThermal").getInt("version") == 7, "Only the current thermal format is written");
        var restored = com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.decode(chunk, 0, 1);
        helper.assertTrue(restored != null && restored.air(0) != null, "The current Section payload is readable");
        near(helper, 35, restored.air(0).spatialAir().blockCenterC(0, 0), "First saved cell round-trips exactly");
        near(helper, 45, restored.air(0).spatialAir().blockCenterC(0, 63), "The last cell retains its own value");
        helper.assertTrue(restored.air(0).spatialAir().sampleTickAt(.5, .5, .5, -1) == 20, "Serialization preserves the actual sampling time");
        var encoded = field.encode();
        helper.assertTrue(!encoded.contains("shapes") && !encoded.contains("components") && encoded.contains("bricks"),
                "The payload contains canonical cells without the retired field representation");
        var old = chunk.copy(); old.getCompound("FrostedHeartThermal").putInt("version", 6);
        helper.assertTrue(com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState.decode(old, 0, 1) == null,
                "Old thermal data is deliberately ignored under the user's compatibility decision");
        var full = new AirFieldCheckpoint.Capture(Double.NaN).atTick(20);
        full.addCellBrick(0, 0, 0, 0, 1, new double[] {0});
        helper.assertTrue(full.finish() != null, "A native accepted capture can defer residual filtering to the real environment");
        FHMain.LOGGER.info("THERMAL_V7_STORAGE,cells=2,lastCell=63,exact=true,sampleTick=20,oldVersionIgnored=true,noLegacyLayout=true");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void boundaryReconstructionCannotInventCoolingInAHeatOnlyField(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(2L << 20);
        var base = aligned(helper, 4352); long section = SectionPos.asLong(base);
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        for (int x = 1; x <= 3; x++) {
            helper.getLevel().setBlockAndUpdate(base.offset(x, 1, 1), Blocks.AIR.defaultBlockState());
            captured[x | 1 << 4 | 1 << 8] = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        }
        try (var field = field(budget, profiles, 1)) {
            helper.assertTrue(field.admit(section, captured, 1, 0), "Capture a finite Air trace ending at the edge of the known field");
            for (double sign : new double[] {1, -1}) {
                double[] h = new double[4096];
                h[1 | 1 << 4 | 1 << 8] = sign * 100 * field.airCapacityJPerK();
                h[2 | 1 << 4 | 1 << 8] = sign * 50 * field.airCapacityJPerK();
                helper.assertTrue(field.restoreValues(section, h, new byte[4096]), "Import the actual cell energies without a reference temperature curve");
                double owned = field.totalEnthalpyJ(); var sample = new TiledThermalField.Sample();
                helper.assertTrue(field.sampleAir(base.getX() + 3.875, base.getY() + 1.5, base.getZ() + 1.5, sample), "Read the real boundary position");
                helper.assertTrue(sign * sample.temperatureC() >= 0,
                        "A query cannot invent the opposite thermal effect at a one-sided boundary; sign=" + sign + ",observedC=" + sample.temperatureC());
                near(helper, owned, field.totalEnthalpyJ(), "The query changes no canonical energy");
                FHMain.LOGGER.info("THERMAL_CELL_BOUNDARY_SIGN,sign={},observedC={},energyUnchanged=true", sign, sample.temperatureC());
            }
        }
        helper.assertTrue(budget.usedBytes() == 0, "Boundary reconstruction owners release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void savedCellValuesKeepEnergyAndInteriorLinear(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(2L << 20);
        var base = aligned(helper, 4096); long section = SectionPos.asLong(base);
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED);
        double[] values = new double[64];
        for (int cell = 0; cell < 64; cell++) {
            int x = cell & 3, y = cell >>> 4, z = cell >>> 2 & 3;
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            captured[x | z << 4 | y << 8] = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
            values[cell] = 10 + 2 * (x + .5) + 3 * (y + .5) + 5 * (z + .5);
        }
        var capture = new AirFieldCheckpoint.Capture(0).atTick(20);
        capture.addCellBrick(base.getX(), base.getY(), base.getZ(), 0, -1L, values);
        try (var field = field(budget, profiles, 1)) {
            helper.assertTrue(field.admit(section, captured, 1, 0), "Capture the actual Air cells before reading their saved means");
            helper.assertTrue(field.restoreValues(section, new double[4096], new byte[4096], capture.finish(), 20, 60, 2),
                    "Saved v7 cell values are cooled once and restored as energy");
            var sample = new TiledThermalField.Sample(); double total = 0;
            for (int cell = 0; cell < 64; cell++) {
                int x = cell & 3, y = cell >>> 4, z = cell >>> 2 & 3;
                double mean = .5 * (10 + 2 * (x + .5) + 3 * (y + .5) + 5 * (z + .5));
                helper.assertTrue(field.read(base.offset(x, y, z).asLong(), false, sample), "Read canonical Air energy");
                near(helper, mean * field.airCapacityJPerK(), sample.enthalpyJ(), "Each stored mean restores its own cell energy");
                total += sample.enthalpyJ();
                for (double fraction : new double[] {.125, .5, .875}) {
                    helper.assertTrue(field.sampleAir(base.getX() + x + fraction, base.getY() + y + .3, base.getZ() + z + .7, sample), "Read reconstructed positions");
                    double expected = .5 * (10 + 2 * (x + fraction) + 3 * (y + .3) + 5 * (z + .7));
                    if (x > 0 && x < 3 && y > 0 && y < 3 && z > 0 && z < 3)
                        near(helper, expected, sample.temperatureC(), "Interior linearity does not require wall extrapolation");
                    else helper.assertTrue(Math.abs(expected - sample.temperatureC()) <= 1,
                            "The small-gradient boundary fixture remains within the user's 1C floor");
                }
            }
            near(helper, total, field.totalEnthalpyJ(), "Queries do not own or alter the imported energy");
            var saved = field.captureAir(section, 1, Double.NaN);
            for (int cell = 0; cell < 64; cell++) {
                field.read(base.offset(cell & 3, cell >>> 4, cell >>> 2 & 3).asLong(), false, sample);
                near(helper, sample.temperatureC(), saved.blockCenterC(0, cell),
                        "The next save contains canonical means, not derived query vertices");
            }
            FHMain.LOGGER.info("THERMAL_CELL_V7_RESTORE,cells=64,fractionalSamples=192,cooledOnce=true,conservedJ={},saveUsesCanonicalMeans=true", total);
        }
        helper.assertTrue(budget.usedBytes() == 0, "The imported field releases both frames");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void zeroTimeGeometryDoesNotExchangeHeatOrRollBackUnrelatedSections(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(4L << 20);
        var base = new BlockPos(106496, 192, 106496);
        var a = base.offset(1, 1, 1); var b = a.east(16); var c = a.east(32);
        for (var origin : new BlockPos[] {base, base.east(16), base.east(32)})
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        try (var banks = new SpatialEnergyBanks(budget, 8, 32, 0); var field = field(budget, profiles, 8)) {
            for (var position : new BlockPos[] {a, b, c}) helper.assertTrue(field.admit(SectionPos.asLong(position),
                    capture(helper, profiles, SectionPos.asLong(position)), 1, 0), "Capture three independent actual Sections");
            long first = banks.retain(new SpatialEnergyBanks.Address(a.asLong(), -1));
            long untouched = banks.retain(new SpatialEnergyBanks.Address(b.asLong(), -1));
            long later = banks.retain(new SpatialEnergyBanks.Address(c.asLong(), -1));
            banks.add(first, 6400); step(field, banks, 20);
            banks.add(untouched, 3200); step(field, banks, 40);
            var sample = new TiledThermalField.Sample(); field.read(b.asLong(), false, sample); double beforeB = sample.temperatureC();
            field.read(c.asLong(), false, sample); double beforeC = sample.temperatureC();
            long pairs = field.pairUpdates(); double accounted = field.totalEnthalpyJ() + field.externalEnergyJ();
            helper.getLevel().setBlockAndUpdate(a, Blocks.STONE.defaultBlockState());
            helper.assertTrue(field.changeCell(a.asLong(), profiles.states().signatureId(Blocks.STONE.defaultBlockState()),
                    TiledThermalField.Change.REPLACEMENT), "Capture one real local geometry edit");
            banks.add(later, 800); step(field, banks, 40);
            helper.assertTrue(field.pairUpdates() == pairs && field.read(b.asLong(), false, sample)
                            && sample.temperatureC() == beforeB && sample.sampleTick() == 40,
                    "A zero-time publication never scans exchange pairs or exposes an unrelated older buffer");
            helper.assertTrue(field.read(c.asLong(), false, sample) && sample.temperatureC() > beforeC,
                    "Input prepares a separate touched Section before injecting into it");
            near(helper, accounted + 800, field.totalEnthalpyJ() + field.externalEnergyJ(),
                    "Geometry projection and deferred input conserve J at the same numerical time");
            step(field, banks, 60);
            helper.assertTrue(field.read(b.asLong(), false, sample) && sample.sampleTick() == 60,
                    "The next ordinary interval uses each Section's correct accepted buffer");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Local zero-time publication releases all field/input ownership");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void windAffectsEnvironmentExchangeWithoutChangingClosedInteriorMixing(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var base = aligned(helper, 3072);
        int air = profiles.states().signatureId(Blocks.AIR.defaultBlockState());
        long section = SectionPos.asLong(base), position = base.offset(1, 1, 1).asLong();
        for (double boundary : new double[] {0, 32}) {
            var calmBudget = new ThermalMemoryBudget(1 << 20);
            var windyBudget = new ThermalMemoryBudget(1 << 20);
            try (var calmBank = new SpatialEnergyBanks(calmBudget, 8, 8, 0);
                    var windyBank = new SpatialEnergyBanks(windyBudget, 8, 8, 0);
                    var calm = new TiledThermalField(calmBudget, profiles.signatures(), profiles.materials(),
                            1200, 96, boundary, 1, 0);
                    var windy = new TiledThermalField(windyBudget, profiles.signatures(), profiles.materials(),
                            1200, 96, boundary, 1, 0)) {
                int[] captured = new int[4096]; Arrays.fill(captured, air);
                helper.assertTrue(calm.admit(section, captured, 1, 0) && windy.admit(section, captured, 1, 0)
                                && windy.windScale(1.75), "Capture identical closed/open numerical volumes");
                long calmInput = calmBank.retain(new SpatialEnergyBanks.Address(position, -1));
                long windyInput = windyBank.retain(new SpatialEnergyBanks.Address(position, -1));
                calmBank.add(calmInput, 6400); windyBank.add(windyInput, 6400);
                for (int tick = 20; tick <= 120; tick += 20) { step(calm, calmBank, tick); step(windy, windyBank, tick); }
                var a = new TiledThermalField.Sample(); var b = new TiledThermalField.Sample();
                if (boundary == 0) {
                    for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                        long probe = base.offset(x, y, z).asLong();
                        helper.assertTrue(calm.read(probe, false, a) && windy.read(probe, false, b)
                                        && a.temperatureC() == b.temperatureC(),
                                "Wind must not alter an interior whose environment exchange is closed");
                    }
                } else {
                    helper.assertTrue(windy.totalEnthalpyJ() < calm.totalEnthalpyJ(),
                            "Wind accelerates actual environment exchange; calmJ=" + calm.totalEnthalpyJ() + ",windyJ=" + windy.totalEnthalpyJ());
                }
            }
            helper.assertTrue(calmBudget.usedBytes() == 0 && windyBudget.usedBytes() == 0, "Wind checks release numerical storage");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void localGrowthPreservesHeatInputAddressesAndPendingReplacement(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare(); var budget = new ThermalMemoryBudget(1024 * 1024);
        var base = aligned(helper, 2048); long section = SectionPos.asLong(base);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        var target = base.offset(1, 1, 1); var ice = base.offset(2, 1, 1);
        helper.getLevel().setBlockAndUpdate(ice, Blocks.ICE.defaultBlockState());
        try (var banks = new SpatialEnergyBanks(budget, 8, 32, 0); var field = field(budget, profiles, 8)) {
            helper.assertTrue(field.admit(section, capture(helper, profiles, section), 1, 0), "Capture one initial Brick");
            long source = banks.retain(new SpatialEnergyBanks.Address(target.asLong(), -1));
            banks.add(source, 6400); step(field, banks, 20);
            double before = field.totalEnthalpyJ() + field.externalEnergyJ();
            var old = new TiledThermalField.Sample(); field.read(target.asLong(), false, old);
            double temperature = old.temperatureC();
            int iceSignature = profiles.states().signatureId(helper.getLevel().getBlockState(ice));
            helper.assertTrue(field.changeCell(ice.asLong(), iceSignature, TiledThermalField.Change.REPLACEMENT), "Capture a same-state physical replacement before growth");
            helper.assertTrue(field.grow(section, capture(helper, profiles, section), 2), "Grow only this Section with overlap pre-admitted");
            var acceptedSave = new TiledThermalField.SectionSnapshot();
            helper.assertTrue(field.snapshotAccepted(section, acceptedSave) && acceptedSave.activeBricks == 1,
                    "An Accepted save cannot mark the newly prepared Brick as captured before publication");
            helper.assertTrue(field.read(target.asLong(), false, old) && old.temperatureC() == temperature && old.sampleTick() == 20,
                    "Growth preserves the exact Accepted temperature and its clock");
            helper.assertTrue(field.tileCount() == 1 && field.sectionCellCount(section) == 128, "Growth installs sparse Bricks in one owner");
            banks.add(source, 1600); step(field, banks, 40);
            // The ice replacement transfers its previous matter to the external
            // ledger, independently of the unchanged world-address source handle.
            near(helper, before + 1600, field.totalEnthalpyJ() + field.externalEnergyJ(), "Growth retains both source J and replacement ownership");
            long available = budget.limitBytes() - budget.usedBytes();
            try (var pressure = budget.tryReserve(available - 1024)) {
                helper.assertTrue(pressure != null && !field.grow(section, capture(helper, profiles, section), -1L), "Refuse full replacement before modifying the live Tile");
                helper.assertTrue(field.tileCount() == 1 && field.sectionCellCount(section) == 128, "A refused growth leaves current coverage usable");
            }
            step(field, banks, 60);
            helper.assertTrue(field.read(target.asLong(), false, old) && old.sampleTick() == 60, "The original field still advances after growth refusal");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Growth releases old/new reservations and retained directories");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void actualSectionBoundaryConservesHeatAndKeepsAcceptedSaveDuringSolve(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        var base = aligned(helper, 512);
        var first = base.offset(15, 1, 1); var second = first.east();
        for (int x = 12; x < 20; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        long a = SectionPos.asLong(first), b = SectionPos.asLong(second);
        try (var banks = new SpatialEnergyBanks(budget, 8, 128, 0);
                var field = field(budget, profiles, 16)) {
            helper.assertTrue(field.admit(a, capture(helper, profiles, a), 1L << 3, 0)
                            && field.admit(b, capture(helper, profiles, b), 1L, 0), "Admit the two actual Section captures");
            long receiver = banks.retain(new SpatialEnergyBanks.Address(first.asLong(), -1));
            double inputJ = profiles.tuning().campfire().ratedPowerW() * profiles.tuning().campfire().port(0).powerShare();
            banks.add(receiver, inputJ);
            var batch = banks.seal(20);
            helper.assertTrue(field.begin(batch), "Begin the first physical second");
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(field.read(second.asLong(), false, sample) && sample.sampleTick() == 0 && sample.temperatureC() == 0,
                    "Readers keep the completed initial field while the writer works");
            finish(field);
            var snapshot = new TiledThermalField.SectionSnapshot();
            helper.assertTrue(field.snapshot(a, snapshot) && snapshot.sampleTick == 0,
                    "Saving a finished but uncommitted trial still takes Accepted");
            double[] pending = {0}; banks.pendingInChunk(new net.minecraft.world.level.ChunkPos(first).toLong(), (address, joules) -> pending[0] += joules);
            helper.assertTrue(pending[0] == inputJ, "The claimed input is still included in the same save prefix");
            helper.assertTrue(field.commit(), "Commit state and bank together");
            helper.assertTrue(field.read(second.asLong(), false, sample) && sample.sampleTick() == 20 && sample.temperatureC() > 0,
                    "Heat crosses a real Section boundary");
            near(helper, inputJ, field.totalEnthalpyJ(), "Every cross-boundary subtraction has an equal addition");
            step(field, banks, 40);
            near(helper, inputJ, field.totalEnthalpyJ(), "A later passive step does not inject the source energy again");
            long changedBefore = field.changedCells();
            helper.getLevel().setBlockAndUpdate(first, Blocks.STONE.defaultBlockState());
            helper.assertTrue(field.changeCell(first.asLong(), profiles.states().signatureId(helper.getLevel().getBlockState(first)),
                    TiledThermalField.Change.REPLACEMENT), "Capture one actual replacement");
            double before = field.totalEnthalpyJ() + field.externalEnergyJ();
            step(field, banks, 60);
            helper.assertTrue(field.changedCells() == changedBefore + 1 && field.tileCount() == 2,
                    "One replacement updates one geometry cell without joining or rebuilding the neighbour");
            near(helper, before, field.totalEnthalpyJ() + field.externalEnergyJ(), "Removed matter is settled to the explicit world boundary");
            FHMain.LOGGER.info("THERMAL_TILED_BOUNDARY_RESULT,tiles={},pairs={},changedCells={},inputJ={},reservedBytes={},scope=numerical_candidate",
                    field.tileCount(), field.pairUpdates(), field.changedCells(), inputJ, budget.usedBytes());
        }
        helper.assertTrue(budget.usedBytes() == 0, "Both frames and inputs release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void actualIceKeepsLatentHeatAndExcessAcrossItsPhaseIdentity(GameTestHelper helper) {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        var ice = aligned(helper, 768);
        helper.getLevel().setBlockAndUpdate(ice, Blocks.ICE.defaultBlockState());
        int signature = profiles.states().signatureId(helper.getLevel().getBlockState(ice));
        var law = profiles.materials().profileOrNull(profiles.signatures().materialProfileId(signature)).thermalLaw();
        helper.assertTrue(law.heating() != null, "The real configured ice has a latent melting edge");
        var edge = law.heating();
        int[] captured = new int[4096]; Arrays.fill(captured, ThermalSignatureTable.UNRESOLVED); captured[0] = signature;
        try (var banks = new SpatialEnergyBanks(budget, 4, 32, 0);
                var field = field(budget, profiles, 4)) {
            helper.assertTrue(field.admit(SectionPos.asLong(ice), captured, 1, edge.transitionTemperatureC() - 1), "Admit one isolated actual ice body");
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(field.read(ice.asLong(), true, sample), "Read its initial identity");
            int generation = sample.materialGeneration();
            double initial = field.totalEnthalpyJ(), excess = 1000;
            double energy = edge.targetEnthalpyJ() - sample.enthalpyJ() + excess;
            long receiver = banks.retain(new SpatialEnergyBanks.Address(ice.asLong(), generation));
            banks.add(receiver, energy);
            step(field, banks, 20);
            helper.assertTrue(field.read(ice.asLong(), true, sample), "The completed ice still waits for its world transition");
            near(helper, edge.targetEnthalpyJ(), sample.enthalpyJ(), "Energy stops at the phase acknowledgement boundary");
            near(helper, edge.transitionTemperatureC(), sample.temperatureC(), "Latent energy does not overheat ice");
            near(helper, excess, banks.acceptedPendingJoules(), "Excess input remains accepted and saveable");
            banks.release(receiver);
            helper.assertTrue(banks.receiverCount() == 1, "The pending balance survives producer retirement");
            var next = Block.BLOCK_STATE_REGISTRY.byId(edge.targetStateId());
            helper.getLevel().setBlockAndUpdate(ice, next);
            helper.assertTrue(field.changeCell(ice.asLong(), profiles.states().signatureId(next), TiledThermalField.Change.PHASE),
                    "Install the actual configured next phase");
            step(field, banks, 40);
            helper.assertTrue(field.read(ice.asLong(), true, sample) && sample.materialGeneration() == generation,
                    "A phase change preserves material incarnation, unlike replacement");
            near(helper, initial + energy, field.totalEnthalpyJ() + banks.acceptedPendingJoules() + field.externalEnergyJ(),
                    "The phase handoff retains both latent heat and once-only excess");
            helper.assertTrue(banks.acceptedPendingJoules() < excess, "The next phase can consume the retained excess");
            FHMain.LOGGER.info("THERMAL_TILED_PHASE_RESULT,generation={},pendingJ={},scope=numerical_prototype", generation, banks.acceptedPendingJoules());
        }
        helper.assertTrue(budget.usedBytes() == 0, "Phase states and retired input release");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void tileSaturationRejectsBeforeAllocationAndRetirementReleasesNeighbours(GameTestHelper helper) throws ReflectiveOperationException {
        var profiles = MinecraftThermalProfiles.prepare();
        var budget = new ThermalMemoryBudget(4 * 1024 * 1024);
        int[] air = new int[4096]; Arrays.fill(air, profiles.states().signatureId(Blocks.AIR.defaultBlockState()));
        long a = SectionPos.asLong(0, 4, 0), b = SectionPos.asLong(1, 4, 0), c = SectionPos.asLong(2, 4, 0);
        try (var field = field(budget, profiles, 8); var banks = new SpatialEnergyBanks(budget, 4, 16, 0)) {
            helper.assertTrue(field.admit(a, air, 1, 0) && field.admit(b, air, 1, 0), "Admit adjacent owners");
            try (var pressure = budget.tryReserve(budget.limitBytes() - budget.usedBytes())) {
                helper.assertTrue(pressure != null && !field.admit(c, air, 1, 0) && field.tileCount() == 2,
                        "A rejected tile allocates neither state buffers nor a pending construction");
                step(field, banks, 20);
                helper.assertTrue(field.acceptedTick() == 20 && budget.peakUsedBytes() == budget.limitBytes(),
                        "Already owned fields can finish at a full budget");
            }
            var data = TiledThermalField.class.getDeclaredField("bySection"); data.setAccessible(true);
            var tiles = (it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<?>) data.get(field);
            Object retired = tiles.get(a), neighbour = tiles.get(b);
            var links = retired.getClass().getDeclaredField("neighbours"); links.setAccessible(true);
            helper.assertTrue(field.retire(a) && field.tileCount() == 1, "Remove the durably handed-off tile");
            for (Object link : (Object[]) links.get(neighbour)) helper.assertTrue(link != retired, "A live neighbour must not retain the retired tile");
            helper.assertTrue(field.admit(c, air, 1, 0), "Real retirement capacity can admit another tile");
        }
        helper.assertTrue(budget.usedBytes() == 0, "Tile directories and both state banks release");
        helper.succeed();
    }

    private static TiledThermalField field(ThermalMemoryBudget budget, MinecraftThermalProfiles.Snapshot profiles, int maximumTiles) {
        return new TiledThermalField(budget, profiles.signatures(), profiles.materials(),
                profiles.tuning().airHeatCapacityJPerBlockK(), profiles.tuning().airMixingWPerBlockK(), 0, maximumTiles, 0);
    }

    private static BlockPos aligned(GameTestHelper helper, int offset) {
        var position = helper.absolutePos(new BlockPos(offset, 192, 0));
        return new BlockPos(position.getX() & ~15, position.getY() & ~15, position.getZ() & ~15);
    }

    private static int[] capture(GameTestHelper helper, MinecraftThermalProfiles.Snapshot profiles, long section) {
        int[] result = new int[4096]; var position = new BlockPos.MutableBlockPos();
        int x = SectionPos.x(section) << 4, y = SectionPos.y(section) << 4, z = SectionPos.z(section) << 4;
        for (int cell = 0; cell < result.length; cell++) {
            position.set(x + (cell & 15), y + (cell >>> 8), z + (cell >>> 4 & 15));
            result[cell] = profiles.states().signatureId(helper.getLevel().getBlockState(position));
        }
        return result;
    }

    private static void step(TiledThermalField field, SpatialEnergyBanks banks, long tick) {
        if (!field.begin(banks.seal(tick))) throw new AssertionError("Could not start tiled step");
        finish(field);
        if (!field.commit()) throw new AssertionError("Could not commit tiled step");
    }

    private static void finish(TiledThermalField field) {
        long limit = System.nanoTime() + 5_000_000_000L;
        while (!field.advance(System.nanoTime() + 1_000_000L))
            if (System.nanoTime() > limit) throw new AssertionError("Tiled numerical step did not finish");
    }

    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= Math.max(1e-6, Math.abs(expected) * 1e-12),
                message + ": expected=" + expected + ", actual=" + actual);
    }
}
