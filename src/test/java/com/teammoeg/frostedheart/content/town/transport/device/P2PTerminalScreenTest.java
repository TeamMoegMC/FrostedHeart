/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.frostedheart.bootstrap.common.FHMenuSlots;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalEndpoint;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P2PTerminalScreenTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void rateScrollMatchesWarehouseInterfaceIncrementsAndBounds() {
        assertEquals(21, P2PTerminalScreen.adjustRateForScroll(
                20, 1, false, false, 1280));
        assertEquals(28, P2PTerminalScreen.adjustRateForScroll(
                20, 1, true, false, 1280));
        assertEquals(36, P2PTerminalScreen.adjustRateForScroll(
                20, 1, false, true, 1280));
        assertEquals(84, P2PTerminalScreen.adjustRateForScroll(
                20, 1, true, true, 1280));
        assertEquals(1280, P2PTerminalScreen.adjustRateForScroll(
                1270, 1, false, true, 1280));
        assertEquals(0, P2PTerminalScreen.adjustRateForScroll(
                4, -1, true, false, 1280));
        assertEquals(20, P2PTerminalScreen.rateForScroll("bad", 20, 1280));
    }

    @Test
    void endpointAndBidirectionalFilterSelectorsMapToTheirOwnedFilters() {
        assertTrue(P2PTerminalScreen.sendFilterFor(P2PTerminalRole.SHIPPING, false));
        assertFalse(P2PTerminalScreen.sendFilterFor(P2PTerminalRole.RECEIVING, true));
        assertTrue(P2PTerminalScreen.sendFilterFor(P2PTerminalRole.BIDIRECTIONAL, true));
        assertFalse(P2PTerminalScreen.sendFilterFor(P2PTerminalRole.BIDIRECTIONAL, false));
    }

    @Test
    void bidirectionalBufferGroupsHaveAVisibleGap() {
        assertEquals(7, P2PTerminalMenu.bufferSlotX(0));
        assertEquals(61, P2PTerminalMenu.bufferSlotX(3));
        assertEquals(97, P2PTerminalMenu.bufferSlotX(4));
        assertEquals(151, P2PTerminalMenu.bufferSlotX(7));
    }

    @Test
    void everyRoleHasItsOwnContainerTitle() {
        assertEquals("container.frostedheart.shipping_terminal",
                P2PTerminalBlockEntity.displayNameKey(P2PTerminalRole.SHIPPING));
        assertEquals("container.frostedheart.receiving_terminal",
                P2PTerminalBlockEntity.displayNameKey(P2PTerminalRole.RECEIVING));
        assertEquals("container.frostedheart.bidirectional_logistics_terminal",
                P2PTerminalBlockEntity.displayNameKey(P2PTerminalRole.BIDIRECTIONAL));
    }

    @Test
    void visualStateNamesSpecificUnavailableFactsAndKeepsPriorityStable() {
        assertEquals(P2PTerminalVisualState.IDLE,
                P2PTerminalBlockEntity.selectVisualState(
                        true, false, false, false, false, false));
        assertEquals(P2PTerminalVisualState.RECEIVER_CONTAINER_UNAVAILABLE,
                P2PTerminalBlockEntity.selectVisualState(
                        true, false, false, true, false, false));
        assertEquals(P2PTerminalVisualState.PEER_UNLOADED,
                P2PTerminalBlockEntity.selectVisualState(
                        true, false, false, false, true, false));
        assertEquals(P2PTerminalVisualState.TRANSFERRING,
                P2PTerminalBlockEntity.selectVisualState(
                        true, false, false, false, false, true));
        assertEquals(P2PTerminalVisualState.REDSTONE_PAUSED,
                P2PTerminalBlockEntity.selectVisualState(
                        true, true, false, true, true, true));
        assertEquals(P2PTerminalVisualState.UNBOUND,
                P2PTerminalBlockEntity.selectVisualState(
                        false, false, false, true, true, true));
    }

    @Test
    void incomingConnectionDescriptionStillUsesSenderToReceiverOrder() {
        P2PTerminalEndpoint shipping = new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 70, 8)),
                P2PTerminalRole.SHIPPING);
        P2PTerminalConnectionView incoming = new P2PTerminalConnectionView(
                new UUID(0L, 1L), shipping, -1, 20, true,
                Optional.empty(), Optional.empty());

        Component flow = P2PTerminalScreen.connectionFlows(incoming).get(0).label();
        TranslatableContents contents = (TranslatableContents) flow.getContents();
        assertEquals("gui.frostedheart.p2p_terminal.connection_flow", contents.getKey());
        assertEquals("gui.frostedheart.p2p_terminal.role.shipping",
                ((TranslatableContents) ((Component) contents.getArgs()[0])
                        .getContents()).getKey());
        assertEquals("gui.frostedheart.p2p_terminal.role.local",
                ((TranslatableContents) ((Component) contents.getArgs()[1])
                        .getContents()).getKey());
        assertEquals("20", contents.getArgs()[2]);
    }

    @Test
    void bidirectionalConnectionUsesOneNormalLinePerDirection() {
        P2PTerminalEndpoint peer = new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 70, 8)),
                P2PTerminalRole.BIDIRECTIONAL);
        P2PTerminalConnectionView connection = new P2PTerminalConnectionView(
                new UUID(0L, 2L), peer, 20, 20, true,
                Optional.empty(), Optional.empty());

        var flows = P2PTerminalScreen.connectionFlows(connection);

        assertEquals(2, flows.size());
        TranslatableContents outgoing = (TranslatableContents) flows.get(0).label().getContents();
        TranslatableContents incoming = (TranslatableContents) flows.get(1).label().getContents();
        assertEquals("gui.frostedheart.p2p_terminal.role.local",
                ((TranslatableContents) ((Component) outgoing.getArgs()[0])
                        .getContents()).getKey());
        assertEquals("gui.frostedheart.p2p_terminal.role.bidirectional",
                ((TranslatableContents) ((Component) incoming.getArgs()[0])
                        .getContents()).getKey());
        assertEquals("20", outgoing.getArgs()[2]);
        assertEquals("20", incoming.getArgs()[2]);
        assertEquals("gui.frostedheart.p2p_terminal.connection_flow_source",
                ((TranslatableContents) flows.get(0).sourceLine().getContents()).getKey());
        assertEquals("gui.frostedheart.p2p_terminal.connection_flow_target_rate",
                ((TranslatableContents) flows.get(0).targetRateLine().getContents()).getKey());
    }

    @Test
    void menuSyncHasNoFalseUnboundDefaultBeforeTheFirstPacket() {
        assertNull(FHMenuSlots.P2P_TERMINAL_MENU_VIEW_ENCODER_SLOT.getDefault());
    }
}
