/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.frostedheart.content.town.TownCareLaw;
import com.teammoeg.frostedheart.content.town.TownHousingPlan;
import com.teammoeg.frostedheart.content.town.TownPolicyState;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TownHousingPolicyPacketTest {
    @Test
    void housingEditsAndAuthoritativePlanRoundTrip() {
        BlockPos house = new BlockPos(2, 64, 3);
        BlockPos before = new BlockPos(7, 64, 8);
        TownHousingEditRequestPacket move = roundTrip(
                TownHousingEditRequestPacket.move(house, Optional.of(before)));
        assertEquals(TownHousingEditRequestPacket.Action.MOVE, move.action());
        assertEquals(Optional.of(before), move.before());
        assertEquals(0, roundTrip(
                TownHousingEditRequestPacket.setGuarantee(house, -5)).target());

        TownHousingPlan plan = new TownHousingPlan(List.of(
                new TownHousingPlan.Entry(house, 3),
                new TownHousingPlan.Entry(before, 1)));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        new TownHousingPlanUpdatePacket(plan).encode(buffer);
        assertEquals(plan, new TownHousingPlanUpdatePacket(buffer).plan());
    }

    @Test
    void careLawAndGenericPolicyStateRoundTrip() {
        FriendlyByteBuf lawBuffer = new FriendlyByteBuf(Unpooled.buffer());
        new TownPolicyEditRequestPacket(TownCareLaw.WORKFORCE_FIRST).encode(lawBuffer);
        assertEquals(TownCareLaw.WORKFORCE_FIRST,
                new TownPolicyEditRequestPacket(lawBuffer).law());

        TownPolicyState state = new TownPolicyState(
                Map.of(TownPolicyState.RESIDENTIAL_CARE, TownCareLaw.DEPENDENT_FIRST.id()),
                Map.of(TownPolicyState.RESIDENTIAL_CARE, TownCareLaw.WORKFORCE_FIRST.id()), 27);
        FriendlyByteBuf stateBuffer = new FriendlyByteBuf(Unpooled.buffer());
        new TownPolicyStateUpdatePacket(state).encode(stateBuffer);
        assertEquals(state, new TownPolicyStateUpdatePacket(stateBuffer).state());
    }

    @Test
    void oversizedHousingPlanIsRejectedAtPacketBoundary() {
        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeVarInt(1_025);
        assertThrows(IllegalArgumentException.class,
                () -> new TownHousingPlanUpdatePacket(oversized));
    }

    private static TownHousingEditRequestPacket roundTrip(
            TownHousingEditRequestPacket packet
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buffer);
        return new TownHousingEditRequestPacket(buffer);
    }
}
