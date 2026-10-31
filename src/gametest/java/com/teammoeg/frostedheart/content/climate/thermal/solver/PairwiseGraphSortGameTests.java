/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeMap;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.LongRadixSortCursor;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.SharedAirGridCompiler;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_pairwise_sort")
@PrefixGameTestTemplate(false)
public final class PairwiseGraphSortGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void packedKeysAndFullPairedKeysRetainTheirExactOrder(GameTestHelper helper) {
        Random random = new Random(9127);
        int cases = 0;
        for (int nodeBits = 1; nodeBits <= 31; nodeBits++) for (int positionBits = 0; positionBits <= 31; positionBits++) {
            if (2 * nodeBits + positionBits > 63) continue;
            int count = Math.min(257, 1 << Math.min(9, Math.max(0, positionBits - 1)));
            long mask = (1L << nodeBits) - 1;
            long[] keys = new long[count];
            for (int i = 0; i < count; i++) {
                long a = random.nextLong() & mask, b = random.nextLong() & mask;
                keys[i] = ((Math.min(a, b) << nodeBits | Math.max(a, b)) << positionBits)
                        | (positionBits == 0 ? 0 : i * 2L);
            }
            long[] expected = keys.clone(); Arrays.sort(expected);
            var sort = new LongRadixSortCursor(keys, null, positionBits, nodeBits * 2);
            finish(helper, sort);
            helper.assertTrue(Arrays.equals(sort.sortedKeys(), expected),
                    "Packed keys must preserve full-key order at nodeBits=" + nodeBits + ",positionBits=" + positionBits);
            cases++;
        }
        var empty = new LongRadixSortCursor(new long[0], null, 0, 0);
        helper.assertTrue(empty.run(0) && empty.sortedKeys().length == 0, "An empty zero-bit graph completes immediately");
        var emptyPaired = new LongRadixSortCursor(new long[0], new long[0], 0, Long.SIZE);
        finish(helper, emptyPaired);
        helper.assertTrue(emptyPaired.sortedPaired().length == 0, "An empty full-key graph retains both columns");
        long[] keys = new long[9001], paired = new long[keys.length];
        Integer[] order = new Integer[keys.length];
        for (int i = 0; i < keys.length; i++) { keys[i] = random.nextInt(31) - 15; paired[i] = order[i] = i; }
        keys[0] = Long.MIN_VALUE; keys[1] = Long.MAX_VALUE;
        long[] original = keys.clone();
        Arrays.sort(order, Comparator.comparingLong(index -> original[index]));
        var full = new LongRadixSortCursor(keys, paired, 0, Long.SIZE);
        helper.assertTrue(!full.run(0), "A large paired input must yield within a radix pass");
        finish(helper, full);
        for (int i = 0; i < keys.length; i++) helper.assertTrue(full.sortedKeys()[i] == original[order[i]]
                        && full.sortedPaired()[i] == order[i], "Full-key sorting must retain stable paired addresses at " + i);
        FHMain.LOGGER.info("THERMAL_PAIRWISE_SORT,packedBitRanges={},pairedEntries={},exact=true", cases, keys.length);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void realVolumeGraphsMatchAnIndependentOrderedEdgeMap(GameTestHelper helper) throws ReflectiveOperationException {
        var components = new AirFieldLayout.Component[8];
        var bricks = new Long2ObjectOpenHashMap<AirFieldLayout.Component[]>();
        for (int i = 0; i < components.length; i++) {
            int[] indices = new int[8];
            for (int corner = 0; corner < 8; corner++) indices[corner] = i * 8 + corner;
            var c = components[i] = component(i, -12 + (i & 1) * 4, 64 + (i >>> 2) * 4, -8 + (i >>> 1 & 1) * 4, indices);
            bricks.put(BlockPos.asLong(c.minX, c.minY, c.minZ), new AirFieldLayout.Component[]{c});
        }
        var grid = new SharedAirGridCompiler().prepare(new AirFieldLayout(components, bricks, 64));
        while (!grid.run(0)) { }
        var layout = grid.layout();
        var sources = new WorkerPhysicalSourceBindings(null, ThermalSignatureTable.builder().build(),
                MinecraftPhysicalSourceProfile.CAMPFIRE, new AirLoadTable()).freeze();
        var units = new AirOperatorFragment.UnitGridPreparation(layout, 128, 1, sources);
        while (!units.run(0)) { }
        assertGraph(helper, units.result, layout.coefficientCount());
        assertGraph(helper, new AirOperatorFragment[0], 0);
        var mixing = new AirMixingRegion();
        assertGraph(helper, new AirOperatorFragment[]{AirOperatorFragment.coarseGridVolume(components[0], 128, 1, mixing)}, 8);
        assertGraph(helper, new AirOperatorFragment[]{AirOperatorFragment.coarseGridVolume(components[0], 128, 0, mixing)}, 8);
        var aliased = AirOperatorFragment.coarseGridVolume(component(0, 0, 64, 0,
                new int[]{0, 0, 1, 1, 2, 2, 3, 3}), 128, 1, mixing);
        assertGraph(helper, new AirOperatorFragment[]{aliased}, 4);
        var wide = AirOperatorFragment.coarseGridVolume(component(0, 0, 64, 0,
                new int[]{0, 1 << 29, 7, (1 << 29) + 1, 17, (1 << 29) + 3, 23, (1 << 29) + 5}), 128, 1, mixing);
        assertGraph(helper, new AirOperatorFragment[]{wide, aliased}, 1 << 30);
        FHMain.LOGGER.info("THERMAL_PAIRWISE_GRAPH,fixtures=6,fineComponents={},fineVolumes=512,fineNodes={},wideNodes={},exact=true",
                units.result.length, layout.coefficientCount(), 1 << 30);
        helper.succeed();
    }

