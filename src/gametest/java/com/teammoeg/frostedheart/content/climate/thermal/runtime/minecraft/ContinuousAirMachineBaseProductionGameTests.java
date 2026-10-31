/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual fuelled furnaces and irregular obstacles in the existing two dense-source bases. */
@GameTestHolder("frostedheart_two_dense_machine_bases")
@PrefixGameTestTemplate(false)
public final class ContinuousAirMachineBaseProductionGameTests {
    private ContinuousAirMachineBaseProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 2500)
    public static void operatingMachinesKeepBothDenseBasesCurrent(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirTwoBaseProductionGameTests.run(helper, true);
    }
}
