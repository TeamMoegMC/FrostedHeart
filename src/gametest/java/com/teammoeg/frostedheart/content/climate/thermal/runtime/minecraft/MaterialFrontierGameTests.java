/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Production conduction and threshold-driven residency without a material-depth cutoff. */
@GameTestHolder("frostedheart_material_frontier")
@PrefixGameTestTemplate(false)
public final class MaterialFrontierGameTests {
    @GameTest(template = "phase0a_empty", batch = "material_frontier_layers", timeoutTicks = 600)
    public static void heatReachesThirdAndDeeperMaterialsInAllSixDirections(GameTestHelper helper) {
        var origin = new BlockPos(425996, 204, 425996);
        fill(helper, origin, 12, 12, 12);
        var source = origin.offset(5, 5, 5);
        helper.getLevel().setBlockAndUpdate(source, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var sim = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 12, 12, 12, 0)) {
            setTemperature(sim, source, 600);
            helper.assertTrue(sim.totalEnergy() > 0, "The enclosed source contains finite controlled heat");
            double before = sim.totalEnergy() + sim.fixed.externalEnergyJ();
            for (int tick = 20; tick <= 4000; tick += 20) {
                sim.tick = tick;
                sim.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            }
            double minimumDeepC = Double.POSITIVE_INFINITY;
            for (BlockFace face : BlockFace.values()) for (int distance : new int[]{1, 2, 4}) {
                var point = source.offset(face.stepX() * distance, face.stepY() * distance, face.stepZ() * distance);
                double value = temperature(helper, sim, point);
                helper.assertTrue(value > 1e-5, "Material receives heat beyond two layers: " + face + " distance=" + distance + " C=" + value);
                if (distance == 4) minimumDeepC = Math.min(minimumDeepC, value);
            }
            near(helper, before, sim.totalEnergy() + sim.fixed.externalEnergyJ(), "Material relaying conserves finite source heat");
            FHMain.LOGGER.info("MATERIAL_FRONTIER_UNLIMITED_DEPTH,cells=1728,simulatedTicks=4000,sixDirections=true,minDistanceFourC={}", minimumDeepC);
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "material_frontier_hysteresis", timeoutTicks = 600)
    public static void buriedStoneRequestsSixFacesWithTheOriginalHysteresis(GameTestHelper helper) {
        var origin = new BlockPos(427008, 192, 427008);
        fill(helper, origin, 4, 4, 4);
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var sim = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0)) {
            installHalo(sim, origin, 4);
            for (BlockFace face : BlockFace.values()) {
                int side = (face.ordinal() & 1) * 3;
                var point = origin.offset(face.stepX() == 0 ? 1 : side, face.stepY() == 0 ? 1 : side, face.stepZ() == 0 ? 1 : side);
                var neighbor = point.offset(face.stepX(), face.stepY(), face.stepZ());
                boolean previouslyRequested = false;
                for (double value : new double[]{.75, 1, .75, .5, .49, -1, -.75, -.5, -.49}) {
                    setTemperature(sim, point, value);
                    sim.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
                    boolean expected = Math.abs(value) >= (previouslyRequested ? .5 : 1);
                    helper.assertTrue(requested(sim, neighbor) == expected,
                            "Buried stone uses 1K entry and 0.5K retention: face=" + face + " C=" + value);
                    previouslyRequested = expected;
                }
                setTemperature(sim, point, 0);
                sim.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            }
            helper.assertTrue(requested(sim, origin), "The actual source seed retains its input receiver");
            FHMain.LOGGER.info("MATERIAL_FRONTIER_SIX_FACE_THRESHOLDS,enterK=1,exitK=0.5,buriedStone=true,positiveAndNegative=true,coldNeighborsRetained=false");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "material_frontier_growth", timeoutTicks = 600)
    public static void innerMaterialCanAdmitTheThirdBrickAndThenRequestTheFourth(GameTestHelper helper) {
        var origin = new BlockPos(428032, 192, 425984);
        fill(helper, origin, 16, 4, 4);
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var sim = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 8, 4, 4, 0)) {
            installHalo(sim, origin, 8);
            var secondEdge = origin.offset(7, 1, 1);
            var third = origin.offset(8, 1, 1);
            var fourth = origin.offset(12, 1, 1);
            helper.assertTrue(!requested(sim, third), "A cold second Brick does not recruit the third");
            setTemperature(sim, secondEdge, 1.25);
            sim.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(requested(sim, third) && !requested(sim, fourth), "Heated inner material requests only its contacting next Brick");
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(!sim.fixed.read(third.asLong(), true, sample), "The third Brick has not yet been captured");

            var page = sim.page(origin);
            var update = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 7, 1,
                    sim.capture.captureBricks(page.sectionKey(), null, sim.capture.unresolvedPage(), 7), null, null);
            var geometry = new ResolvedGeometryBatch.Builder();
            geometry.addHalo(page, page.liveGeometryRevision(), halo(sim, origin, 12, 4, 4));
            var completion = RegionalEngineTestDriver.submit(helper, sim.engine, new ThermalInputBatch(1, ++sim.sequence, sim.tick,
                    ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                    new ThermalInputBatch.PageResidencyUpdate[]{update}, geometry.buildAndReset(),
                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN));
            helper.assertTrue(completion.status() == ThermalCompletion.Status.COMPLETED, "Capture the third Brick through the coordinator");
            near(helper, 0, temperature(helper, sim, third), "Admission alone does not create heat");
            helper.assertTrue(!requested(sim, fourth), "The newly captured cold third Brick cannot request the fourth");
            setTemperature(sim, origin.offset(11, 1, 1), 1.25);
            sim.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(requested(sim, fourth), "Once heated, the third Brick continues the same boundary rule");
            FHMain.LOGGER.info("MATERIAL_FRONTIER_SEQUENTIAL_GROWTH,thirdCaptured=true,fourthRequestedAfterHeating=true,noColdCascade=true");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "material_frontier_contacts", timeoutTicks = 300)
    public static void sharedMaterialCanHeatBothSidesAndShapesStillRequireContact(GameTestHelper helper) {
        var origin = new BlockPos(429056, 192, 425984);
        fill(helper, origin, 8, 4, 4);
        var left = origin.offset(2, 1, 1);
        var right = origin.offset(4, 1, 1);
        var fire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);
        helper.getLevel().setBlockAndUpdate(left, fire);
        helper.getLevel().setBlockAndUpdate(right, fire);
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var sim = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 8, 4, 4, 0)) {
            setTemperature(sim, left, 600);
            sim.tick = 1000;
            sim.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(temperature(helper, sim, right) > 1e-5, "The intervening stone no longer has an exclusive surface owner");
            int bottom = sim.profiles.states().signatureId(Blocks.STONE_SLAB.defaultBlockState());
            int top = sim.profiles.states().signatureId(Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP));
            near(helper, 0, sim.profiles.signatures().contactFraction(bottom, top, 1), "Opposite slab halves still have no direct contact");
            near(helper, .5, sim.profiles.signatures().contactFraction(bottom, bottom, 1), "Matching slab halves retain partial contact");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    private static void setTemperature(ThermalMaterialBehaviorGameTests.Simulation sim, BlockPos point, double c) {
        int slot = sim.slot(point);
        var sample = new MaterialSample();
        sim.material.readMaterial(slot, sample);
        sim.material.applyMaterialControl(slot, sample.law().enthalpyAtTemperature(c), sample.law().initialBranch(c));
    }

    private static double temperature(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation sim, BlockPos point) {
        var sample = new TiledThermalField.Sample();
        helper.assertTrue(sim.fixed.read(point.asLong(), true, sample), "Captured material is readable at " + point);
        return sample.temperatureC();
    }

    private static boolean requested(ThermalMaterialBehaviorGameTests.Simulation sim, BlockPos point) {
        Object pages = ThermalLoadedWorldGameTests.read(sim.engine, "pages");
        it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap masks = ThermalLoadedWorldGameTests.read(pages, "desiredBySection");
        int brick = (point.getX() & 15) >>> 2 | (point.getZ() & 12) | (point.getY() & 12) << 2;
        return (masks.get(SectionPos.asLong(point)) & 1L << brick) != 0;
    }

    private static void installHalo(ThermalMaterialBehaviorGameTests.Simulation sim, BlockPos origin, int width) {
        var geometry = new ResolvedGeometryBatch.Builder();
        geometry.addHalo(sim.page(origin), sim.page(origin).liveGeometryRevision(), halo(sim, origin, width, 4, 4));
        sim.publish(geometry.buildAndReset(), ThermalInputBatch.NO_PHASE_ACKS);
    }

    /** Capture only the immediate face shell; known neighbors are not force-retained by this fixture. */
    private static GeometryHalo halo(ThermalMaterialBehaviorGameTests.Simulation sim, BlockPos origin, int sx, int sy, int sz) {
        long[] positions = new long[2 * (sx * sy + sx * sz + sy * sz)];
        int n = 0;
        for (int x = -1; x <= sx; x++) for (int y = -1; y <= sy; y++) for (int z = -1; z <= sz; z++) {
            int outside = (x == -1 || x == sx ? 1 : 0) + (y == -1 || y == sy ? 1 : 0) + (z == -1 || z == sz ? 1 : 0);
            if (outside == 1) positions[n++] = origin.offset(x, y, z).asLong();
        }
        Arrays.sort(positions);
        int[] signatures = new int[positions.length];
        for (int i = 0; i < positions.length; i++) signatures[i] = sim.profiles.states().signatureId(sim.level.getBlockState(BlockPos.of(positions[i])));
        return new GeometryHalo(positions, signatures);
    }

    private static void fill(GameTestHelper helper, BlockPos origin, int sx, int sy, int sz) {
        for (int x = -1; x <= sx; x++) for (int y = -1; y <= sy; y++) for (int z = -1; z <= sz; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.STONE.defaultBlockState());
    }

    private static void near(GameTestHelper helper, double expected, double actual, String description) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)), description + " expected=" + expected + " actual=" + actual);
    }
}
