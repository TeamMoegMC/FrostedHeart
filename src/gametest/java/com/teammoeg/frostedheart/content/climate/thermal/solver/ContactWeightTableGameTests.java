/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_contact_weights")
@PrefixGameTestTemplate(false)
public final class ContactWeightTableGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void uniformPaletteAndDenseBlocksPreserveEveryRawBit(GameTestHelper helper) {
        var source = new DoubleArrayList();
        double nan = Double.longBitsToDouble(0x7ff8000000000042L);
        for (int i = 0; i < 4096; i++) source.add(-0.0);
        for (int i = 0; i < 4096; i++) source.add(nan);
        double[] values = {+0.0, -0.0, nan, -1.0, 0.25};
        for (int i = 0; i < 4096; i++) source.add(values[i % values.length]);
        for (int i = 0; i < 4096; i++) source.add(i + 0.125);
        source.add(Double.NEGATIVE_INFINITY);
        var preparation = new ContactWeightTable.Preparation(source, null);
        helper.assertTrue(!preparation.run(0), "An expired slice yields between blocks");
        int resumes = 0;
        while (!preparation.run(0)) helper.assertTrue(++resumes < 5, "Every resumed slice completes a block");
        assertBits(helper, source, preparation.result());
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void reuseAndChangedTailLeavePreviousWeightsIntact(GameTestHelper helper) {
        var empty = new ContactWeightTable.Preparation(new DoubleArrayList(), null);
        helper.assertTrue(empty.run(0), "An empty weight table needs no block work");
        var source = new DoubleArrayList();
        for (int i = 0; i < 4096; i++) source.add(1.0);
        for (int i = 0; i < 4096; i++) source.add(i % 2 == 0 ? 0.5 : -0.5);
        source.add(-0.0);
        var originalValues = new DoubleArrayList(source);
        var first = build(source, empty.result());
        var reused = build(source, first);
        source.set(4097, -0.25);
        source.add(+0.0);
        var changed = build(source, reused);
        assertBits(helper, originalValues, first);
        assertBits(helper, originalValues, reused);
        assertBits(helper, source, changed);
        source.removeDouble(source.size() - 1);
        assertBits(helper, source, build(source, changed));
        source.clear();
        source.add(0.75);
        var single = build(source, changed);
        helper.assertTrue(single.get(0) == 0.75, "A smaller next table replaces the former block contents");
        assertBits(helper, originalValues, first);
        helper.succeed();
    }

    private static ContactWeightTable build(DoubleArrayList source, ContactWeightTable previous) {
        var preparation = new ContactWeightTable.Preparation(source, previous);
        while (!preparation.run(0)) { }
        return preparation.result();
    }

    private static void assertBits(GameTestHelper helper, DoubleArrayList expected, ContactWeightTable actual) {
        for (int i = 0; i < expected.size(); i++)
            helper.assertTrue(Double.doubleToRawLongBits(expected.getDouble(i)) == Double.doubleToRawLongBits(actual.get(i)),
                    "Contact weight changed at entry " + i);
    }
}
