/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_air_corner_topology")
@PrefixGameTestTemplate(false)
public final class AirCornerTopologyGameTests {
    private static final int[] CENTERS = {2, 4, 16, -16, 0};
    private static final int MAGIC = 0x51474331;

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void allLocalCornerPatternsMatchExistingQ1UnionFind(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(AirCornerTopology.count(0) == 0, "No Air creates no corner state");
        int comparisons = 0, maximumGroups = 0;
        String recordPath = System.getenv("FH_THERMAL_CORNER_RECORD");
        var fixture = recordPath == null ? AirCornerTopologyGameTests.class.getResourceAsStream("/thermal/q1-corner-reference.bin") : null;
        helper.assertTrue(recordPath != null || fixture != null, "The original Q1 numbering fixture must be present");
        if (recordPath != null) Files.createDirectories(Path.of(recordPath).getParent());
        try (var record = recordPath == null ? null : new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Path.of(recordPath))));
                var expected = fixture == null ? null : new DataInputStream(new BufferedInputStream(fixture))) {
        if (record != null) { record.writeInt(MAGIC); record.writeInt(CENTERS.length); }
        if (expected != null) helper.assertTrue(expected.readInt() == MAGIC && expected.readInt() == CENTERS.length, "Known geometry fixture format");
        for (int center : CENTERS) for (int mask = 0; mask < 256; mask++) {
            var compiled = compile(mask, center);
            if (record != null) record.writeInt(compiled.coefficientCount());
            if (expected != null) helper.assertTrue(expected.readInt() == compiled.coefficientCount(), "The original total corner count is unchanged");
            for (int octant = 0; octant < 8; octant++) for (int corner = 0; corner < 8; corner++) {
                int actual = (mask & 1 << octant) == 0 ? 255 : identity(compiled, center, octant, corner);
                if (record != null) record.writeByte(actual);
                if (expected != null) helper.assertTrue(expected.readUnsignedByte() == actual,
                        "Preserve original global corner order: center=" + center + ",mask=" + mask + ",octant=" + octant + ",corner=" + corner);
            }
            for (int first = 0; first < 8; first++) for (int second = 0; second < 8; second++) {
                if ((mask & 1 << first) == 0 || (mask & 1 << second) == 0) continue;
                int a = identity(compiled, center, first, first);
                int b = identity(compiled, center, second, second);
                helper.assertTrue((a == b) == (AirCornerTopology.group(mask, first) == AirCornerTopology.group(mask, second)),
                        "Local identities must equal the real region compiler: mask=" + mask + ",first=" + first + ",second=" + second);
                comparisons++;
            }
            int partition = 0, massCells = 0;
            for (int group = 0; group < AirCornerTopology.count(mask); group++) {
                int cells = AirCornerTopology.component(mask, group);
                helper.assertTrue((partition & cells) == 0, "One voxel cannot contribute capacity twice to the same corner");
                partition |= cells; massCells += Integer.bitCount(cells);
            }
            helper.assertTrue(partition == mask && massCells == Integer.bitCount(mask), "The exact incident capacity is retained");
            maximumGroups = Math.max(maximumGroups, AirCornerTopology.count(mask));
        }
        if (expected != null) helper.assertTrue(expected.read() == -1, "All geometry fixture records were consumed");
        }
        helper.assertTrue(maximumGroups == AirCornerTopology.MAXIMUM_GROUPS, "Four states cover even diagonally separated Air pockets");
        FHMain.LOGGER.info("THERMAL_CORNER_TOPOLOGY_RESULT,patterns=256,spatialCases={},identityComparisons={},maximumGroups={},recording={}",
                CENTERS.length, comparisons, maximumGroups, recordPath != null);
        helper.succeed();
    }

    private static AirFieldLayout compile(int mask, int center) {
        var blocks = new TreeMap<Long, Long>();
        for (int octant = 0; octant < 8; octant++) if ((mask & 1 << octant) != 0) {
            int x = center - (octant & 1), z = center - (octant >>> 1 & 1), y = center - (octant >>> 2 & 1);
            blocks.merge(BlockPos.asLong(x & ~3, y & ~3, z & ~3), 1L << ((x & 3) | (z & 3) << 2 | (y & 3) << 4), (a, b) -> a | b);
        }
        var components = new AirFieldLayout.Component[blocks.size()];
        var directory = new Long2ObjectOpenHashMap<AirFieldLayout.Component[]>();
        int slot = 0;
        for (var entry : blocks.entrySet()) {
            long brick = entry.getKey();
            var component = new AirFieldLayout.Component(slot, 1, BlockPos.getX(brick), BlockPos.getY(brick), BlockPos.getZ(brick),
                    entry.getValue(), new int[0], new ThermalPageHandle[0], new long[0]);
            components[slot++] = component; directory.put(brick, new AirFieldLayout.Component[] {component});
        }
        var preparing = new SharedAirGridCompiler().prepare(new AirFieldLayout(components, directory, 0));
        while (!preparing.run(System.nanoTime() + 1_000_000L)) { }
        return preparing.layout();
    }

    private static int identity(AirFieldLayout grid, int center, int octant, int corner) {
        int x = center - (octant & 1), z = center - (octant >>> 1 & 1), y = center - (octant >>> 2 & 1);
        var component = grid.borrowedComponentsAtBrick(BlockPos.asLong(x & ~3, y & ~3, z & ~3))[0];
        return component.coefficientIndex(component.unitCorner((x & 3) | (z & 3) << 2 | (y & 3) << 4, corner));
    }
}
