/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player.thermalitem;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarmStoneGateBPacketCounterTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @AfterEach
    void stopCounter() {
        WarmStoneGateBPacketCounter.stop();
    }

    @Test
    void remainsDefaultOffUntilStarted() {
        WarmStoneGateBPacketCounter.stop();
        WarmStoneGateBPacketCounter.reset();

        WarmStoneGateBPacketCounter.onProbeError("disabled");

        WarmStoneGateBPacketCounter.Snapshot snapshot =
                WarmStoneGateBPacketCounter.snapshot();
        assertFalse(snapshot.enabled());
        assertEquals(0L, snapshot.probeErrors());
    }

    @Test
    void separatesAllPacketsFromThermalReservoirPackets() {
        WarmStoneGateBPacketCounter.Counts counts =
                new WarmStoneGateBPacketCounter.Counts();
        counts.recordCurios(false);
        counts.recordCurios(true);
        counts.recordContainerSlot(false);
        counts.recordContainerSlot(true);
        counts.recordContainerContent(2);

        WarmStoneGateBPacketCounter.Snapshot snapshot = counts.snapshot(true, 0L);
        assertEquals(2L, snapshot.curiosStackPackets());
        assertEquals(1L, snapshot.thermalCuriosStackPackets());
        assertEquals(2L, snapshot.containerSlotPackets());
        assertEquals(1L, snapshot.thermalContainerSlotPackets());
        assertEquals(1L, snapshot.containerContentPackets());
        assertEquals(1L, snapshot.thermalContainerContentPackets());
        assertEquals(2L, snapshot.thermalStacksInContentPackets());
        assertEquals(0L, snapshot.probeErrors());
    }

    @Test
    void resetKeepsRunStateAndStopFreezesTheCounter() {
        WarmStoneGateBPacketCounter.start();
        WarmStoneGateBPacketCounter.onProbeError("before_reset");

        WarmStoneGateBPacketCounter.Snapshot reset =
                WarmStoneGateBPacketCounter.reset();
        assertTrue(reset.enabled());
        assertEquals(0L, reset.probeErrors());

        WarmStoneGateBPacketCounter.onProbeError("after_reset");
        WarmStoneGateBPacketCounter.onProbeError("after_reset_again");
        WarmStoneGateBPacketCounter.Snapshot stopped =
                WarmStoneGateBPacketCounter.stop();
        assertFalse(stopped.enabled());
        assertEquals(2L, stopped.probeErrors());

        WarmStoneGateBPacketCounter.onProbeError("after_stop");
        assertEquals(2L,
                WarmStoneGateBPacketCounter.snapshot().probeErrors());
    }
}
