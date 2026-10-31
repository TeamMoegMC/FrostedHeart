/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real dependent outlets and local edits must keep their own physical boundary. */
@GameTestHolder("frostedheart_region_dependencies")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRegionDependencyProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 3000)
    public static void affectedOutletAndLocalRoomWaitForTheirGeometry(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirRegionalBoundaryProductionGameTests.runDependencies(helper);
    }
}
