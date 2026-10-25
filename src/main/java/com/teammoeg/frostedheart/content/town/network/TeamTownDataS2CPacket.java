/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.network;

import java.util.UUID;
import java.util.function.Supplier;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.chorda.io.codec.DataOps;
import com.teammoeg.chorda.io.codec.ObjectWriter;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Frozen full view with explicit unavailable/absent semantics. */
public final class TeamTownDataS2CPacket implements CMessage {
    public enum State { PENDING, PRESENT, ABSENT }
    private final State state;
    private final UUID holderId;
    private final Object data;
    private final Object transport;

    private TeamTownDataS2CPacket(State state, UUID holderId, Object data, Object transport) {
        this.state = state;
        this.holderId = holderId;
        this.data = data;
        this.transport = transport;
    }

    public static TeamTownDataS2CPacket pending(UUID holderId) {
        return new TeamTownDataS2CPacket(State.PENDING, holderId, null, null);
    }

    public static TeamTownDataS2CPacket absent(UUID holderId) {
        return new TeamTownDataS2CPacket(State.ABSENT, holderId, null, null);
    }

    public static TeamTownDataS2CPacket present(UUID holderId, TeamTownData town) throws Exception {
        Object data = CodecUtil.encodeOrThrow(TeamTownData.MANAGEMENT_CODEC.encodeStart(DataOps.COMPRESSED, town));
        Object transport = CodecUtil.encodeOrThrow(TownTransportSnapshot.CODEC.encodeStart(
                DataOps.COMPRESSED, town.createTeamTown().getTransportSnapshot()));
        return new TeamTownDataS2CPacket(State.PRESENT, java.util.Objects.requireNonNull(holderId), data, transport);
    }

    /** Compatibility for codec tests; production sends use TownSync and the real holder ID. */
    public TeamTownDataS2CPacket(TeamTownData town) {
        this(captureForTest(town));
    }

    private TeamTownDataS2CPacket(TeamTownDataS2CPacket captured) {
        this(captured.state, captured.holderId, captured.data, captured.transport);
    }

    private static TeamTownDataS2CPacket captureForTest(TeamTownData town) {
        try { return present(new UUID(0, 0), town); }
        catch (Exception failure) { throw new IllegalStateException("Unable to capture town view", failure); }
    }

    public TeamTownDataS2CPacket(FriendlyByteBuf buffer) {
        state = buffer.readEnum(State.class);
        holderId = buffer.readBoolean() ? buffer.readUUID() : null;
        data = state == State.PRESENT ? ObjectWriter.readObject(buffer) : null;
        transport = state == State.PRESENT ? ObjectWriter.readObject(buffer) : null;
    }

    TeamTownData decodeTownData() throws Exception {
        TeamTownData town = CodecUtil.decodeOrThrow(TeamTownData.MANAGEMENT_CODEC.decode(DataOps.COMPRESSED, data));
        TownTransportSnapshot snapshot = CodecUtil.decodeOrThrow(
                TownTransportSnapshot.CODEC.decode(DataOps.COMPRESSED, transport));
        town.applyTransportSnapshot(snapshot);
        return town;
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!com.teammoeg.frostedheart.content.town.client.TownClientUpdates.isCurrentConnection(context.get().getNetworkManager())) return;
            try {
                switch (state) {
                    case PENDING -> TownClientUpdates.pending(holderId);
                    case ABSENT -> TownClientUpdates.install(holderId, null);
                    case PRESENT -> TownClientUpdates.install(holderId, decodeTownData());
                }
            } catch (Exception failure) { TownClientUpdates.failed(failure); }
        });
        context.get().setPacketHandled(true);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(state);
        buffer.writeBoolean(holderId != null);
        if (holderId != null) buffer.writeUUID(holderId);
        if (state == State.PRESENT) {
            ObjectWriter.writeObject(buffer, data);
            ObjectWriter.writeObject(buffer, transport);
        }
    }
}