    private static void finish(GameTestHelper helper, LongRadixSortCursor sort) {
        int resumes = 0;
        while (!sort.run(0)) helper.assertTrue(++resumes < 1000, "Every expired slice must advance the same sorter");
    }

    private static AirFieldLayout.Component component(int slot, int x, int y, int z, int[] indices) {
        return new AirFieldLayout.Component(slot, 1, x, y, z, -1L, indices, new ThermalPageHandle[0], new long[0]);
    }

    private static void assertGraph(GameTestHelper helper, AirOperatorFragment[] volumes, int nodes)
            throws ReflectiveOperationException {
        // Reference groups physical contributions by their full endpoint pair, independently of radix packing.
        var edges = new TreeMap<Long, ArrayList<Integer>>();
        int count = 0;
        for (var fragment : volumes) count += fragment.component.isUnitGrid() ? Long.bitCount(fragment.component.airBlocks) : 1;
        int[] expectedPositions = new int[count * 12]; Arrays.fill(expectedPositions, -1);
        int v = 0;
        for (var fragment : volumes) {
            var component = fragment.component;
            long cells = component.isUnitGrid() ? component.airBlocks : 1;
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells), ordinal = 0; cells &= cells - 1;
                for (int a = 0; a < 8; a++) for (int b = a + 1; b < 8; b++) {
                    if (Integer.bitCount(a ^ b) != 1) continue;
                    int position = v * 12 + ordinal++, entry = a * 8 - a * (a - 1) / 2 + b - a;
                    int x = component.coefficientIndex(component.isUnitGrid() ? component.unitCorner(cell, a) : a);
                    int y = component.coefficientIndex(component.isUnitGrid() ? component.unitCorner(cell, b) : b);
                    int first = Math.min(x, y), second = Math.max(x, y);
                    if (first == second || fragment.horizontal(cell)[entry] == 0 && fragment.vertical(cell)[entry] == 0) continue;
                    edges.computeIfAbsent((long) first << 32 | second, unused -> new ArrayList<>()).add(position);
                }
                v++;
            }
        }
        int[] expectedFirst = new int[edges.size()], expectedSecond = new int[edges.size()]; int edge = 0;
        for (var item : edges.entrySet()) {
            expectedFirst[edge] = (int) (item.getKey() >>> 32); expectedSecond[edge] = item.getKey().intValue();
            for (int position : item.getValue()) expectedPositions[position] = edge;
            edge++;
        }
        var type = Class.forName(CoupledThermalOperator.class.getName() + "$PairwiseGraphPreparation");
        var constructor = type.getDeclaredConstructor(AirOperatorFragment[].class, int.class, int.class); constructor.setAccessible(true);
        var preparing = constructor.newInstance(volumes, nodes, count);
        var run = type.getDeclaredMethod("run", long.class); run.setAccessible(true);
        int resumes = 0;
        while (!(boolean) run.invoke(preparing, 0L)) helper.assertTrue(++resumes < 1000, "Graph construction must resume to completion");
        for (var column : new Object[][]{{"first", expectedFirst}, {"second", expectedSecond}, {"positions", expectedPositions}}) {
            var field = type.getDeclaredField((String) column[0]); field.setAccessible(true);
            helper.assertTrue(Arrays.equals((int[]) field.get(preparing), (int[]) column[1]),
                    "Real graph " + column[0] + " differs from the independent edge map at nodes=" + nodes);
        }
    }
}
