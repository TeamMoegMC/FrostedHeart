/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_retirement_reload")
@PrefixGameTestTemplate(false)
public final class ThermalRetirementReloadGameTests {
    private ThermalRetirementReloadGameTests() { }

    @GameTest(template = "phase0a_empty", timeoutTicks = 8000)
    public static void ventilatedRoomCoolsUnloadsRetiresAndRelights(GameTestHelper helper) {
        ThermalRetirementProductionGameTests.realCampfireStopsExpandingCoolsAndRelights(helper);
    }
}
