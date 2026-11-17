/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Source controls keep their physical order without rebuilding unchanged geometry. */
@GameTestHolder("frostedheart_fixed_mixing")
@PrefixGameTestTemplate(false)
public final class ThermalMixingUpdateGameTests {
    private record Result(double[] temperatures, double[] energy, int geometryCommits, double degradedJ) { }

    @GameTest(template = "phase0a_empty", batch = "fixed_mixing_open", timeoutTicks = 600)
    public static void sourceControlsMatchFullPreparationWithoutRebuildingOpenGeometry(GameTestHelper helper) {
        compare(helper, false, false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_mixing_passage", timeoutTicks = 600)
    public static void newSourceAndRepeatedControlsReuseTheCapturedPassage(GameTestHelper helper) {
        compare(helper, true, false);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_mixing_future_source", timeoutTicks = 600)
    public static void aLaterSourceAcrossTheSectionBorderCannotChangeAnEarlierCoefficientCut(GameTestHelper helper) {
        compare(helper, false, true);
        helper.succeed();
    }

    private static void compare(GameTestHelper helper, boolean passage, boolean future) {
        var base = new BlockPos(future ? 362496 : passage ? 361472 : 360448, 192, 352256);
        Result reference = run(helper, base, passage, future, true);
        Result ordinary = run(helper, base.east(32), passage, future, false);
        double maximumC = 0, maximumJ = 0;
        for (int index = 0; index < reference.temperatures.length; index++)
            maximumC = Math.max(maximumC, Math.abs(reference.temperatures[index] - ordinary.temperatures[index]));
        for (int index = 0; index < reference.energy.length; index++)
            maximumJ = Math.max(maximumJ, Math.abs(reference.energy[index] - ordinary.energy[index]));
        FHMain.LOGGER.info("THERMAL_MIXING_UPDATE,passage={},future={},referenceGeometry={},ordinaryGeometry={},samples={},maximumC={},maximumJ={},degradedJ={}",
                passage, future, reference.geometryCommits, ordinary.geometryCommits, ordinary.temperatures.length, maximumC, maximumJ, ordinary.degradedJ);
        helper.assertTrue(maximumC < 1e-9 && maximumJ < 1e-7,
                "The identical real source history preserves temperature and energy against ordinary full preparation; C=" + maximumC + ", J=" + maximumJ);
        helper.assertTrue(ordinary.degradedJ < 1e-7, "The new source can use the already captured passage without losing heat");
        helper.assertTrue(reference.geometryCommits > 0 && ordinary.geometryCommits == 0,
                "Source-only controls must update coefficients without a geometry transaction; full=" + reference.geometryCommits + ", ordinary=" + ordinary.geometryCommits);
    }

    private static Result run(GameTestHelper helper, BlockPos base, boolean passage, boolean future, boolean fullPreparation) {
        var level = helper.getLevel();
        int width = future ? 24 : 4;
        for (int x = 0; x < width; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        var fire = base.offset(future ? 10 : 1, 0, 1);
        var later = base.offset(17, 0, 1);
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        if (future) {
            level.setBlockAndUpdate(later, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            level.setBlockAndUpdate(base.offset(15, 1, 1), Blocks.STONE.defaultBlockState());
        }
        if (passage) for (int y = 1; y <= 2; y++)
            level.setBlockAndUpdate(base.offset(1, y, 1), Blocks.OAK_FENCE.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, width, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 8)) {
            var profile = simulation.profiles.tuning().campfire();
            if (future) {
                var warm = base.offset(15, 1, 1); var state = new TiledThermalField.Sample();
                helper.assertTrue(simulation.fixed.read(warm.asLong(), true, state), "The warm body exposes both sides of the Section border");
                simulation.material.applyMaterialControl(simulation.slot(warm), state.law().enthalpyAtTemperature(64), state.law().initialBranch(64));
            }
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 0), "Register a real source after its geometry and passage were captured");
            int[] geometry = {0};
            var previous = ThermalDiagnostics.observer;
            ThermalDiagnostics.observer = new ThermalDiagnostics.Observer() {
                @Override public void geometry(Object task, ThermalInputBatch input, RegionalContinuationBoundary boundary, String stage, boolean complete) {
                    if (stage.equals("PUBLISHED")) geometry[0]++;
                }
                @Override public void mixingUpdated(ThermalInputBatch input, int bricks) {
                    if (!future || input.targetTick() != 0 && input.targetTick() != 40) return;
                    WorkerPhysicalSourceBindings bindings = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceBindings");
                    helper.assertTrue(bindings.activeAirPortCount() == 1,
                            "Exactly the source emitting at this real cut is live; tick=" + input.targetTick() + ",ports=" + bindings.activeAirPortCount());
                    var region = new com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion();
                    region.reset(base.getX() + 12, base.getY(), base.getZ());
                    bindings.collectMixingSources(region, 0);
                    helper.assertTrue(region.volumeScale(base.getX() + 15.5, base.getY() + 1.5, base.getZ() + 1.5) == 1,
                            "The later source contributes no coefficient before its birth");
                    region.reset(base.getX() + 12, base.getY(), base.getZ());
                    bindings.collectMixingSources(region, 40);
                    helper.assertTrue((region.volumeScale(base.getX() + 15.5, base.getY() + 1.5, base.getZ() + 1.5) > 1) == (input.targetTick() == 40),
                            "Even a future-dated query cannot see a source before its control becomes physically effective");
                }
            };
            var temperatures = new java.util.ArrayList<Double>();
            var energy = new java.util.ArrayList<Double>();
            try {
                var sample = new TiledThermalField.Sample();
                for (long tick = 0; tick <= 240; tick += 20) {
                    if (future && tick == 20) continue;
                    if (future && tick == 40) helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(later.asLong(), 1,
                            ThermalSourceMode.POWER_SOURCE, later.getX(), later.getY(), later.getZ(), profile.profileId(),
                            WorkerPhysicalSourceBindings.initialPorts(later.asLong(), profile)), profile.ratedPowerW(), true, 40),
                            "Queue a later source in the adjacent Section before the first control is processed");
                    if (tick > 0 && tick % 40 == 0) producer.enabled(fire.asLong(), tick % 80 == 0, tick - 20);
                    var input = new ThermalInputBatch(1, ++simulation.sequence, tick, ThermalInputBatch.NO_ADMISSIONS,
                            ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                            ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
                    var envelope = new ThermalInputEnvelope(input, producer.seal(tick));
                    if (future && tick == 0) { simulation.engine.offerInput(envelope); continue; }
                    driveComparison(helper, simulation, envelope, fullPreparation);
                    helper.assertTrue(simulation.fixed.acceptedTick() == tick, "All source controls settle at their actual physical boundary");
                    for (int x = 0; x < width; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
                        var position = base.offset(x, y, z);
                        helper.assertTrue(simulation.fixed.read(position.asLong(), !level.getBlockState(position).isAir(), sample), "Read every captured cell with its actual matter kind");
                        temperatures.add(sample.temperatureC());
                    }
                    energy.add(simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
                    ThermalSourceLedger sources = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
                    var balance = sources.energyBalance();
                    energy.add(balance.outputJ()); energy.add(balance.deliveredJ());
                    energy.add(balance.declaredLossJ()); energy.add(balance.degradedLossJ());
                }
                helper.assertTrue(simulation.fixed.read(fire.above(3).asLong(), false, sample) && sample.temperatureC() > 0,
                        "Actual heating reaches the receiver above the source and its passage");
            } finally { ThermalDiagnostics.observer = previous; }
            ThermalSourceLedger sources = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
            return new Result(temperatures.stream().mapToDouble(Double::doubleValue).toArray(),
                    energy.stream().mapToDouble(Double::doubleValue).toArray(), geometry[0], sources.energyBalance().degradedLossJ());
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
    }

    /** Select the existing full task on identical inputs; no alternate solver or production switch. */
    private static void driveComparison(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation,
            ThermalInputEnvelope envelope, boolean fullPreparation) {
        var engine = simulation.engine;
        try {
            var type = engine.getClass();
            var selectedMethod = type.getDeclaredMethod("nextFixedGeometryInput"); selectedMethod.setAccessible(true);
            var refresh = type.getDeclaredMethod("refreshFixedBoundaries"); refresh.setAccessible(true);
            var cancel = type.getDeclaredMethod("cancelFixedStep"); cancel.setAccessible(true);
            var active = type.getDeclaredField("fixedGeometry"); active.setAccessible(true);
            var work = java.util.Arrays.stream(type.getDeclaredClasses()).filter(candidate -> candidate.getSimpleName().equals("FixedGeometry")).findFirst().orElseThrow();
            var constructor = work.getDeclaredConstructor(type, com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox.Input.class);
            constructor.setAccessible(true);
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress ingress = ThermalLoadedWorldGameTests.read(engine, "sourceIngress");
            engine.offerInput(envelope);
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
            do {
                if (simulation.fixed.idle() && active.get(engine) == null) {
                    // Present the same available source controls before either path
                    // selects a Page. Binding waits still use the original dependencies.
                    while (ingress.hasReadyWork()) {
                        ingress.advance(System.nanoTime() + 2_000_000L);
                        helper.assertTrue(System.nanoTime() < deadline, "Source input reaches its original physical boundary");
                    }
                    if (fullPreparation) {
                        refresh.invoke(engine);
                        var selected = selectedMethod.invoke(engine);
                        if (selected != null) { cancel.invoke(engine); active.set(engine, constructor.newInstance(engine, selected)); }
                    }
                }
                if (engine.hasReadyWork()) engine.processWorkRound();
                com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion completion;
                while ((completion = engine.pollWorkCompletion()) != null)
                    helper.assertTrue(completion.failure() == null, "The existing full preparation completes without a worker error");
                helper.assertTrue(System.nanoTime() < deadline, "The original full preparation drains the identical input: " + engine.regionalProgressDescription());
            } while (engine.hasWork());
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Select the original full geometry task in this controlled comparison", failure); }
    }
}
