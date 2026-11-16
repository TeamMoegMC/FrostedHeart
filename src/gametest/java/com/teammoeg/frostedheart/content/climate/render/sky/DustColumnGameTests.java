/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.render.sky;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Checks the window of first open heights that the diamond dust glints look up: how it follows the eye and rereads only
 * the columns it brings in, which changes mark a column, that the shader's lookup finds the same column the window
 * holds, and which blocks stop a falling crystal.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class DustColumnGameTests {
    /**
     * The modulus and the base of the fake first open heights, which stay within the exact range of a float.
     */
    private static final int FAKE_MODULUS = 50;
    private static final int FAKE_BASE = -20;
    /**
     * The eye positions of the shader lookup test, as x, y and z.
     */
    private static final double[][] EYES = {
            {0.5, 70.25, 0.5},
            {-0.25, 64.0, -1000.75},
            {29999000.9, 100.5, -29999000.1},
            {-17.999, 80.0, 33.001}
    };
    /**
     * The index of the eye of EYES that lies far from the origin, whose texel coordinates wrap around the window.
     */
    private static final int FAR_EYE = 2;
    /**
     * The offsets of a glint from the eye, across x and z, and up y.
     */
    private static final double[] GLINT_OFFSETS = {-30.3, -16.7, -1.2, -0.4, 0.0, 0.3, 0.6, 1.4, 15.9, 30.6};
    private static final double[] GLINT_HEIGHTS = {-5.5, 0.0, 7.25};
    /**
     * Skips a glint whose world coordinate lies this near a whole number, where the shader's floor and the test's floor
     * may disagree on the side of the boundary.
     */
    private static final double WHOLE_NUMBER_TOLERANCE = 1.0e-4;
    /**
     * Skips a glint whose height lies this near the first open height of its column, where the float sum of the shader
     * and the double sum of the test may disagree on the side of the boundary.
     */
    private static final double FIRST_OPEN_TOLERANCE = 1.0e-3;
    /**
     * The eye's place in its block and the offset that the clamping test pushes out of the window.
     */
    private static final int CLAMP_EYE_IN_WINDOW = 32;
    private static final float CLAMP_EYE_FRACTION = 0.5F;
    private static final float CLAMP_OFFSET = 40F;
    /**
     * The floor height of the scene, above the terrain that covers the templates of the GameTest world.
     */
    private static final int HIGH_FLOOR_Y = 300;

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void dustColumnsFollowTheEyeAndReadOnlyTheColumnsTheyBringIn(GameTestHelper helper) {
        DustColumnWindow window = new DustColumnWindow();
        helper.assertTrue(!window.isPlaced(), "A new window is placed");
        helper.assertTrue(window.markedCount() == 0,
                "A new window marks " + window.markedCount() + " columns instead of 0");
        helper.assertTrue(window.height(0, 0) == DustColumnWindow.CLOSED,
                "A new window holds the height " + window.height(0, 0) + " at 0 0 instead of CLOSED");

        FakeSource source = new FakeSource();
        window.follow(100, -37);
        helper.assertTrue(window.originX() == 68 && window.originZ() == -69, "The window of the eye at 100 -37 starts "
                + "at " + window.originX() + " " + window.originZ() + " instead of 68 -69");
        helper.assertTrue(window.markedCount() == DustColumnWindow.COLUMNS, "The first follow marks "
                + window.markedCount() + " columns instead of " + DustColumnWindow.COLUMNS);
        assertAllClosed(helper, window, "of the first window");

        int read = window.readMarked(source, 1000);
        helper.assertTrue(read == 1000, "A budget of 1000 reads " + read + " columns instead of 1000");
        helper.assertTrue(window.markedCount() == DustColumnWindow.COLUMNS - 1000, "After a budget of 1000 the window "
                + "marks " + window.markedCount() + " columns instead of " + (DustColumnWindow.COLUMNS - 1000));
        read = window.readMarked(source, 10000);
        helper.assertTrue(read == DustColumnWindow.COLUMNS - 1000, "The rest of the first window reads " + read
                + " columns instead of " + (DustColumnWindow.COLUMNS - 1000));
        helper.assertTrue(source.calls == DustColumnWindow.COLUMNS, "The source is called " + source.calls
                + " times instead of " + DustColumnWindow.COLUMNS);
        assertAllFake(helper, window, "after the first read");
        helper.assertTrue(window.takeChanged(), "The window reports no change after its first read");
        helper.assertTrue(!window.takeChanged(), "The window reports a second change after its first read");

        window.follow(103, -39);
        helper.assertTrue(window.originX() == 71 && window.originZ() == -71, "The window of the eye at 103 -39 starts "
                + "at " + window.originX() + " " + window.originZ() + " instead of 71 -71");
        helper.assertTrue(window.markedCount() == 314, "The window moved by 3 and -2 marks " + window.markedCount()
                + " columns instead of 314");
        assertEnteredColumnsAreClosed(helper, window, 132, 134, -71, -70, "after the window moved by 3 and -2");
        int callsBefore = source.calls;
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 314, "The columns the window moved by 3 and -2 brought in read " + read
                + " columns instead of 314");
        helper.assertTrue(source.calls - callsBefore == 314, "The source is called " + (source.calls - callsBefore)
                + " times for the window moved by 3 and -2 instead of 314");
        assertAllFake(helper, window, "after the window moved by 3 and -2");

        window.follow(98, -35);
        helper.assertTrue(window.originX() == 66 && window.originZ() == -67, "The window of the eye at 98 -35 starts "
                + "at " + window.originX() + " " + window.originZ() + " instead of 66 -67");
        helper.assertTrue(window.markedCount() == 556, "The window moved by -5 and 4 marks " + window.markedCount()
                + " columns instead of 556");
        assertEnteredColumnsAreClosed(helper, window, 66, 70, -7, -4, "after the window moved by -5 and 4");
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 556, "The columns the window moved by -5 and 4 brought in read " + read
                + " columns instead of 556");
        assertAllFake(helper, window, "after the window moved by -5 and 4");

        window.follow(98 + 63, -35);
        helper.assertTrue(window.originX() == 129 && window.originZ() == -67, "The window of the eye at 161 -35 starts "
                + "at " + window.originX() + " " + window.originZ() + " instead of 129 -67");
        helper.assertTrue(window.markedCount() == 4032, "The window moved by 63 marks " + window.markedCount()
                + " columns instead of 4032");
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 4032, "The columns the window moved by 63 brought in read " + read
                + " columns instead of 4032");
        assertAllFake(helper, window, "after the window moved by 63");

        window.follow(98 + 63 + 64, -35);
        helper.assertTrue(window.originX() == 193 && window.originZ() == -67, "The window of the eye at 225 -35 starts "
                + "at " + window.originX() + " " + window.originZ() + " instead of 193 -67");
        helper.assertTrue(window.markedCount() == DustColumnWindow.COLUMNS, "A jump of the window's size marks "
                + window.markedCount() + " columns instead of " + DustColumnWindow.COLUMNS);
        assertAllClosed(helper, window, "after a jump of the window's size");

        window.follow(-29_999_000, -29_999_000);
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == DustColumnWindow.COLUMNS, "The window of the far eye reads " + read
                + " columns instead of " + DustColumnWindow.COLUMNS);
        assertAllFake(helper, window, "of the far eye");

        DustColumnWindow budgeted = new DustColumnWindow();
        FakeSource budgetSource = new FakeSource();
        budgeted.follow(0, 0);
        for (int quarter = 0; quarter < 4; quarter++) {
            int readQuarter = budgeted.readMarked(budgetSource, DustColumnWindow.COLUMNS / 4);
            helper.assertTrue(readQuarter == DustColumnWindow.COLUMNS / 4, "Quarter " + quarter + " of the budget "
                    + "reads " + readQuarter + " columns instead of " + (DustColumnWindow.COLUMNS / 4));
        }
        int fifth = budgeted.readMarked(budgetSource, DustColumnWindow.COLUMNS / 4);
        helper.assertTrue(fifth == 0, "A fifth quarter of the budget reads " + fifth + " columns instead of 0");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void dustColumnsMarkOnlyWhatChangedInsideTheWindow(GameTestHelper helper) {
        DustColumnWindow window = new DustColumnWindow();
        window.markColumn(0, 0);
        window.markChunk(0, 0);
        helper.assertTrue(window.markedCount() == 0, "A window that never followed marks " + window.markedCount()
                + " columns instead of 0");

        FakeSource source = new FakeSource();
        window.follow(0, 0);
        helper.assertTrue(window.originX() == -32 && window.originZ() == -32, "The window of the eye at 0 0 starts at "
                + window.originX() + " " + window.originZ() + " instead of -32 -32");
        int read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == DustColumnWindow.COLUMNS, "The first follow reads " + read + " columns instead of "
                + DustColumnWindow.COLUMNS);
        window.takeChanged();

        window.markColumn(5, -7);
        helper.assertTrue(window.markedCount() == 1, "One marked column leaves " + window.markedCount()
                + " marked instead of 1");
        window.markColumn(32, 0);
        window.markColumn(-33, 0);
        helper.assertTrue(window.markedCount() == 1, "Columns outside the window are marked: " + window.markedCount()
                + " marked instead of 1");
        window.markColumn(-32, 31);
        helper.assertTrue(window.markedCount() == 2, "A second marked column leaves " + window.markedCount()
                + " marked instead of 2");

        source.raise = 1;
        helper.assertTrue(window.height(5, -7) == fake(5, -7), "The marked column holds " + window.height(5, -7)
                + " before it is read instead of " + fake(5, -7));
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 2, "The two marked columns read " + read + " columns instead of 2");
        helper.assertTrue(window.height(5, -7) == fake(5, -7) + 1, "The raised column holds " + window.height(5, -7)
                + " instead of " + (fake(5, -7) + 1));
        helper.assertTrue(window.height(-32, 31) == fake(-32, 31) + 1, "The raised column at -32 31 holds "
                + window.height(-32, 31) + " instead of " + (fake(-32, 31) + 1));
        helper.assertTrue(window.height(6, -7) == fake(6, -7), "The unmarked column beside a raised one holds "
                + window.height(6, -7) + " instead of " + fake(6, -7));
        helper.assertTrue(window.takeChanged(), "The window reports no change after a raised height was read");

        window.markColumn(5, -7);
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 1, "A column marked again reads " + read + " columns instead of 1");
        helper.assertTrue(!window.takeChanged(), "The window reports a change after a column was read with the height "
                + "it already held");

        window.markChunk(0, 0);
        helper.assertTrue(window.markedCount() == 256, "The chunk at 0 0 marks " + window.markedCount()
                + " columns instead of 256");
        window.markChunk(1, 1);
        helper.assertTrue(window.markedCount() == 512, "The chunk at 1 1 marks " + window.markedCount()
                + " columns instead of 512");
        window.markChunk(2, 0);
        helper.assertTrue(window.markedCount() == 512, "The chunk at 2 0, outside the window, marks "
                + window.markedCount() + " columns instead of 512");
        window.markChunk(-3, -3);
        helper.assertTrue(window.markedCount() == 512, "The chunk at -3 -3, outside the window, marks "
                + window.markedCount() + " columns instead of 512");
        window.markChunk(-2, -2);
        helper.assertTrue(window.markedCount() == 768, "The chunk at -2 -2 marks " + window.markedCount()
                + " columns instead of 768");
        read = window.readMarked(source, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 768, "The three chunks read " + read + " columns instead of 768");

        window.forget();
        helper.assertTrue(!window.isPlaced(), "A forgotten window is still placed");
        helper.assertTrue(window.markedCount() == 0, "A forgotten window marks " + window.markedCount()
                + " columns instead of 0");
        helper.assertTrue(window.height(0, 0) == DustColumnWindow.CLOSED, "A forgotten window holds the height "
                + window.height(0, 0) + " at 0 0 instead of CLOSED");
        for (int index = 0; index < DustColumnWindow.COLUMNS; index++) {
            helper.assertTrue(window.heights()[index] == DustColumnWindow.CLOSED, "The texel " + index
                    + " of a forgotten window holds " + window.heights()[index] + " instead of CLOSED");
        }
        helper.assertTrue(window.takeChanged(), "A forgotten window reports no change");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void glintsLookUpTheColumnTheyStandInAsTheShaderDoes(GameTestHelper helper) {
        FakeSource source = new FakeSource();
        for (double[] eye : EYES) {
            double eyeX = eye[0];
            double eyeY = eye[1];
            double eyeZ = eye[2];
            DustColumnWindow window = new DustColumnWindow();
            window.follow((int) Math.floor(eyeX), (int) Math.floor(eyeZ));
            int read = window.readMarked(source, DustColumnWindow.COLUMNS);
            helper.assertTrue(read == DustColumnWindow.COLUMNS, "The window of the eye at " + eyeX + " " + eyeZ
                    + " reads " + read + " columns instead of " + DustColumnWindow.COLUMNS);
            int eyeColumnX = (int) Math.floor(eyeX) - window.originX();
            int eyeColumnZ = (int) Math.floor(eyeZ) - window.originZ();
            float eyeFractionX = (float) (eyeX - Math.floor(eyeX));
            float eyeFractionZ = (float) (eyeZ - Math.floor(eyeZ));
            int firstTexelX = DustColumnWindow.texelOfColumn(window.originX(), 0);
            int firstTexelZ = DustColumnWindow.texelOfColumn(window.originZ(), 0);
            for (double offsetX : GLINT_OFFSETS) {
                for (double offsetZ : GLINT_OFFSETS) {
                    if (isNearWholeNumber(eyeX + offsetX) || isNearWholeNumber(eyeZ + offsetZ)) {
                        continue;
                    }
                    int columnX = DustColumnWindow.columnInWindow(eyeColumnX, eyeFractionX, (float) offsetX);
                    int columnZ = DustColumnWindow.columnInWindow(eyeColumnZ, eyeFractionZ, (float) offsetZ);
                    int texelX = DustColumnWindow.texelOfColumn(firstTexelX, columnX);
                    int texelZ = DustColumnWindow.texelOfColumn(firstTexelZ, columnZ);
                    float shaderHeight = window.heights()[texelZ * DustColumnWindow.SIZE + texelX];
                    int worldX = (int) Math.floor(eyeX + offsetX);
                    int worldZ = (int) Math.floor(eyeZ + offsetZ);
                    double expectedHeight = window.height(worldX, worldZ);
                    helper.assertTrue((double) shaderHeight == expectedHeight, "The shader reads the height "
                            + shaderHeight + " for the glint " + offsetX + " " + offsetZ + " from the eye at " + eyeX
                            + " " + eyeZ + " instead of the " + expectedHeight + " the window holds at " + worldX
                            + " " + worldZ);
                    for (double offsetY : GLINT_HEIGHTS) {
                        if (Math.abs(eyeY + offsetY - expectedHeight) < FIRST_OPEN_TOLERANCE) {
                            continue;
                        }
                        boolean open = DustColumnWindow.isOpenAbove((float) eyeY, (float) offsetY, shaderHeight);
                        boolean expectedOpen = eyeY + offsetY >= expectedHeight;
                        helper.assertTrue(open == expectedOpen, "The glint " + offsetX + " " + offsetY + " "
                                + offsetZ + " from the eye at " + eyeX + " " + eyeY + " " + eyeZ + " is open above "
                                + open + " instead of " + expectedOpen + " over the first open height "
                                + expectedHeight);
                    }
                }
            }
        }

        helper.assertTrue(DustColumnWindow.columnInWindow(CLAMP_EYE_IN_WINDOW, CLAMP_EYE_FRACTION, CLAMP_OFFSET)
                        == DustColumnWindow.SIZE - 1,
                "An offset of " + CLAMP_OFFSET + " from the middle of the window gives the column "
                        + DustColumnWindow.columnInWindow(CLAMP_EYE_IN_WINDOW, CLAMP_EYE_FRACTION, CLAMP_OFFSET)
                        + " instead of " + (DustColumnWindow.SIZE - 1));
        helper.assertTrue(DustColumnWindow.columnInWindow(CLAMP_EYE_IN_WINDOW, CLAMP_EYE_FRACTION, -CLAMP_OFFSET) == 0,
                "An offset of " + -CLAMP_OFFSET + " from the middle of the window gives the column "
                        + DustColumnWindow.columnInWindow(CLAMP_EYE_IN_WINDOW, CLAMP_EYE_FRACTION, -CLAMP_OFFSET)
                        + " instead of 0");

        DustColumnWindow far = new DustColumnWindow();
        far.follow((int) Math.floor(EYES[FAR_EYE][0]), (int) Math.floor(EYES[FAR_EYE][2]));
        for (int x = far.originX(); x < far.originX() + DustColumnWindow.SIZE; x++) {
            for (int z = far.originZ(); z < far.originZ() + DustColumnWindow.SIZE; z++) {
                int expectedIndex = DustColumnWindow.texelOfColumn(
                        DustColumnWindow.texelOfColumn(far.originZ(), 0), z - far.originZ()) * DustColumnWindow.SIZE
                        + DustColumnWindow.texelOfColumn(DustColumnWindow.texelOfColumn(far.originX(), 0),
                        x - far.originX());
                helper.assertTrue(DustColumnWindow.texelIndex(x, z) == expectedIndex, "The column " + x + " " + z
                        + " of the far window has the texel " + DustColumnWindow.texelIndex(x, z) + " instead of the "
                        + expectedIndex + " the shader finds");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void fallingCrystalsStopAtGlassFencesLeavesAndWaterButNotCarpetSnowOrPlants(GameTestHelper helper) {
        assertStopsCrystals(helper, Blocks.STONE.defaultBlockState(), "stone", true);
        assertStopsCrystals(helper, Blocks.GLASS.defaultBlockState(), "glass", true);
        assertStopsCrystals(helper, Blocks.GLASS_PANE.defaultBlockState(), "a glass pane", true);
        assertStopsCrystals(helper, Blocks.IRON_BARS.defaultBlockState(), "iron bars", true);
        assertStopsCrystals(helper, Blocks.OAK_FENCE.defaultBlockState(), "an oak fence", true);
        assertStopsCrystals(helper, Blocks.COBBLESTONE_WALL.defaultBlockState(), "a cobblestone wall", true);
        assertStopsCrystals(helper, Blocks.OAK_LEAVES.defaultBlockState(), "oak leaves", true);
        assertStopsCrystals(helper, Blocks.ICE.defaultBlockState(), "ice", true);
        assertStopsCrystals(helper, Blocks.WATER.defaultBlockState(), "water", true);
        assertStopsCrystals(helper, Blocks.OAK_SLAB.defaultBlockState(), "an oak slab", true);
        assertStopsCrystals(helper, Blocks.OAK_STAIRS.defaultBlockState(), "oak stairs", true);
        assertStopsCrystals(helper, Blocks.AIR.defaultBlockState(), "air", false);
        assertStopsCrystals(helper, Blocks.WHITE_CARPET.defaultBlockState(), "a white carpet", false);
        assertStopsCrystals(helper, Blocks.SNOW.defaultBlockState(), "one layer of snow", false);
        assertStopsCrystals(helper, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 8),
                "8 layers of snow", false);
        assertStopsCrystals(helper, Blocks.GRASS.defaultBlockState(), "grass", false);
        assertStopsCrystals(helper, Blocks.POPPY.defaultBlockState(), "a poppy", false);
        assertStopsCrystals(helper, Blocks.TORCH.defaultBlockState(), "a torch", false);
        assertStopsCrystals(helper, Blocks.COBWEB.defaultBlockState(), "a cobweb", false);
        assertStopsCrystals(helper, Blocks.RAIL.defaultBlockState(), "a rail", false);

        fillGrassFloor(helper);
        BlockPos glass = highAbove(helper, 2, 5, 2);
        BlockPos fence = highAbove(helper, 4, 5, 2);
        BlockPos leaves = highAbove(helper, 6, 5, 2);
        BlockPos carpet = highAbove(helper, 8, 1, 2);
        BlockPos snow = highAbove(helper, 10, 1, 2);
        BlockPos poppy = highAbove(helper, 6, 1, 4);
        BlockPos empty = highAbove(helper, 2, 1, 4);
        ServerLevel level = helper.getLevel();
        level.setBlockAndUpdate(glass, Blocks.GLASS.defaultBlockState());
        level.setBlockAndUpdate(fence, Blocks.OAK_FENCE.defaultBlockState());
        level.setBlockAndUpdate(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        level.setBlockAndUpdate(carpet, Blocks.WHITE_CARPET.defaultBlockState());
        level.setBlockAndUpdate(snow, Blocks.SNOW.defaultBlockState());
        level.setBlockAndUpdate(poppy, Blocks.POPPY.defaultBlockState());

        assertHeightmapHeight(helper, level, glass, HIGH_FLOOR_Y + 6, "the glass");
        assertHeightmapHeight(helper, level, fence, HIGH_FLOOR_Y + 6, "the oak fence");
        assertHeightmapHeight(helper, level, leaves, HIGH_FLOOR_Y + 6, "the oak leaves");
        assertHeightmapHeight(helper, level, carpet, HIGH_FLOOR_Y + 1, "the white carpet");
        assertHeightmapHeight(helper, level, snow, HIGH_FLOOR_Y + 1, "the one layer of snow");
        assertHeightmapHeight(helper, level, poppy, HIGH_FLOOR_Y + 1, "the poppy");
        assertHeightmapHeight(helper, level, empty, HIGH_FLOOR_Y + 1, "the empty column");

        DustColumnWindow.ColumnSource levelSource = (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        DustColumnWindow window = new DustColumnWindow();
        window.follow(glass.getX(), glass.getZ());
        int read = window.readMarked(levelSource, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == DustColumnWindow.COLUMNS, "The window of the glass column reads " + read
                + " columns instead of " + DustColumnWindow.COLUMNS);
        assertWindowHeight(helper, window, level, glass, "the glass");
        assertWindowHeight(helper, window, level, fence, "the oak fence");
        assertWindowHeight(helper, window, level, leaves, "the oak leaves");
        assertWindowHeight(helper, window, level, carpet, "the white carpet");
        assertWindowHeight(helper, window, level, snow, "the one layer of snow");
        assertWindowHeight(helper, window, level, poppy, "the poppy");
        assertWindowHeight(helper, window, level, empty, "the empty column");

        float glassHeight = window.height(glass.getX(), glass.getZ());
        float emptyHeight = window.height(empty.getX(), empty.getZ());
        helper.assertTrue(!DustColumnWindow.isOpenAbove(302.5F, 0.0F, glassHeight), "A glint at 302.5 over the glass "
                + "at " + glassHeight + " is open above");
        helper.assertTrue(DustColumnWindow.isOpenAbove(302.5F, 0.0F, emptyHeight), "A glint at 302.5 over the empty "
                + "column at " + emptyHeight + " is not open above");
        helper.assertTrue(DustColumnWindow.isOpenAbove(310.0F, -3.5F, glassHeight), "A glint at 306.5 over the glass "
                + "at " + glassHeight + " is not open above");

        level.setBlockAndUpdate(glass, Blocks.AIR.defaultBlockState());
        window.markColumn(glass.getX(), glass.getZ());
        read = window.readMarked(levelSource, DustColumnWindow.COLUMNS);
        helper.assertTrue(read == 1, "The column of the removed glass reads " + read + " columns instead of 1");
        helper.assertTrue(window.height(glass.getX(), glass.getZ()) == HIGH_FLOOR_Y + 1, "The column of the removed "
                + "glass holds " + window.height(glass.getX(), glass.getZ()) + " instead of " + (HIGH_FLOOR_Y + 1));
        helper.succeed();
    }

    /**
     * The fake first open height of a column, which stays within the exact range of a float.
     */
    private static int fake(int x, int z) {
        return Math.floorMod(7 * x + 13 * z, FAKE_MODULUS) + FAKE_BASE;
    }

    /**
     * Whether a coordinate lies so near a whole number that the shader's floor and the test's floor may disagree.
     */
    private static boolean isNearWholeNumber(double value) {
        return Math.abs(value - Math.rint(value)) < WHOLE_NUMBER_TOLERANCE;
    }

    /**
     * Asserts that every column of the window reads CLOSED.
     */
    private static void assertAllClosed(GameTestHelper helper, DustColumnWindow window, String where) {
        for (int x = window.originX(); x < window.originX() + DustColumnWindow.SIZE; x++) {
            for (int z = window.originZ(); z < window.originZ() + DustColumnWindow.SIZE; z++) {
                helper.assertTrue(window.height(x, z) == DustColumnWindow.CLOSED, "The column " + x + " " + z + " "
                        + where + " holds " + window.height(x, z) + " instead of CLOSED");
            }
        }
    }

    /**
     * Asserts that every column of the window reads its fake first open height.
     */
    private static void assertAllFake(GameTestHelper helper, DustColumnWindow window, String where) {
        for (int x = window.originX(); x < window.originX() + DustColumnWindow.SIZE; x++) {
            for (int z = window.originZ(); z < window.originZ() + DustColumnWindow.SIZE; z++) {
                helper.assertTrue(window.height(x, z) == fake(x, z), "The column " + x + " " + z + " " + where
                        + " holds " + window.height(x, z) + " instead of the fake " + fake(x, z));
            }
        }
    }

    /**
     * Asserts that the columns a follow brought in read CLOSED and that every other column of the window keeps its fake
     * first open height. A column was brought in if its x lies in the x range or its z lies in the z range.
     */
    private static void assertEnteredColumnsAreClosed(GameTestHelper helper, DustColumnWindow window, int fromX,
            int toX, int fromZ, int toZ, String where) {
        for (int x = window.originX(); x < window.originX() + DustColumnWindow.SIZE; x++) {
            for (int z = window.originZ(); z < window.originZ() + DustColumnWindow.SIZE; z++) {
                boolean entered = x >= fromX && x <= toX || z >= fromZ && z <= toZ;
                float expected = entered ? DustColumnWindow.CLOSED : fake(x, z);
                String expectedName = entered ? "CLOSED" : "the fake " + fake(x, z);
                helper.assertTrue(window.height(x, z) == expected, "The column " + x + " " + z + " " + where
                        + " holds " + window.height(x, z) + " instead of " + expectedName);
            }
        }
    }

    /**
     * Asserts whether a block state stops falling crystals.
     */
    private static void assertStopsCrystals(GameTestHelper helper, BlockState state, String name, boolean expected) {
        boolean actual = DustColumnWindow.stopsCrystals(state);
        helper.assertTrue(actual == expected, "The " + name + " stops falling crystals " + actual + " instead of "
                + expected);
    }

    /**
     * Asserts the height the MOTION_BLOCKING heightmap gives a column.
     */
    private static void assertHeightmapHeight(GameTestHelper helper, ServerLevel level, BlockPos column,
            int expected, String name) {
        int actual = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        helper.assertTrue(actual == expected, "The height above the " + name + " at " + column.getX() + " "
                + column.getZ() + " is " + actual + " instead of " + expected);
    }

    /**
     * Asserts that the window holds the height the MOTION_BLOCKING heightmap gives a column.
     */
    private static void assertWindowHeight(GameTestHelper helper, DustColumnWindow window, ServerLevel level,
            BlockPos column, String name) {
        float actual = window.height(column.getX(), column.getZ());
        int expected = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        helper.assertTrue(actual == expected, "The window holds the height " + actual + " above the " + name + " at "
                + column.getX() + " " + column.getZ() + " instead of the heightmap's " + expected);
    }

    /**
     * Fills a grass floor at HIGH_FLOOR_Y over the template's footprint, so the floor's top lies 1 above HIGH_FLOOR_Y.
     */
    private static void fillGrassFloor(GameTestHelper helper) {
        for (int x = 0; x < 20; x++) {
            for (int z = 0; z < 20; z++) {
                helper.getLevel().setBlockAndUpdate(highAbove(helper, x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState());
            }
        }
    }

    /**
     * The block at the template's relative x and z, the given height above HIGH_FLOOR_Y.
     */
    private static BlockPos highAbove(GameTestHelper helper, int x, int y, int z) {
        BlockPos column = helper.absolutePos(new BlockPos(x, 0, z));
        return new BlockPos(column.getX(), HIGH_FLOOR_Y + y, column.getZ());
    }

    /**
     * The fake first open heights of the tests, raised by a settable amount, counting the columns it is asked for.
     */
    private static final class FakeSource implements DustColumnWindow.ColumnSource {
        private int calls;
        private int raise;

        @Override
        public int firstOpenY(int x, int z) {
            calls++;
            return fake(x, z) + raise;
        }
    }
}
