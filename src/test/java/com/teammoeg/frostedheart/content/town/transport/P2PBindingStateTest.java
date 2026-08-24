/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class P2PBindingStateTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void orderIndependentRolesProduceTheExpectedDirections() {
        P2PTerminalEndpoint shipping = endpoint(P2PTerminalRole.SHIPPING, 0);
        P2PTerminalEndpoint receiving = endpoint(P2PTerminalRole.RECEIVING, 10);
        P2PTerminalEndpoint bidirectional = endpoint(P2PTerminalRole.BIDIRECTIONAL, 20);

        assertDirections(shipping, receiving, List.of("0->10"));
        assertDirections(receiving, shipping, List.of("0->10"));
        assertDirections(shipping, bidirectional, List.of("0->20"));
        assertDirections(bidirectional, receiving, List.of("20->10"));
        assertDirections(bidirectional, endpoint(P2PTerminalRole.BIDIRECTIONAL, 30),
                List.of("20->30", "30->20"));

        assertThrows(IllegalArgumentException.class, () -> P2PBindingState.EMPTY.planConnection(
                shipping, endpoint(P2PTerminalRole.SHIPPING, 1), 20, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> P2PBindingState.EMPTY.planConnection(
                receiving, endpoint(P2PTerminalRole.RECEIVING, 1), 20, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> P2PBindingState.EMPTY.planConnection(
                shipping, shipping, 20, UUID.randomUUID()));
    }

    @Test
    void receivingEndpointAcceptsManySourcesButEachSenderHasOneTarget() {
        P2PTerminalEndpoint receiver = endpoint(P2PTerminalRole.RECEIVING, 20);
        P2PBindingState state = P2PBindingState.EMPTY;
        P2PBindingState.BindingPlan first = state.planConnection(
                endpoint(P2PTerminalRole.SHIPPING, 0), receiver, 20, uuid(1));
        state = state.apply(first);
        P2PBindingState.BindingPlan second = state.planConnection(
                endpoint(P2PTerminalRole.SHIPPING, 1), receiver, 20, uuid(2));
        state = state.apply(second);

        assertEquals(2, state.incoming(receiver.pos()).size());
        assertEquals(2, state.bindings().size());

        P2PBindingState.BindingPlan rebind = state.planConnection(
                endpoint(P2PTerminalRole.SHIPPING, 0),
                endpoint(P2PTerminalRole.RECEIVING, 30), 20, uuid(3));
        assertEquals(List.of(uuid(1)), rebind.removedConnectionIds().stream().toList());
        state = state.apply(rebind);
        assertEquals(1, state.incoming(receiver.pos()).size());
        assertEquals(2, state.bindings().size());
    }

    @Test
    void bidirectionalRebindRemovesBothOldDirectionsAndRetainsOutgoingRate() {
        P2PTerminalEndpoint first = endpoint(P2PTerminalRole.BIDIRECTIONAL, 0);
        P2PTerminalEndpoint second = endpoint(P2PTerminalRole.BIDIRECTIONAL, 10);
        P2PBindingState state = P2PBindingState.EMPTY.apply(
                P2PBindingState.EMPTY.planConnection(first, second, 20, uuid(1)));
        state = state.withRate(first.pos(), 35);

        P2PBindingState.BindingPlan plan = state.planConnection(first,
                endpoint(P2PTerminalRole.RECEIVING, 20), 20, uuid(2));

        assertEquals(List.of(uuid(1)), plan.removedConnectionIds().stream().toList());
        assertEquals(1, plan.newBindings().size());
        assertEquals(35, plan.newBindings().get(0).rateItemsPerSecond());
        P2PBindingState rebound = state.apply(plan);
        assertEquals(1, rebound.bindings().size());
        assertTrue(rebound.connection(uuid(1)).isEmpty());
        assertEquals(uuid(2), rebound.bindings().get(0).connectionId());
    }

    @Test
    void stateCodecIsStableAndUnknownRolesAreRejected() {
        P2PBindingState state = P2PBindingState.EMPTY.apply(P2PBindingState.EMPTY.planConnection(
                endpoint(P2PTerminalRole.BIDIRECTIONAL, 0),
                endpoint(P2PTerminalRole.BIDIRECTIONAL, 10), 20, uuid(1)));
        state = state.withEndpointRedstonePowered(
                endpoint(P2PTerminalRole.BIDIRECTIONAL, 10).pos(), true);

        JsonElement encoded = P2PBindingState.CODEC.encodeStart(JsonOps.INSTANCE, state)
                .result().orElseThrow();
        P2PBindingState decoded = P2PBindingState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();
        assertEquals(state.bindings(), decoded.bindings());
        assertEquals("bidirectional", P2PTerminalRole.CODEC.encodeStart(
                JsonOps.INSTANCE, P2PTerminalRole.BIDIRECTIONAL).result().orElseThrow().getAsString());
        assertFalse(P2PTerminalRole.CODEC.parse(JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive("future_role")).result().isPresent());

        encoded.getAsJsonObject().getAsJsonArray("bindings").forEach(element -> {
            element.getAsJsonObject().remove("senderRedstonePowered");
            element.getAsJsonObject().remove("receiverRedstonePowered");
        });
        P2PBindingState migrated = P2PBindingState.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();
        assertTrue(migrated.bindings().stream().allMatch(P2PDirectedBinding::redstonePaused));
    }

    @Test
    void routeCardStateKeepsSelectionAndConnectionMutuallyExclusive() {
        P2PRouteCardState selected = P2PRouteCardState.selected(
                endpoint(P2PTerminalRole.SHIPPING, 0));
        P2PRouteCardState connected = P2PRouteCardState.connected(uuid(1));

        assertTrue(selected.selectedEndpoint().isPresent());
        assertTrue(selected.connectionId().isEmpty());
        assertTrue(connected.selectedEndpoint().isEmpty());
        assertEquals(uuid(1), connected.connectionId().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> new P2PRouteCardState(
                selected.selectedEndpoint(), connected.connectionId()));
    }

    private static void assertDirections(
            P2PTerminalEndpoint first,
            P2PTerminalEndpoint second,
            List<String> expected
    ) {
        P2PBindingState.BindingPlan plan = P2PBindingState.EMPTY.planConnection(
                first, second, 20, uuid(9));
        List<String> actual = plan.newBindings().stream()
                .map(binding -> binding.sender().pos().pos().getX() + "->"
                        + binding.receiver().pos().pos().getX())
                .toList();
        assertEquals(expected, actual);
    }

    private static P2PTerminalEndpoint endpoint(P2PTerminalRole role, int x) {
        return new P2PTerminalEndpoint(
                GlobalPos.of(Level.OVERWORLD, new BlockPos(x, 64, 0)), role);
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
