/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.Arrays;
import java.util.TreeMap;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalBrickCellLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources;
import com.teammoeg.frostedheart.content.climate.thermal.topology.SharedAirGridCompiler;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_compact_thermal_data")
@PrefixGameTestTemplate(false)
public final class CompactThermalDataGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void compactFacesPreserveOrderedExchangePhaseAndEmptyPoints(GameTestHelper helper) throws ReflectiveOperationException {
        var layout = grid(); var fragments = fragments(layout);
        var compact = new CoupledThermalOperator.Contacts(); var expanded = new CoupledThermalOperator.Contacts();
        int[][] faces = {{-1, 1, 2, 3}, {4, -1, 6, 7}};
        for (int face = 0; face < faces.length; face++) {
            compact.surface(face, 1.75, faces[face]);
            for (int node : faces[face]) {
                if (node >= 0) expanded.term(node, 1);
                expanded.finishMaterialSurface(1.75, face, null);
            }
        }
        for (var contacts : new CoupledThermalOperator.Contacts[]{compact, expanded}) {
            contacts.term(3, .5); contacts.term(4, -.5); contacts.finish(3, -1, null);
            contacts.term(5, 1); contacts.finish(2, 0, null);
        }
        var arena = new ThermalCellArena(4);
        var cells = new ThermalBrickCellLayout(); cells.reset(0, 64, 0);
        cells.addMaterialCell(0, 64, 0, 100, 0); cells.addMaterialCell(1, 64, 0, 100, 0);
        var span = arena.stageBrickCells(0, 1, cells, 0, 0, 4);
        int[] slots = {span.firstSlot(), span.firstSlot() + 1};
        arena.stageMaterialLaw(slots[0], 0, MaterialThermalLaw.sensible(100), 0);
        arena.stageMaterialLaw(slots[1], 0, new MaterialThermalLaw(100, 0,
                new MaterialThermalLaw.Transition(0, 0, 0, 10), null), 0);
        var a = operator(layout, fragments, compact); var b = operator(layout, fragments, expanded);
        bind(a, layout, compact, arena, slots); bind(b, layout, expanded, arena, slots);
        helper.assertTrue(a.pairwiseContactCount() == b.pairwiseContactCount(), "Four logical point exchanges remain per compact face");
        double[] ta = new double[layout.coefficientCount()]; Arrays.fill(ta, 80); ta[0] = -10;
        double[] tb = ta.clone(), ha = {100, -1}, hb = ha.clone(); byte[] ba = new byte[2], bb = ba.clone();
        a.freezeEnvironment(1.25, page -> 20, 10); b.freezeEnvironment(1.25, page -> 20, 10);
        a.refreshContacts(null, 10, 0, a.pairwiseContactCount()); b.refreshContacts(null, 10, 0, b.pairwiseContactCount());
        for (int step = 0; step < 8; step++) {
            boolean reverse = (step & 1) != 0;
            for (int ordinal = 0; ordinal < a.pairwiseContactCount(); ordinal++) {
                a.sweepPairwiseContacts(ta, ha, ba, .5, 10, reverse, ordinal, ordinal + 1);
                b.sweepPairwiseContacts(tb, hb, bb, .5, 10, reverse, ordinal, ordinal + 1);
                helper.assertTrue(Arrays.equals(ta, tb) && Arrays.equals(ha, hb) && Arrays.equals(ba, bb)
                                && Arrays.equals(a.materialBoundary.trialWaiting, b.materialBoundary.trialWaiting),
                        "Compact and expanded faces must agree after every forward/reverse exchange, including an empty point");
            }
        }
        helper.assertTrue(a.materialBoundary.trialWaiting[1], "The fixture must reach and wait for an actual phase boundary");
        long compactBytes, expandedBytes;
        try (var meter = new AirBackingMeter()) { a.recordBacking(meter); compactBytes = meter.bytes(); }
        try (var meter = new AirBackingMeter()) { b.recordBacking(meter); expandedBytes = meter.bytes(); }
        helper.assertTrue(compactBytes < expandedBytes, "Compact faces must retain less array backing, including frozen state");
        FHMain.LOGGER.info("THERMAL_COMPACT_FACES,exchanges={},temperatureAndEnthalpyExact=true,phaseWaiting=true,compactBytes={},expandedBytes={}",
                a.pairwiseContactCount() * 8, compactBytes, expandedBytes);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void componentVolumesPreserveMassMixedEdgesAndReuse(GameTestHelper helper) throws ReflectiveOperationException {
        var layout = grid(); var fragments = fragments(layout);
        helper.assertTrue(fragments.length == layout.componentCount(), "Only one volume description is retained per component");
        var operator = operator(layout, fragments, new CoupledThermalOperator.Contacts());
        double[] mass = new double[layout.coefficientCount()], temperatures = new double[mass.length];
        for (int i = 0; i < temperatures.length; i++) temperatures[i] = i % 23 - 8;
        var edges = new TreeMap<Long, Double>(); int cellCount = 0, mixedCount = 0;
        for (int part = 0; part < layout.componentCount(); part++) {
            var c = layout.component(part);
            double factor = operator.buoyancyFactors[part] = fragments[part].buoyancyFactor(temperatures, new BuoyancyConductance.Parameters(.25, 4, 10));
            for (int cell = 0; cell < 64; cell++) if ((c.airBlocks & 1L << cell) != 0) {
                cellCount++;
                double dx = c.minX + (cell & 3) + .5 - (-7.5), dy = c.minY + (cell >>> 4) + .5 - 64.5,
                        dz = c.minZ + (cell >>> 2 & 3) + .5 - .5;
                boolean mixed = dx * dx + dy * dy + dz * dz <= 16;
                if (mixed) mixedCount++;
                double coefficient = (mixed ? AirMixingRegion.MULTIPLIER : 1) / 4.0;
                for (int a = 0; a < 8; a++) {
                    int first = c.coefficientIndex(c.unitCorner(cell, a)); mass[first] += 16;
                    for (int b = a + 1; b < 8; b++) {
                        if (Integer.bitCount(a ^ b) != 1) continue;
                        int second = c.coefficientIndex(c.unitCorner(cell, b));
                        long pair = (long) Math.min(first, second) << 32 | Math.max(first, second);
                        double horizontal = (a ^ b) == 4 ? 0 : -coefficient, vertical = (a ^ b) == 4 ? -coefficient : 0;
                        edges.put(pair, edges.getOrDefault(pair, 0.0) - (horizontal + factor * vertical));
                    }
                }
            }
        }
        helper.assertTrue(mixedCount > 0 && mixedCount < cellCount, "Both ordinary and enhanced cells must occur");
        helper.assertTrue(Arrays.equals(mass, operator.massJPerK), "Component storage retains every cell's original nodal mass addition");
        operator.clearPairwiseEdges(0, operator.pairwiseEdgeCount());
        for (int i = 0; i < fragments.length; i++) operator.accumulatePairwiseVolumes(i, i + 1);
        int[] first = (int[]) field(operator, "transportFirst"), second = (int[]) field(operator, "transportSecond");
        double[] conductance = (double[]) field(operator, "transportOffDiagonal"); int edge = 0;
        helper.assertTrue(edges.size() == first.length, "All unique axial edges must remain");
        for (var entry : edges.entrySet()) {
            helper.assertTrue(first[edge] == (int) (entry.getKey() >>> 32) && second[edge] == entry.getKey().intValue()
                            && Double.doubleToRawLongBits(conductance[edge]) == Double.doubleToRawLongBits(entry.getValue()),
                    "Mixed conductance contributions retain exact per-cell accumulation order");
            edge++;
        }
        var preparing = new CoupledThermalOperator.Preparation(layout, fragments(layout), new CoupledThermalOperator.Contacts(), operator,
                () -> { throw new AssertionError("Unchanged geometry must reuse its pair graph"); });
        while (!preparing.run(0)) { }
        helper.assertTrue(field(preparing.result(), "transportFirst") == first, "Graph reuse transfers the original endpoint array");
        FHMain.LOGGER.info("THERMAL_COMPACT_VOLUMES,components={},cells={},mixedCells={},edges={},massAndConductanceExact=true,graphReused=true",
                fragments.length, cellCount, mixedCount, first.length);
        helper.succeed();
    }

