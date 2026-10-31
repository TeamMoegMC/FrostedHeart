/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.SharedAirGridCompiler;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_diagonal_projection")
@PrefixGameTestTemplate(false)
public final class DirectAirProjectionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void signedProjectionYieldsAndWritesOnlyTheAirPrefix(GameTestHelper helper) {
        int count = 2049;
        double[] mass = new double[count], rhs = new double[count], solution = new double[count + 2];
        Arrays.fill(solution, 99);
        for (int row = 0; row < count; row++) {
            mass[row] = Math.scalb(1, row % 8 - 4);
            rhs[row] = mass[row] * (row % 17 - 8);
        }
        var projection = new CoupledThermalOperator.ProjectionPreparation(mass, rhs, solution, count,
                ContinuousAirSolver.DEFAULT_CORRECTION_TOLERANCE_C);
        helper.assertTrue(!projection.run(0) && solution[count - 1] == 99,
                "An expired slice must yield before processing a large Air field");
        boolean done = false;
        for (int resume = 0; resume <= count && !done; resume++) done = projection.run(0);
        helper.assertTrue(done, "Every resumed slice must make progress");
        for (int row = 0; row < count; row++)
            helper.assertTrue(solution[row] == row % 17 - 8, "Signed nodal energy must produce the exact temperature");
        helper.assertTrue(solution[count] == 99 && solution[count + 1] == 99,
                "Air projection must not overwrite material entries");
        double[] empty = {17};
        helper.assertTrue(new CoupledThermalOperator.ProjectionPreparation(new double[0], new double[0], empty,
                0, 1e-5).run(0) && empty[0] == 17, "An empty Air system finishes without touching material state");
        double[] nearly = {20 + Math.scalb(1, -22)};
        new CoupledThermalOperator.ProjectionPreparation(new double[]{1}, new double[]{20}, nearly, 1, 1e-5).run(0);
        helper.assertTrue(nearly[0] == 20, "Direct calculation also resolves differences below the old warm-start threshold");
        double[] invalid = {17};
        boolean refused = false;
        try {
            new CoupledThermalOperator.ProjectionPreparation(new double[]{0}, new double[]{1}, invalid, 1, 1e-5).run(0);
        } catch (IllegalStateException failure) {
            refused = failure.getMessage().contains("row=0");
        }
        helper.assertTrue(refused && invalid[0] == 17, "Invalid mass reports its row without installing a non-finite value");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void assembledSharedGridRetainsAffineTemperatureAndTotalHeat(GameTestHelper helper) {
        var captured = new AirFieldLayout.Component(0, 1, -12, 64, -8, -1L,
                new int[]{0, 1, 2, 3, 4, 5, 6, 7}, new ThermalPageHandle[0], new long[0]);
        var bricks = new Long2ObjectOpenHashMap<AirFieldLayout.Component[]>();
        bricks.put(BlockPos.asLong(-12, 64, -8), new AirFieldLayout.Component[]{captured});
        var grid = new SharedAirGridCompiler().prepare(new AirFieldLayout(new AirFieldLayout.Component[]{captured}, bricks, 8));
        while (!grid.run(Long.MAX_VALUE)) { }
        var layout = grid.layout();
        var bindings = new WorkerPhysicalSourceBindings(null, ThermalSignatureTable.builder().build(),
                MinecraftPhysicalSourceProfile.CAMPFIRE, new AirLoadTable()).freeze();
        var volumes = new AirOperatorFragment.UnitGridPreparation(layout, 128, 1, bindings);
        while (!volumes.run(Long.MAX_VALUE)) { }
        var preparing = new CoupledThermalOperator.Preparation(layout, volumes.result,
                new CoupledThermalOperator.Contacts(), null, null);
        while (!preparing.run(Long.MAX_VALUE)) { }
        var operator = preparing.result();
        var component = layout.component(0);
        double[] expected = new double[layout.coefficientCount()], rhs = new double[expected.length];
        for (int basis = 0; basis < component.basisCount(); basis++) {
            int position = component.unitPosition(basis);
            expected[component.coefficientIndex(basis)] = 10 + (position & 7)
                    + 2 * (position >>> 6) - 3 * (position >>> 3 & 7);
        }
        for (var fragment : volumes.result) {
            long cells = fragment.component.airBlocks;
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells); cells &= cells - 1;
                for (int local = 0; local < 8; local++) {
                    int row = fragment.component.coefficientIndex(fragment.component.unitCorner(cell, local));
                    rhs[row] += fragment.capacityJPerK[local] * expected[row];
                }
            }
        }
        double[] solution = new double[expected.length]; Arrays.fill(solution, -1000);
        var projection = operator.prepareProjection(rhs, solution, 1e-5);
        while (!projection.run(0)) { }
        helper.assertTrue(Arrays.equals(solution, expected), "The compiled grid must preserve every affine nodal value");
        double capacity = 0, energy = 0;
        for (int row = 0; row < solution.length; row++) {
            capacity += operator.massJPerK[row]; energy += operator.massJPerK[row] * solution[row];
        }
        helper.assertTrue(capacity == 64 * 128 && Math.abs(energy - 81920) < 1e-8,
                "A 64-cell cube has 8192 J/K capacity and 81920 J at this field's 10 C mean");
        helper.assertTrue(component.temperatureOffsetC(-10.75, 66.5, -7.25, solution) == 14,
                "The projected affine field must also interpolate correctly inside a cell");
        helper.succeed();
    }
}
