/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.AbstractTeam;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CBaseNetwork;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotice;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TownTransportShortageRecipientTest {
    @Test
    void sendToOnlineDeliversOneSharedNoticeToEveryOnlineTeamMember() {
        AbstractTeam team = new StubTeam(Arrays.asList(null, null));
        TeamDataHolder holder = new TeamDataHolder(team.getId(), team);
        RecordingNetwork network = new RecordingNetwork();
        TownTransportShortageNotificationPacket packet =
                new TownTransportShortageNotificationPacket(1L, List.of(
                        TownTransportShortageNotice.from(0.0, 20.0).orElseThrow()));

        holder.sendToOnline(network, packet);

        assertEquals(2, network.sent.size());
        assertSame(packet, network.sent.get(0));
        assertSame(packet, network.sent.get(1));
    }

    private record StubTeam(Collection<ServerPlayer> onlineMembers) implements AbstractTeam {
        @Override
        public Collection<ServerPlayer> getOnlineMembers() {
            return onlineMembers;
        }

        @Override
        public UUID getId() {
            return UUID.fromString("00000000-0000-0000-0000-000000000011");
        }

        @Override
        public String getName() {
            return "transport-test";
        }

        @Override
        public UUID getOwner() {
            return getId();
        }
    }

    private static final class RecordingNetwork extends CBaseNetwork {
        private final List<CMessage> sent = new java.util.ArrayList<>();

        private RecordingNetwork() {
            super("frostedheart-test");
        }

        @Override
        public void registerMessages() {
        }

        @Override
        public void sendPlayer(ServerPlayer player, CMessage message) {
            sent.add(message);
        }
    }
}