    private static AirFieldLayout grid() {
        var parts = new AirFieldLayout.Component[2]; var bricks = new Long2ObjectOpenHashMap<AirFieldLayout.Component[]>();
        for (int i = 0; i < parts.length; i++) {
            int[] indices = new int[8]; for (int n = 0; n < 8; n++) indices[n] = i * 8 + n;
            var part = parts[i] = new AirFieldLayout.Component(i, 1, -8 + i * 4, 64, 0,
                    i == 0 ? -1L : 0x80ff00ff00ff00ffL, indices, new ThermalPageHandle[0], new long[0]);
            bricks.put(BlockPos.asLong(part.minX, part.minY, part.minZ), new AirFieldLayout.Component[]{part});
        }
        var preparing = new SharedAirGridCompiler().prepare(new AirFieldLayout(parts, bricks, 16));
        while (!preparing.run(0)) { }
        return preparing.layout();
    }

    private static AirOperatorFragment[] fragments(AirFieldLayout layout) throws ReflectiveOperationException {
        var bySection = new Long2ObjectOpenHashMap<int[]>(); bySection.put(SectionPos.asLong(-1, 4, 0), new int[]{0});
        var constructor = FrozenSources.class.getDeclaredConstructor(long[].class, byte[].class, Long2ObjectOpenHashMap.class,
                LongOpenHashSet.class, long.class, ThermalSignatureTable.class); constructor.setAccessible(true);
        var sources = constructor.newInstance(new long[]{BlockPos.asLong(-8, 64, 0)}, new byte[]{0}, bySection,
                new LongOpenHashSet(), 1L, ThermalSignatureTable.builder().build());
        var preparing = new AirOperatorFragment.UnitGridPreparation(layout, 128, 1, sources);
        while (!preparing.run(0)) { }
        return preparing.result;
    }

    private static CoupledThermalOperator operator(AirFieldLayout layout, AirOperatorFragment[] fragments, CoupledThermalOperator.Contacts contacts) {
        var preparing = new CoupledThermalOperator.Preparation(layout, fragments, contacts, null, null);
        while (!preparing.run(0)) { }
        return preparing.result();
    }
    private static void bind(CoupledThermalOperator operator, AirFieldLayout layout, CoupledThermalOperator.Contacts contacts,
            ThermalCellArena arena, int[] slots) {
        var unknowns = new Int2IntOpenHashMap(); unknowns.defaultReturnValue(-1);
        for (int i = 0; i < slots.length; i++) unknowns.put(slots[i], layout.coefficientCount() + i);
        var preparing = new LocalMaterialBoundary.Preparation(layout, slots, unknowns, arena, contacts, operator, new ThermalFragment[0]);
        while (!preparing.run(0)) { }
        operator.materialBoundary = preparing.result;
    }
    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        var field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }
}
