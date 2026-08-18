/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownPolicyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Full bounded wire representation of the extensible town policy state. */
public final class TownPolicyStateUpdatePacket implements CMessage {
    private static final int MAX_DOMAINS = 64;
    private static final int MAX_ID_LENGTH = 64;
    private final TownPolicyState state;

    public TownPolicyStateUpdatePacket(TownPolicyState state) { this.state = state; }

    public TownPolicyStateUpdatePacket(FriendlyByteBuf buffer) {
        Map<String, String> selections = readMap(buffer);
        Map<String, String> pending = readMap(buffer);
        state = new TownPolicyState(selections, pending, buffer.readLong());
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        writeMap(buffer, state.selections());
        writeMap(buffer, state.pending());
        buffer.writeLong(state.changedAtTownDay());
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .ifPresent(town -> town.applyPolicyState(state)));
        context.get().setPacketHandled(true);
    }

    private static Map<String, String> readMap(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_DOMAINS) throw new IllegalArgumentException("Invalid policy domain count");
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            result.put(buffer.readUtf(MAX_ID_LENGTH), buffer.readUtf(MAX_ID_LENGTH));
        }
        return result;
    }

    private static void writeMap(FriendlyByteBuf buffer, Map<String, String> values) {
        if (values.size() > MAX_DOMAINS) throw new IllegalArgumentException("Too many policy domains");
        buffer.writeVarInt(values.size());
        values.forEach((domain, option) -> {
            buffer.writeUtf(domain, MAX_ID_LENGTH);
            buffer.writeUtf(option, MAX_ID_LENGTH);
        });
    }

    public TownPolicyState state() { return state; }
}
