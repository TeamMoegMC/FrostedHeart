/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.TownStaffingPlan;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TownStaffingPacketTest {
    @Test
    void editOperationsRoundTripWithoutTownIdentity() {
        BlockPos moved = new BlockPos(1, 64, 2);
        BlockPos before = new BlockPos(3, 64, 4);
        TownStaffingEditRequestPacket move = roundTrip(
                TownStaffingEditRequestPacket.move(moved, Optional.of(before)));
        assertEquals(TownStaffingEditRequestPacket.Action.MOVE, move.action());
        assertEquals(moved, move.building());
        assertEquals(Optional.of(before), move.before());

        TownStaffingEditRequestPacket target = roundTrip(
                TownStaffingEditRequestPacket.setTarget(moved, 7));
        assertEquals(TownStaffingEditRequestPacket.Action.SET_TARGET, target.action());
        assertEquals(7, target.target());
    }

    @Test
    void authoritativePlanRoundTripsInQueueOrder() {
        TownStaffingPlan plan = new TownStaffingPlan(List.of(
                new TownStaffingPlan.Entry(new BlockPos(5, 1, 0), 4),
                new TownStaffingPlan.Entry(new BlockPos(2, 1, 0), 1)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        new TownStaffingPlanUpdatePacket(plan).encode(buffer);

        TownStaffingPlanUpdatePacket decoded = new TownStaffingPlanUpdatePacket(buffer);

        assertEquals(plan, decoded.plan());
    }

    @Test
    void packetBoundariesRejectOversizedPlansAndClampNegativeTargets() {
        BlockPos pos = BlockPos.ZERO;
        assertEquals(0, roundTrip(
                TownStaffingEditRequestPacket.setTarget(pos, -10)).target());

        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeVarInt(1_025);
        assertThrows(IllegalArgumentException.class,
                () -> new TownStaffingPlanUpdatePacket(oversized));
    }

    private static TownStaffingEditRequestPacket roundTrip(
            TownStaffingEditRequestPacket packet
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        return new TownStaffingEditRequestPacket(buffer);
    }
}
