/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_busy_lifecycle")
@PrefixGameTestTemplate(false)
public final class ContinuousAirBusyLifecycleProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 12500)
    public static void twoDenseMachineBasesSurviveForcedSaveAndConstructionBeforeUnload(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(true, "Use the configured shared-grid backend");
        ContinuousAirTwoBaseProductionGameTests.runBusyLifecycle(helper);
    }
}
