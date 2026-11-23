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

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Checks the block light that the diamond dust glints take from torches and lamps: how the volume follows the eye and
 * reads only the blocks it brings in, which sections it rereads, that the shader's filtered lookup finds the light
 * between the two blocks a glint lies between without crossing the seam, and that the color is vanilla's block light.
 */
@GameTestHolder("frostedheart_calculated_sky")
@PrefixGameTestTemplate(false)
public final class DustLightGameTests {
    /**
     * The modulus and the weights of the fake block light, which stays from 0 to BRIGHTEST_LEVEL.
     */
    private static final int FAKE_MODULUS = 16;
    private static final int FAKE_X_WEIGHT = 3;
    private static final int FAKE_Y_WEIGHT = 5;
    private static final int FAKE_Z_WEIGHT = 7;
    /**
     * The eye positions of the shader lookup test, as x, y and z.
     */
    private static final double[][] EYES = {
            {0.5, 70.25, 0.5},
            {-0.25, 64.0, -1000.75},
            {29999000.9, 100.5, -29999000.1},
            {-17.999, 80.49, 33.51}
    };
    /**
     * The index of the eye of EYES that lies far from the origin, whose texel coordinates wrap around the volume.
     */
    private static final int FAR_EYE = 2;
    /**
     * The camera's drift from the eye between ticks, and the offsets of a glint from the camera.
     */
    private static final double[] CAMERA_DRIFTS = {-1.0, -0.55, 0.0, 0.45, 1.0};
    private static final double FIRST_GLINT_OFFSET = -15.99;
    private static final double GLINT_OFFSET_STEP = 0.13;
    private static final int GLINT_OFFSET_COUNT = 247;
    /**
     * Skips a glint whose point lies this near a boundary between blocks, where the float sum of the shader and the
     * double sum of the test may disagree on the side of the boundary.
     */
    private static final double BOUNDARY_TOLERANCE = 2.0e-3;
    /**
     * The tolerance of the block light color, which the shader and the test compute in a different order.
     */
    private static final float COLOR_TOLERANCE = 1.0e-6F;
    private static final float STORAGE_TOLERANCE = 1.0e-6F;
    private static final float BRIGHTEST_BRIGHTNESS = 1.5F;
    private static final float GREEN_GREEN_WEIGHT = 0.6F;
    private static final float GREEN_RED_WEIGHT = 0.4F;
    private static final float BLUE_BLUE_WEIGHT = 0.6F;
    private static final float BLUE_RED_WEIGHT = 0.4F;
    private static final float STORED_MAXIMUM = 255.0F;

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void dustLightsFollowTheEyeAndReadOnlyTheBlocksTheyBringIn(GameTestHelper helper) {
        DustLightVolume volume = new DustLightVolume();
        helper.assertTrue(!volume.isPlaced(), "A new volume is placed");
        helper.assertTrue(volume.markedCount() == 0,
                "A new volume marks " + volume.markedCount() + " blocks instead of 0");
        helper.assertTrue(volume.light(0, 0, 0) == 0,
                "A new volume holds the light " + volume.light(0, 0, 0) + " at 0 0 0 instead of 0");

        FakeSource source = new FakeSource();
        volume.follow(100, 64, -37);
        assertOrigin(helper, volume, 82, 46, -55, "the eye at 100 64 -37");
        helper.assertTrue(volume.markedCount() == DustLightVolume.BLOCKS, "The first follow marks "
                + volume.markedCount() + " blocks instead of " + DustLightVolume.BLOCKS);
        assertAllDark(helper, volume, "of the first window");

        int read = volume.readMarked(source, 10000);
        helper.assertTrue(read == 10000, "A budget of 10000 reads " + read + " blocks instead of 10000");
        helper.assertTrue(volume.markedCount() == DustLightVolume.BLOCKS - 10000, "After a budget of 10000 the volume "
                + "marks " + volume.markedCount() + " blocks instead of " + (DustLightVolume.BLOCKS - 10000));
        read = volume.readMarked(source, 100000);
        helper.assertTrue(read == DustLightVolume.BLOCKS - 10000, "The rest of the first window reads " + read
                + " blocks instead of " + (DustLightVolume.BLOCKS - 10000));
        helper.assertTrue(source.calls == DustLightVolume.BLOCKS, "The source is called " + source.calls
                + " times instead of " + DustLightVolume.BLOCKS);
        assertAllFake(helper, volume, "after the first read");
        helper.assertTrue(volume.takeChanged(), "The volume reports no change after its first read");
        helper.assertTrue(!volume.takeChanged(), "The volume reports a second change after its first read");

        volume.follow(103, 62, -36);
        assertOrigin(helper, volume, 85, 44, -54, "the eye at 103 62 -36");
        helper.assertTrue(volume.markedCount() == 7386, "The volume moved by 3, -2 and 1 marks "
                + volume.markedCount() + " blocks instead of 7386");
        assertEnteredBlocksAreDark(helper, volume, 118, 120, 44, 45, -19, -19, "after the volume moved by 3, -2 and 1");
        int callsBefore = source.calls;
        read = volume.readMarked(source, DustLightVolume.BLOCKS);
        helper.assertTrue(read == 7386, "The blocks the volume moved by 3, -2 and 1 brought in read " + read
                + " blocks instead of 7386");
        helper.assertTrue(source.calls - callsBefore == 7386, "The source is called " + (source.calls - callsBefore)
                + " times for the volume moved by 3, -2 and 1 instead of 7386");
        assertAllFake(helper, volume, "after the volume moved by 3, -2 and 1");

        volume.follow(138, 62, -36);
        assertOrigin(helper, volume, 120, 44, -54, "the eye at 138 62 -36");
        helper.assertTrue(volume.markedCount() == 45360, "The volume moved by 35 in x marks " + volume.markedCount()
                + " blocks instead of 45360");
        read = volume.readMarked(source, DustLightVolume.BLOCKS);
        helper.assertTrue(read == 45360, "The blocks the volume moved by 35 in x brought in read " + read
                + " blocks instead of 45360");
        assertAllFake(helper, volume, "after the volume moved by 35 in x");

        volume.follow(174, 62, -36);
        assertOrigin(helper, volume, 156, 44, -54, "the eye at 174 62 -36");
        helper.assertTrue(volume.markedCount() == DustLightVolume.BLOCKS, "A move of the volume's size marks "
                + volume.markedCount() + " blocks instead of " + DustLightVolume.BLOCKS);
        assertAllDark(helper, volume, "after a move of the volume's size");

        volume.follow(-29_999_000, 300, 29_999_000);
        assertOrigin(helper, volume, -29_999_018, 282, 29_998_982, "the far eye");
        read = volume.readMarked(source, DustLightVolume.BLOCKS);
        helper.assertTrue(read == DustLightVolume.BLOCKS, "The volume of the far eye reads " + read
                + " blocks instead of " + DustLightVolume.BLOCKS);
        assertAllFake(helper, volume, "of the far eye");

        int beforeX = volume.light(volume.originX() - 1, volume.originY(), volume.originZ());
        helper.assertTrue(beforeX == 0, "The block before the far volume holds the light " + beforeX
                + " instead of 0");
        int afterX = volume.light(volume.originX() + DustLightVolume.SIZE, volume.originY(), volume.originZ());
        helper.assertTrue(afterX == 0, "The block after the far volume holds the light " + afterX
                + " instead of 0");
        int aboveY = volume.light(volume.originX(), volume.originY() + DustLightVolume.SIZE, volume.originZ());
        helper.assertTrue(aboveY == 0, "The block above the far volume holds the light " + aboveY
                + " instead of 0");

        DustLightVolume budgeted = new DustLightVolume();
        FakeSource budgetSource = new FakeSource();
        budgeted.follow(0, 0, 0);
        for (int eighth = 0; eighth < 8; eighth++) {
            int readEighth = budgeted.readMarked(budgetSource, DustLightVolume.BLOCKS / 8);
            helper.assertTrue(readEighth == DustLightVolume.BLOCKS / 8, "Eighth " + eighth + " of the budget reads "
                    + readEighth + " blocks instead of " + (DustLightVolume.BLOCKS / 8));
        }
        int ninth = budgeted.readMarked(budgetSource, DustLightVolume.BLOCKS / 8);
        helper.assertTrue(ninth == 0, "A ninth eighth of the budget reads " + ninth + " blocks instead of 0");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void dustLightsRereadOnlyTheSectionsWhoseLightChanged(GameTestHelper helper) {
        DustLightVolume volume = new DustLightVolume();
        helper.assertTrue(!volume.markSection(0, 0, 0), "A volume that never followed marks the section 0 0 0");
        helper.assertTrue(volume.markedCount() == 0, "A volume that never followed marks " + volume.markedCount()
                + " blocks instead of 0");

        FakeSource source = new FakeSource();
        volume.follow(0, 0, 0);
        assertOrigin(helper, volume, -18, -18, -18, "of the eye at 0 0 0");
        int read = volume.readMarked(source, DustLightVolume.BLOCKS);
        helper.assertTrue(read == DustLightVolume.BLOCKS, "The first follow reads " + read + " blocks instead of "
                + DustLightVolume.BLOCKS);
        volume.takeChanged();

        helper.assertTrue(volume.markSection(0, 0, 0), "The section 0 0 0 marks no block");
        helper.assertTrue(volume.markedCount() == 4096, "The section 0 0 0 marks " + volume.markedCount()
                + " blocks instead of 4096");
        helper.assertTrue(volume.markSection(1, 0, 0), "The section 1 0 0 marks no block");
        helper.assertTrue(volume.markedCount() == 4608, "The section 1 0 0 leaves " + volume.markedCount()
                + " blocks marked instead of 4608");
        helper.assertTrue(!volume.markSection(2, 0, 0), "The section 2 0 0, outside the volume, marks blocks");
        helper.assertTrue(volume.markedCount() == 4608, "The section 2 0 0, outside the volume, leaves "
                + volume.markedCount() + " blocks marked instead of 4608");
        helper.assertTrue(volume.markSection(-2, -2, -2), "The section -2 -2 -2 marks no block");
        helper.assertTrue(volume.markedCount() == 4616, "The section -2 -2 -2 leaves " + volume.markedCount()
                + " blocks marked instead of 4616");
        helper.assertTrue(!volume.markSection(-3, 0, 0), "The section -3 0 0, outside the volume, marks blocks");
        helper.assertTrue(volume.markedCount() == 4616, "The section -3 0 0, outside the volume, leaves "
                + volume.markedCount() + " blocks marked instead of 4616");
        helper.assertTrue(volume.markSection(0, 1, 0), "The section 0 1 0 marks no block");
        helper.assertTrue(volume.markedCount() == 5128, "The section 0 1 0 leaves " + volume.markedCount()
                + " blocks marked instead of 5128");

        source.raise = 1;
        helper.assertTrue(volume.light(0, 0, 0) == fake(0, 0, 0, 0), "The marked block 0 0 0 holds "
                + volume.light(0, 0, 0) + " before it is read instead of the fake " + fake(0, 0, 0, 0));
        read = volume.readMarked(source, DustLightVolume.BLOCKS);
        helper.assertTrue(read == 5128, "The marked sections read " + read + " blocks instead of 5128");
        assertRaised(helper, volume, 0, 0, 0);
        assertRaised(helper, volume, 17, 0, 0);
        assertRaised(helper, volume, -17, -17, -17);
        assertRaised(helper, volume, 0, 17, 0);
        assertUnraised(helper, volume, -1, 0, 0);
        assertUnraised(helper, volume, 0, 0, 16);
        helper.assertTrue(volume.takeChanged(), "The volume reports no change after the raised blocks were read");

        helper.assertTrue(volume.markSection(0, 0, 0), "The section 0 0 0 marks no block when marked again");
        read = volume.readMarked(source, DustLightVolume.BLOCKS);
        helper.assertTrue(read == 4096, "The section 0 0 0 read again reads " + read + " blocks instead of 4096");
        helper.assertTrue(!volume.takeChanged(), "The volume reports a change after the section 0 0 0 was read with "
                + "the light it already held");

        volume.forget();
        helper.assertTrue(!volume.isPlaced(), "A forgotten volume is still placed");
        helper.assertTrue(volume.markedCount() == 0, "A forgotten volume marks " + volume.markedCount()
                + " blocks instead of 0");
        helper.assertTrue(volume.light(0, 0, 0) == 0, "A forgotten volume holds the light " + volume.light(0, 0, 0)
                + " at 0 0 0 instead of 0");
        for (int index = 0; index < DustLightVolume.BLOCKS; index++) {
            helper.assertTrue(volume.values()[index] == 0, "The texel " + index + " of a forgotten volume holds "
                    + volume.values()[index] + " instead of 0");
        }
        helper.assertTrue(volume.takeChanged(), "A forgotten volume reports no change");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void lampGlintsSampleTheBlocksAroundThemWithoutCrossingTheSeam(GameTestHelper helper) {
        for (double[] eye : EYES) {
            DustLightVolume volume = new DustLightVolume();
            volume.follow(corner(eye[0]), corner(eye[1]), corner(eye[2]));
            assertOrigin(helper, volume, corner(eye[0]) - DustLightVolume.HALF_SIZE,
                    corner(eye[1]) - DustLightVolume.HALF_SIZE, corner(eye[2]) - DustLightVolume.HALF_SIZE,
                    "of the eye at " + eye[0] + " " + eye[1] + " " + eye[2]);
            for (int axis = 0; axis < 3; axis++) {
                double eyeCoordinate = eye[axis];
                int origin = axis == 0 ? volume.originX() : axis == 1 ? volume.originY() : volume.originZ();
                int originTexel = DustLightVolume.texel(origin);
                for (double drift : CAMERA_DRIFTS) {
                    double camera = eyeCoordinate + drift;
                    float eyeInVolume = (float) (camera - origin);
                    for (int step = 0; step < GLINT_OFFSET_COUNT; step++) {
                        double offset = FIRST_GLINT_OFFSET + GLINT_OFFSET_STEP * step;
                        float point = eyeInVolume + (float) offset;
                        helper.assertTrue(point >= 0.5F && point <= 35.5F, "The point " + point + " of the glint "
                                + offset + " from the camera " + camera + " on axis " + axis + " of the eye at "
                                + eye[0] + " " + eye[1] + " " + eye[2] + " lies outside the volume");
                        double below = camera + offset - 0.5;
                        if (Math.abs(below - Math.rint(below)) < BOUNDARY_TOLERANCE) {
                            continue;
                        }
                        float coordinate = DustLightVolume.textureCoordinate(originTexel, eyeInVolume, (float) offset);
                        double scaled = (double) coordinate * DustLightVolume.SIZE - 0.5;
                        int firstTexel = Math.floorMod((int) Math.floor(scaled), DustLightVolume.SIZE);
                        int secondTexel = (firstTexel + 1) % DustLightVolume.SIZE;
                        double weight = scaled - Math.floor(scaled);
                        int block = (int) Math.floor(below);
                        helper.assertTrue(firstTexel == DustLightVolume.texel(block), "The glint " + offset
                                + " from the camera " + camera + " on axis " + axis + " of the eye at " + eye[0] + " "
                                + eye[1] + " " + eye[2] + " reads the texel " + firstTexel + " instead of the "
                                + DustLightVolume.texel(block) + " of the block " + block);
                        helper.assertTrue(secondTexel == DustLightVolume.texel(block + 1), "The glint " + offset
                                + " from the camera " + camera + " on axis " + axis + " of the eye at " + eye[0] + " "
                                + eye[1] + " " + eye[2] + " reads the texel " + secondTexel + " instead of the "
                                + DustLightVolume.texel(block + 1) + " of the block " + (block + 1));
                        helper.assertTrue(Math.abs(weight - (below - block)) < BOUNDARY_TOLERANCE, "The glint "
                                + offset + " from the camera " + camera + " on axis " + axis + " of the eye at "
                                + eye[0] + " " + eye[1] + " " + eye[2] + " weighs the second block with " + weight
                                + " instead of " + (below - block));
                        long fromOrigin = (long) block - origin;
                        helper.assertTrue(fromOrigin >= 0 && fromOrigin + 1 <= DustLightVolume.SIZE - 1, "The glint "
                                + offset + " from the camera " + camera + " on axis " + axis + " of the eye at "
                                + eye[0] + " " + eye[1] + " " + eye[2] + " reads the blocks " + fromOrigin + " and "
                                + (fromOrigin + 1) + " of the volume instead of two blocks inside it");
                    }
                }
            }
        }

        helper.assertTrue(DustLightVolume.textureCoordinate(0, 18.0F, 40.0F) == 35.5F / DustLightVolume.SIZE,
                "An offset of 40 from the middle of the volume gives the coordinate "
                        + DustLightVolume.textureCoordinate(0, 18.0F, 40.0F) + " instead of "
                        + (35.5F / DustLightVolume.SIZE));
        helper.assertTrue(DustLightVolume.textureCoordinate(0, 18.0F, -40.0F) == 0.5F / DustLightVolume.SIZE,
                "An offset of -40 from the middle of the volume gives the coordinate "
                        + DustLightVolume.textureCoordinate(0, 18.0F, -40.0F) + " instead of "
                        + (0.5F / DustLightVolume.SIZE));
        helper.assertTrue(DustLightVolume.textureCoordinate(10, 18.0F, 0.25F) == (10 + 18.25F) / DustLightVolume.SIZE,
                "The coordinate of a point 0.25 from the eye in the texel 10 is "
                        + DustLightVolume.textureCoordinate(10, 18.0F, 0.25F) + " instead of "
                        + ((10 + 18.25F) / DustLightVolume.SIZE));

        DustLightVolume far = new DustLightVolume();
        far.follow(corner(EYES[FAR_EYE][0]), corner(EYES[FAR_EYE][1]), corner(EYES[FAR_EYE][2]));
        int farOriginX = far.originX();
        int farOriginY = far.originY();
        int farOriginZ = far.originZ();
        for (int x = farOriginX; x < farOriginX + DustLightVolume.SIZE; x++) {
            int texelX = Math.floorMod(DustLightVolume.texel(farOriginX) + (x - farOriginX), DustLightVolume.SIZE);
            for (int y = farOriginY; y < farOriginY + DustLightVolume.SIZE; y++) {
                int texelY = Math.floorMod(DustLightVolume.texel(farOriginY) + (y - farOriginY), DustLightVolume.SIZE);
                for (int z = farOriginZ; z < farOriginZ + DustLightVolume.SIZE; z++) {
                    int texelZ = Math.floorMod(DustLightVolume.texel(farOriginZ) + (z - farOriginZ),
                            DustLightVolume.SIZE);
                    int expectedIndex = (texelZ * DustLightVolume.SIZE + texelY) * DustLightVolume.SIZE + texelX;
                    helper.assertTrue(DustLightVolume.texelIndex(x, y, z) == expectedIndex, "The block " + x + " " + y
                            + " " + z + " of the far volume has the texel " + DustLightVolume.texelIndex(x, y, z)
                            + " instead of the " + expectedIndex + " the shader finds");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 200)
    public static void lampGlintsTakeVanillasBlockLightColor(GameTestHelper helper) {
        float ambientLight = helper.getLevel().dimensionType().ambientLight();
        helper.assertTrue(ambientLight == 0.0F, "The GameTest level has the ambient light " + ambientLight
                + " instead of 0.0");
        float[] color = new float[3];
        float[] previous = new float[3];
        for (int level = 0; level <= DustLightVolume.BRIGHTEST_LEVEL; level++) {
            float share = level / (float) DustLightVolume.BRIGHTEST_LEVEL;
            DustLightVolume.blockLightColor(share, color);
            float brightness = Mth.lerp(ambientLight, share / (4.0F - 3.0F * share), 1.0F) * BRIGHTEST_BRIGHTNESS;
            float[] expected = {
                    Math.min(1.0F, brightness),
                    Math.min(1.0F, brightness * ((brightness * GREEN_GREEN_WEIGHT + GREEN_RED_WEIGHT)
                            * GREEN_GREEN_WEIGHT + GREEN_RED_WEIGHT)),
                    Math.min(1.0F, brightness * (brightness * brightness * BLUE_BLUE_WEIGHT + BLUE_RED_WEIGHT))
            };
            for (int channel = 0; channel < 3; channel++) {
                helper.assertTrue(Math.abs(color[channel] - expected[channel]) < COLOR_TOLERANCE, "The channel "
                        + channel + " of the level " + level + " is " + color[channel] + " instead of the vanilla "
                        + expected[channel]);
            }
            if (level == 0) {
                for (int channel = 0; channel < 3; channel++) {
                    helper.assertTrue(color[channel] == 0.0F, "The channel " + channel + " of the level 0 is "
                            + color[channel] + " instead of exactly 0");
                }
            }
            if (level == DustLightVolume.BRIGHTEST_LEVEL) {
                for (int channel = 0; channel < 3; channel++) {
                    helper.assertTrue(color[channel] == 1.0F, "The channel " + channel + " of the level "
                            + DustLightVolume.BRIGHTEST_LEVEL + " is " + color[channel] + " instead of exactly 1");
                }
            }
            if (level > 0) {
                for (int channel = 0; channel < 3; channel++) {
                    helper.assertTrue(color[channel] >= previous[channel], "The channel " + channel + " falls from "
                            + previous[channel] + " at the level " + (level - 1) + " to " + color[channel]
                            + " at the level " + level);
                }
            }
            helper.assertTrue(color[0] >= color[1] && color[1] >= color[2], "The level " + level + " colors red "
                    + color[0] + ", green " + color[1] + " and blue " + color[2] + " instead of red >= green >= blue");
            float stored = (level * DustLightVolume.STORED_PER_LEVEL) / STORED_MAXIMUM;
            helper.assertTrue(Math.abs(stored - share) < STORAGE_TOLERANCE, "The level " + level + " is stored as "
                    + stored + " instead of the share " + share);
            System.arraycopy(color, 0, previous, 0, 3);
        }

        for (int sourceLevel = 0; sourceLevel <= DustLightVolume.BRIGHTEST_LEVEL; sourceLevel++) {
            assertSourceClamped(helper, sourceLevel, sourceLevel);
        }
        assertSourceClamped(helper, 20, DustLightVolume.BRIGHTEST_LEVEL);
        assertSourceClamped(helper, -3, 0);
        helper.succeed();
    }

    /**
     * The fake block light of a block, raised by the given amount.
     */
    private static int fake(int x, int y, int z, int raise) {
        return Math.floorMod(FAKE_X_WEIGHT * (long) x + FAKE_Y_WEIGHT * (long) y + FAKE_Z_WEIGHT * (long) z + raise,
                FAKE_MODULUS);
    }

    /**
     * The fake block light of a block.
     */
    private static int fake(int x, int y, int z) {
        return fake(x, y, z, 0);
    }

    /**
     * The block corner nearest an eye coordinate, as the volume is centered on it.
     */
    private static int corner(double eyeCoordinate) {
        return (int) Math.floor(eyeCoordinate + 0.5);
    }

    /**
     * Asserts the first block of the volume on each axis.
     */
    private static void assertOrigin(GameTestHelper helper, DustLightVolume volume, int expectedX, int expectedY,
            int expectedZ, String where) {
        helper.assertTrue(volume.originX() == expectedX && volume.originY() == expectedY
                        && volume.originZ() == expectedZ, "The volume " + where + " starts at " + volume.originX() + " "
                        + volume.originY() + " " + volume.originZ() + " instead of " + expectedX + " " + expectedY + " "
                        + expectedZ);
    }

    /**
     * Asserts that every block of the window holds no light.
     */
    private static void assertAllDark(GameTestHelper helper, DustLightVolume volume, String where) {
        for (int x = volume.originX(); x < volume.originX() + DustLightVolume.SIZE; x++) {
            for (int y = volume.originY(); y < volume.originY() + DustLightVolume.SIZE; y++) {
                for (int z = volume.originZ(); z < volume.originZ() + DustLightVolume.SIZE; z++) {
                    int actual = volume.light(x, y, z);
                    helper.assertTrue(actual == 0, "The block " + x + " " + y + " " + z + " " + where + " holds "
                            + actual + " instead of 0");
                }
            }
        }
    }

    /**
     * Asserts that every block of the window holds its fake block light.
     */
    private static void assertAllFake(GameTestHelper helper, DustLightVolume volume, String where) {
        for (int x = volume.originX(); x < volume.originX() + DustLightVolume.SIZE; x++) {
            for (int y = volume.originY(); y < volume.originY() + DustLightVolume.SIZE; y++) {
                for (int z = volume.originZ(); z < volume.originZ() + DustLightVolume.SIZE; z++) {
                    int actual = volume.light(x, y, z);
                    int expected = fake(x, y, z);
                    helper.assertTrue(actual == expected, "The block " + x + " " + y + " " + z + " " + where
                            + " holds " + actual + " instead of the fake " + expected);
                }
            }
        }
    }

    /**
     * Asserts that the blocks a follow brought in hold no light and that every other block of the window keeps its
     * fake block light. A block was brought in if its x lies in the x range, or its y in the y range, or its z in the
     * z range.
     */
    private static void assertEnteredBlocksAreDark(GameTestHelper helper, DustLightVolume volume, int fromX, int toX,
            int fromY, int toY, int fromZ, int toZ, String where) {
        for (int x = volume.originX(); x < volume.originX() + DustLightVolume.SIZE; x++) {
            for (int y = volume.originY(); y < volume.originY() + DustLightVolume.SIZE; y++) {
                for (int z = volume.originZ(); z < volume.originZ() + DustLightVolume.SIZE; z++) {
                    boolean entered = x >= fromX && x <= toX || y >= fromY && y <= toY || z >= fromZ && z <= toZ;
                    int actual = volume.light(x, y, z);
                    int expected = entered ? 0 : fake(x, y, z);
                    String expectedName = entered ? "0 of a block that entered the volume" : "the fake " + expected;
                    helper.assertTrue(actual == expected, "The block " + x + " " + y + " " + z + " " + where
                            + " holds " + actual + " instead of " + expectedName);
                }
            }
        }
    }

    /**
     * Asserts that a block holds the fake block light raised by 1.
     */
    private static void assertRaised(GameTestHelper helper, DustLightVolume volume, int x, int y, int z) {
        int expected = fake(x, y, z, 1);
        helper.assertTrue(volume.light(x, y, z) == expected, "The marked block " + x + " " + y + " " + z + " holds "
                + volume.light(x, y, z) + " instead of the raised fake " + expected);
    }

    /**
     * Asserts that a block holds the fake block light without a raise.
     */
    private static void assertUnraised(GameTestHelper helper, DustLightVolume volume, int x, int y, int z) {
        int expected = fake(x, y, z, 0);
        helper.assertTrue(volume.light(x, y, z) == expected, "The unmarked block " + x + " " + y + " " + z + " holds "
                + volume.light(x, y, z) + " instead of the unraised fake " + expected);
    }

    /**
     * Asserts that a source of a constant level gives the clamped level at the block 0 0 0 of a whole volume.
     */
    private static void assertSourceClamped(GameTestHelper helper, int sourceLevel, int expected) {
        DustLightVolume volume = new DustLightVolume();
        volume.follow(0, 0, 0);
        int read = volume.readMarked(new ConstantSource(sourceLevel), DustLightVolume.BLOCKS);
        helper.assertTrue(read == DustLightVolume.BLOCKS, "The volume of the source " + sourceLevel + " reads " + read
                + " blocks instead of " + DustLightVolume.BLOCKS);
        int actual = volume.light(0, 0, 0);
        helper.assertTrue(actual == expected, "The source " + sourceLevel + " gives the light " + actual
                + " at 0 0 0 instead of " + expected);
    }

    /**
     * The fake block light of the tests, raised by a settable amount, counting the blocks it is asked for.
     */
    private static final class FakeSource implements DustLightVolume.LightSource {
        private int calls;
        private int raise;

        @Override
        public int blockLight(int x, int y, int z) {
            calls++;
            return fake(x, y, z, raise);
        }
    }

    /**
     * A source of one constant block light level.
     */
    private static final class ConstantSource implements DustLightVolume.LightSource {
        private final int level;

        private ConstantSource(int level) {
            this.level = level;
        }

        @Override
        public int blockLight(int x, int y, int z) {
            return level;
        }
    }
}
