/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** The two repeatable comparison scenes can run without the heavier correctness fixtures. */
@GameTestHolder("frostedheart_infrared_bandwidth_measure")
@PrefixGameTestTemplate(false)
public final class InfraredBandwidthMeasurementGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void constantField(GameTestHelper helper) {
        InfraredBandwidthGameTests.constantFieldDoesNotResendItsCommittedColors(helper);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void movedWindow(GameTestHelper helper) {
        InfraredBandwidthGameTests.movingOneSectionReusesTheOverlappingField(helper);
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 500)
    public static void pairedConstantField(GameTestHelper helper) {
        InfraredBandwidthGameTests.pairedConstantField(helper);
    }
}
