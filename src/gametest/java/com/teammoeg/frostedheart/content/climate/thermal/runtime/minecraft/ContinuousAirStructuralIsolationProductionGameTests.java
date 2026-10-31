/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_structural_isolation")
@PrefixGameTestTemplate(false)
public final class ContinuousAirStructuralIsolationProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2200)
    public static void newRoomInstallsDuringUnfinishedDenseConstruction(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirIndependentIntakeProductionGameTests.run(helper, true);
    }
}
