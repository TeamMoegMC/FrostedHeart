/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The previous dual-base construction case must now prove a real unresolved-route boundary. */
@GameTestHolder("frostedheart_pending_route_boundary")
@PrefixGameTestTemplate(false)
public final class ContinuousAirPendingRouteBoundaryProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 3000)
    public static void unfinishedDistantPassagesDoNotHoldTheIndependentRoom(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirRegionalBoundaryProductionGameTests.run(helper, true);
    }
}
