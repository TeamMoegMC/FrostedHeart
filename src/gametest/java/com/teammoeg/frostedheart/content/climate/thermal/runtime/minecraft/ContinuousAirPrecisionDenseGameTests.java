/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Existing real 128-source base, including the all-off cooling window. */
@GameTestHolder("frostedheart_precision_128")
@PrefixGameTestTemplate(false)
public final class ContinuousAirPrecisionDenseGameTests {
    private ContinuousAirPrecisionDenseGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 2500)
    public static void denseSourceBaseIncludesPoweredAndUnpoweredCuts(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        ContinuousAirLargeBaseProductionGameTests.run(helper, 2400, true);
    }
}
