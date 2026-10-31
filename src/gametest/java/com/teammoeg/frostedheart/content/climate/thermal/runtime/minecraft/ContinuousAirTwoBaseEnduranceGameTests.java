/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Target-load endurance reuses the two-base production scene and ordinary world lifecycle. */
@GameTestHolder("frostedheart_two_base_endurance")
@PrefixGameTestTemplate(false)
public final class ContinuousAirTwoBaseEnduranceGameTests {
    private ContinuousAirTwoBaseEnduranceGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 12500)
    public static void tenMinutesOfTwoDenseMachineBasesAndRealChunkLifecycle(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(true, "Exercise the formal shared-grid configuration");
        int ticks = Integer.parseInt(System.getenv().getOrDefault("FH_CONTINUOUS_AIR_TWO_BASE_TICKS", "12000"));
        helper.assertTrue(ticks == 4000 || ticks == 12000, "Run either one complete lifecycle cycle or the full target endurance");
        ContinuousAirTwoBaseProductionGameTests.run(helper, true, ticks, true);
    }
}
