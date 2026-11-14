/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Local Q1 storage requirements; complete field exchange is a separate adoption gate. */
@GameTestHolder("frostedheart_brick_air")
@PrefixGameTestTemplate(false)
public final class BrickAirStateGameTests {
    private static final double[] FRACTIONS = {.125, .5, .875};
    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void actualAdjacentAirCubesReproduceFractionalAffineFieldsAndCapacity(GameTestHelper helper) {
        var base = new BlockPos(118784, 192, 118784);
        var profiles = MinecraftThermalProfiles.prepare();
        double capacity = profiles.tuning().airHeatCapacityJPerBlockK(), totalJ = 0, cellJ = 0;
        int samples = 0;
        for (int brick = 0; brick < 2; brick++) {
            long mask = 0;
            for (int cell = 0; cell < 64; cell++) {
                var position = base.offset(brick * 4 + (cell & 3), cell >>> 4, cell >>> 2 & 3);
                helper.getLevel().setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                if (profiles.signatures().isAir(profiles.states().signatureId(helper.getLevel().getBlockState(position)))) mask |= 1L << cell;
            }
            helper.assertTrue(mask == -1L, "Capture both adjacent cubes from actual Air BlockStates");
            var state = new BrickAirState(mask, capacity, 0);
            for (int node = 0; node < state.nodeCount(); node++) state.enthalpyJ(node,
                    state.capacityJPerK(node) * affine(brick * 4 + state.vertexX(node), state.vertexY(node), state.vertexZ(node)));
            double ownedCapacity = 0;
            for (int node = 0; node < state.nodeCount(); node++) ownedCapacity += state.capacityJPerK(node);
            near(helper, 64 * capacity, ownedCapacity, "Boundary corners own this cube's capacity share only");
            for (int cell = 0; cell < 64; cell++) {
                int x = brick * 4 + (cell & 3), y = cell >>> 4, z = cell >>> 2 & 3;
                cellJ += state.cellEnthalpyJ(cell);
                for (double fx : FRACTIONS) for (double fy : FRACTIONS) for (double fz : FRACTIONS) {
                        near(helper, affine(x + fx, y + fy, z + fz), state.sampleC(cell, fx, fy, fz),
                                "Q1 retains variation inside a cell at the actual sample position");
                        samples++;
                    }
            }
            totalJ += state.totalEnthalpyJ();
            helper.assertTrue(state.retainedBytes() < 4096, "A full Air cube has bounded local value storage");
        }
        near(helper, 128 * capacity * affine(4, 2, 2), totalJ, "Adjacent cubes preserve the analytic integral without double counting");
        near(helper, totalJ, cellJ, "All cell shares sum to the stored corner energy");
        FHMain.LOGGER.info("THERMAL_BRICK_AIR_AFFINE,samples={},capacityPerCell={},totalJ={},cellShareJ={},globalGraph=false", samples, capacity, totalJ, cellJ);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void coincidentDisconnectedCornersDoNotMixHeatAndFramesDoNotAlias(GameTestHelper helper) {
        int first = 1 | 1 << 2 | 1 << 4, second = 2 | 2 << 2 | 2 << 4;
        long mask = 1L << first | 1L << second;
        var accepted = new BrickAirState(mask, 1200, 0);
        int a = accepted.node(first, 7), b = accepted.node(second, 0);
        helper.assertTrue(a != b && accepted.vertex(a) == accepted.vertex(b),
                "Two cells touching at one geometric corner retain separate physical Air branches");
        for (int corner = 0; corner < 8; corner++) {
            int node = accepted.node(first, corner); accepted.enthalpyJ(node, accepted.capacityJPerK(node) * 10);
            node = accepted.node(second, corner); accepted.enthalpyJ(node, accepted.capacityJPerK(node) * 90);
        }
        var working = new BrickAirState(mask, 1200, -10); working.copyFrom(accepted);
        working.addEnergyJ(a, 800);
        near(helper, 10, accepted.sampleC(first, .875, .875, .875), "A working update leaves Accepted readable");
        near(helper, 90, working.sampleC(second, .125, .125, .125), "Disconnected coincident corners transfer no J");
        near(helper, 120000 + 800, working.totalEnthalpyJ(), "The working update changes the sole owned total by its input J");
        working.copyFrom(accepted);
        near(helper, accepted.totalEnthalpyJ(), working.totalEnthalpyJ(), "Reusing a cancelled frame restores the accepted prefix");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void aSharedCornerCombinesCapacityOnceAndEquilibratesConservatively(GameTestHelper helper) {
        var left = new BrickAirState(-1L, 1200, 10); var right = new BrickAirState(-1L, 1200, 30);
        int a = left.nodeAtVertex(4, 2, 2, 1), b = right.nodeAtVertex(0, 2, 2, 0);
        double before = left.totalEnthalpyJ() + right.totalEnthalpyJ();
        var corner = new BrickAirState.Corner();
        try {
            corner.add(left, a); corner.add(right, b); corner.add(left, a);
            helper.assertTrue(corner.partCount() == 2, "Repeated incident cells do not count a local capacity share twice");
            near(helper, 1200, corner.capacityJPerK(), "The shared interior corner owns eight cell capacity shares");
            near(helper, 20, corner.temperatureC(), "Independent saved shares rejoin at their capacity-weighted temperature");
            corner.addEnergyJ(600);
            near(helper, 20.5, left.temperatureC(a), "The source changes both sides of the shared physical corner");
            near(helper, left.temperatureC(a), right.temperatureC(b), "Both cubes observe the same shared temperature");
            near(helper, before + 600, left.totalEnthalpyJ() + right.totalEnthalpyJ(), "Joining and adding J conserve the whole field");
        } finally { corner.clear(); }
        helper.assertTrue(corner.partCount() == 0 && corner.capacityJPerK() == 0,
                "The reusable view releases state references and capacity after the operation");
        helper.succeed();
    }

    private static double affine(double x, double y, double z) { return 7 + 2 * x - 3 * y + .5 * z; }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-10 * Math.max(1, Math.abs(expected)),
                message + "; expected=" + expected + ",actual=" + actual);
    }
}
