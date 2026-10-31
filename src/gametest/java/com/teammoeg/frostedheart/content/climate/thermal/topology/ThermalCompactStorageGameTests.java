/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import java.util.Arrays;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MinimumThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQuerySnapshot;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_compact_storage")
@PrefixGameTestTemplate(false)
public final class ThermalCompactStorageGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void legacyFunctionCheckpointKeepsSignedFieldAndBranchAcrossSectionSlices(GameTestHelper helper) {
        long branch = 0x1234_5678_9ABC_DEF0L;
        var shape = new CompoundTag();
        shape.putLong("origin", BlockPos.asLong(15, 64, 0));
        shape.putIntArray("size", new int[]{2, 1, 1});
        shape.putLong("source", BlockPos.asLong(14, 64, 0));
        shape.putByte("face", (byte) 1);
        shape.putInt("scale", 2);
        shape.putLong("branch", branch);
        shape.putDouble("amplitude", -2);
        long[] corners = new long[4];
        int[] values = new int[16];
        for (int vertex = 0; vertex < values.length; vertex++) {
            corners[vertex >>> 2] |= (long) vertex << ((vertex & 3) * 16);
            int corner = vertex & 7;
            values[vertex] = Float.floatToIntBits((vertex < 8 ? 0 : 10)
                    + (corner & 1) * 2 + (corner >>> 1 & 1) * 4 + (corner >>> 2) * 8);
        }
        shape.putLongArray("corners", corners);
        shape.putIntArray("values", values);
        for (int page = 0; page < 2; page++) {
            var tag = new CompoundTag();
            tag.putLong("origin", BlockPos.asLong(page * 16, 64, 0));
            var shapes = new ListTag(); shapes.add(shape.copy()); tag.put("shapes", shapes);
            var cell = new CompoundTag();
            cell.putByte("brick", (byte) (page == 0 ? 3 : 0));
            cell.putLong("blocks", page == 0 ? 1L << 3 : 1);
            cell.putLong("sample_tick", 123);
            long[] coarse = new long[8]; Arrays.fill(coarse, Double.doubleToLongBits(10));
            cell.putLongArray("coarse", coarse); cell.putIntArray("modes", new int[]{0});
            var cells = new ListTag(); cells.add(cell); tag.put("components", cells);
            var decoded = AirFieldCheckpoint.decode(tag);
            helper.assertTrue(decoded != null, "Existing function checkpoint must decode");
            double x = 15.25 + page, expected = page == 0 ? -5 : -25;
            exact(helper, expected, decoded.sampleC(x, 64.5, .75), "Signed legacy amplitude must preserve the actual field");
            var encoded = decoded.encode();
            var savedShape = encoded.getList("shapes", 10).getCompound(0);
            helper.assertTrue(Arrays.equals(savedShape.getIntArray("size"), new int[]{1, 1, 1}),
                    "Each page saves only its own shape slice");
            helper.assertTrue(savedShape.getLong("origin") == BlockPos.asLong(15 + page, 64, 0)
                            && savedShape.getLong("branch") == branch
                            && savedShape.getLong("source") == shape.getLong("source")
                            && savedShape.getByte("face") == 1 && savedShape.getInt("scale") == 2,
                    "Slicing must retain the source identity and full branch value");
            var restored = AirFieldCheckpoint.decode(encoded);
            helper.assertTrue(restored != null, "Section slice must decode again");
            exact(helper, expected, restored.sampleC(x, 64.5, .75), "Re-encoding must preserve both sides of the section boundary");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void backingInventoryKeepsNestedArraysAndClearsSharedVisits(GameTestHelper helper) {
        int[] integers = {1, 2, 3};
        double[] values = {1, 2};
        String[] labels = {"first", "second"};
        Object[] objects = {integers, values, labels};
        Cloneable[] cloneable = {integers, objects};
        java.io.Serializable[] serializable = {cloneable, values};
        try (var meter = new AirBackingMeter()) {
            meter.array(serializable);
            helper.assertTrue(meter.bytes() == 200, "All six unique nested arrays retain their aligned backing charges");
            meter.array(objects); meter.array(integers);
            helper.assertTrue(meter.bytes() == 200, "Aliases do not add a second charge");
            meter.clear();
            helper.assertTrue(meter.bytes() == 0 && meter.entries() == 0, "A finished inventory retains no visited objects");
            meter.array(serializable);
            helper.assertTrue(meter.bytes() == 200, "The next inventory counts the same live backing again");
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void localGridAddressesPreserveDisconnectedVerticesAndAffineField(GameTestHelper helper) {
        long checkerboard = 0;
        for (int cell = 0; cell < 64; cell++)
            if (((cell & 3) + (cell >>> 2 & 3) + (cell >>> 4)) % 2 == 0) checkerboard |= 1L << cell;
        var components = new AirFieldLayout.Component[]{component(0, -12, -1L), component(1, 8, checkerboard)};
        var bricks = new Long2ObjectOpenHashMap<AirFieldLayout.Component[]>();
        for (var component : components)
            bricks.put(BlockPos.asLong(component.minX, component.minY, component.minZ), new AirFieldLayout.Component[]{component});
        var captured = new AirFieldLayout(components, bricks, 16);
        var preparation = new SharedAirGridCompiler().prepare(captured);
        while (!preparation.run(Long.MAX_VALUE)) { }
        var layout = preparation.layout();
        helper.assertTrue(layout.component(0).basisCount() == 125, "Full cube has 125 connected vertices");
        helper.assertTrue(layout.component(1).basisCount() == 256, "Coincident disconnected vertices retain distinct identities");
        double[] field = new double[layout.coefficientCount()];
        for (int i = 0; i < layout.componentCount(); i++) {
            var component = layout.component(i);
            for (int basis = 0; basis < component.basisCount(); basis++) {
                int position = component.unitPosition(basis);
                field[component.coefficientIndex(basis)] = affine(component.minX + (position & 7),
                        component.minY + (position >>> 6), component.minZ + (position >>> 3 & 7));
            }
            int[] savedCorners = component.unitCornersCopy();
            helper.assertTrue(savedCorners.length == 512, "Existing v6 corner array stays complete");
            for (int cell = 0; cell < 64; cell++) {
                boolean present = (component.airBlocks & 1L << cell) != 0;
                for (int corner = 0; corner < 8; corner++) {
                    int local = savedCorners[cell * 8 + corner];
                    helper.assertTrue(present ? local >= 0 && local < component.basisCount() : local == -1,
                            "All local addresses and absent sentinels survive expansion");
                }
                if (!present) continue;
                for (int corner = 0; corner < 8; corner++) {
                    double vx = component.minX + (cell & 3) + (corner & 1);
                    double vy = component.minY + (cell >>> 4) + (corner >>> 2);
                    double vz = component.minZ + (cell >>> 2 & 3) + (corner >>> 1 & 1);
                    exact(helper, affine(vx, vy, vz), component.temperatureOffsetAtVertex(cell, corner, field),
                            "Known-cell vertex lookup preserves the affine field");
                }
                double x = component.minX + (cell & 3) + .25;
                double y = component.minY + (cell >>> 4) + .5;
                double z = component.minZ + (cell >>> 2 & 3) + .75;
                exact(helper, affine(x, y, z), component.temperatureOffsetAtAirCell(cell, x, y, z, field),
                        "Affine interpolation is unchanged at every occupied cell");
            }
        }
        // Both reuse paths keep immutable local addresses while global rows may move.
        var retained = captured.withRetainedUnitGrid(layout);
        var combined = AirFieldLayout.combine(java.util.List.of(layout));
        helper.assertTrue(retained.component(1).unitCorner(62, 7) == layout.component(1).unitCorner(62, 7), "Retained layout keeps corner identity");
        helper.assertTrue(combined.layout().component(1).coefficientIndex(255) == layout.component(1).coefficientIndex(255),
                "Combined rows retain global addresses beyond the local vertex range");
        try (var meter = new AirBackingMeter()) {
            layout.recordBacking(meter); long first = meter.bytes();
            layout.recordBacking(meter);
            helper.assertTrue(meter.bytes() == first, "Repeated solver/query aliases retain one layout charge");
            retained.recordBacking(meter);
            helper.assertTrue(meter.bytes() > first, "A new immutable layout still charges its own indexes");
            meter.clear(); layout.recordBacking(meter);
            helper.assertTrue(meter.bytes() == first, "Layout visits are confined to one inventory");
        }
        try (var summary = new AirBackingMeter(true); var detail = new AirBackingMeter()) {
            layout.recordBacking(summary); layout.recordBacking(detail);
            helper.assertTrue(summary.bytes() == detail.bytes(), "One layout's cached capacity matches its arrays");
            long first = summary.bytes();
            layout.recordBacking(summary);
            helper.assertTrue(summary.bytes() == first, "Repeated use of one layout does not accumulate estimates");
            retained.recordBacking(summary); retained.recordBacking(detail);
            helper.assertTrue(summary.bytes() >= detail.bytes(), "Shared successor layouts use a conservative estimate");
            summary.clear(); layout.recordBacking(summary);
            helper.assertTrue(summary.bytes() == first, "An old reader's immutable layout keeps the same capacity");
        }
        // Diagonal cells can touch at a coordinate while retaining different physical vertices.
        var disconnected = layout.component(1);
        double[] branches = new double[layout.coefficientCount()];
        for (int cell = 0; cell < 64; cell++) if ((checkerboard & 1L << cell) != 0)
            for (int corner = 0; corner < 8; corner++)
                branches[disconnected.coefficientIndex(disconnected.unitCorner(cell, corner))] = 100 + cell;
        for (int cell = 0; cell < 64; cell++) if ((checkerboard & 1L << cell) != 0)
            for (int corner = 0; corner < 8; corner++)
                exact(helper, 100 + cell, disconnected.temperatureOffsetAtVertex(cell, corner, branches),
                        "A coincident vertex must use its own Air-side branch");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void activityUsesPerturbationAndPreservesBothHysteresisThresholds(GameTestHelper helper) {
        var arena = new ThermalCellArena(8);
        var cells = new ThermalBrickCellLayout(); cells.reset(0, 64, -8); cells.setRegularAir(1);
        var span = arena.stageBrickCells(0, 1, cells, 0, 0, 8); arena.commitStagedCells(span);
        var captured = component(span.firstSlot(), 0, -1L);
        var bricks = new Long2ObjectOpenHashMap<AirFieldLayout.Component[]>();
        bricks.put(BlockPos.asLong(0, 64, -8), new AirFieldLayout.Component[]{captured});
        var grid = new SharedAirGridCompiler().prepare(new AirFieldLayout(new AirFieldLayout.Component[]{captured}, bricks, 8));
        while (!grid.run(Long.MAX_VALUE)) { }
        var layout = grid.layout(); var component = layout.component(0);
        var preparing = new RegionalQuerySnapshot.TemplatePreparation(new RegionalQuerySnapshot.Handle(1), layout,
                new int[0], arena, slot -> 0);
        while (!preparing.run(Long.MAX_VALUE)) { }
        double[] backgroundC = new double[layout.coefficientCount()];
        for (int basis = 0; basis < component.basisCount(); basis++) {
            int position = component.unitPosition(basis);
            backgroundC[component.coefficientIndex(basis)] = 20 + (position & 7) + .25 * (position >>> 6);
        }
        double[] capacities = new double[MinimumThermalState.MAXIMUM_NODES];
        Arrays.fill(capacities, 0, MinimumThermalState.AIR_NODES, 1);
        double[] nodeBackground = new double[capacities.length];
        for (int node = 0; node < MinimumThermalState.AIR_NODES; node++)
            nodeBackground[node] = 20 + (node & 3) * 4.0 / 3 + .25 * (node >>> 4) * 4.0 / 3;
        var background = new MinimumThermalState.Background(backgroundC, nodeBackground, new int[0], new double[0], 0);
        var minimumLayout = new MinimumThermalState.Layout(0, 64, -8, 4, 4, 4, capacities, new double[capacities.length],
                new double[capacities.length], new int[0], new int[0], new double[0], new int[0], new MaterialThermalLaw[0], background);
        for (double perturbation : new double[]{0, .5, -.5, .75, 1, -1}) {
            double[] values = backgroundC.clone();
            for (int index = 0; index < values.length; index++) values[index] += perturbation;
            double[] energy = nodeBackground.clone();
            for (int node = 0; node < MinimumThermalState.AIR_NODES; node++) energy[node] += perturbation;
            var minimum = new MinimumThermalState(minimumLayout, energy, new byte[0], new boolean[0]);
            var fine = new RegionalQuerySnapshot(preparing.result(), values, new double[0], new byte[0], new boolean[0],
                    arena, 0, 20, 1, null).withMinimumSummary(minimum, null); // Air-only: no material anchor.
            var coarse = fine.activatePreparedMinimum();
            for (var field : new RegionalQuerySnapshot[]{fine, coarse}) {
                for (double threshold : new double[]{.5, 1})
                    helper.assertTrue(field.activeAtSlot(span.firstSlot(), -40, threshold) == (Math.abs(perturbation) >= threshold),
                            "Activity follows perturbation, including equality at both hysteresis thresholds");
                for (int cell = 0; cell < 64; cell++) for (int corner = 0; corner < 8; corner++)
                    helper.assertTrue(Math.abs(field.vertexPerturbationC(component, cell, corner, -40) - perturbation) < 1e-12,
                            "Fine and coarse vertices subtract the represented background");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void materialActivityRetainsLatentHeatAndPendingLayout(GameTestHelper helper) {
        var arena = new ThermalCellArena(8);
        var cells = new ThermalBrickCellLayout(); cells.reset(0, 64, 0);
        cells.addMaterialCell(0, 64, 0, 100, 0); cells.addMaterialCell(1, 64, 0, 100, 0);
        var span = arena.stageBrickCells(0, 1, cells, 0, 0, 8);
        int sensible = span.firstSlot(), latent = sensible + 1;
        arena.stageMaterialLaw(sensible, 1, MaterialThermalLaw.sensible(100), 0);
        arena.stageMaterialLaw(latent, 2, new MaterialThermalLaw(100, 0,
                new MaterialThermalLaw.Transition(1, 0, 0, 1000), null), -1);
        arena.commitStagedCells(span);
        var layout = new AirFieldLayout(new AirFieldLayout.Component[0], new Long2ObjectOpenHashMap<>(), 0);
        var preparing = new RegionalQuerySnapshot.TemplatePreparation(new RegionalQuerySnapshot.Handle(1), layout,
                new int[]{sensible, latent}, arena, slot -> 0);
        while (!preparing.run(Long.MAX_VALUE)) { }
        for (double temperature : new double[]{0, .5, 1, -1}) {
            var field = new RegionalQuerySnapshot(preparing.result(), new double[0], new double[]{temperature * 100, 500},
                    new byte[]{MaterialThermalLaw.SENSIBLE, MaterialThermalLaw.HEATING}, new boolean[2], arena, 0, 20, 1, null);
            helper.assertTrue(field.activeAtSlot(sensible, 0, 1) == (Math.abs(temperature) >= 1), "Sensible material uses the entry threshold");
            helper.assertTrue(field.activeAtSlot(sensible, 0, .5) == (Math.abs(temperature) >= .5), "Sensible material uses the release threshold");
            helper.assertTrue(field.activeAtSlot(latent, 0, 1), "Latent progress remains active even on the ambient plateau");
        }
        arena.awaitMaterialLayout(sensible);
        var pending = new RegionalQuerySnapshot(preparing.result(), new double[0], new double[]{0, 0}, new byte[2],
                new boolean[2], arena, 0, 40, 1, null);
        helper.assertTrue(pending.activeAtSlot(sensible, 0, 1), "Pending material replacement must remain active");
        helper.assertTrue(!pending.activeAtSlot(7, 0, 1), "A slot outside the snapshot has no activity");
        helper.succeed();
    }

    private static AirFieldLayout.Component component(int slot, int x, long mask) {
        int[] coarse = new int[8];
        for (int i = 0; i < 8; i++) coarse[i] = slot * 8 + i;
        return new AirFieldLayout.Component(slot, 1, x, 64, -8, mask, coarse, new ThermalPageHandle[0], new long[0]);
    }
    private static double affine(double x, double y, double z) { return x * .5 + y * .25 - z * .125; }

    private static void exact(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Double.doubleToRawLongBits(expected) == Double.doubleToRawLongBits(actual), message);
    }
}
