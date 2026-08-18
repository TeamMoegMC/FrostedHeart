/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownCareLaw;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests a pending law choice; all policy domains share one cooldown. */
public final class TownPolicyEditRequestPacket implements CMessage {
    private final TownCareLaw law;

    public TownPolicyEditRequestPacket(TownCareLaw law) {
        this.law = law == null ? TownCareLaw.CLINICAL_TRIAGE : law;
    }

    public TownPolicyEditRequestPacket(FriendlyByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        if (ordinal >= TownCareLaw.values().length) throw new IllegalArgumentException("Unknown care law");
        law = TownCareLaw.values()[ordinal];
    }

    @Override
    public void encode(FriendlyByteBuf buffer) { buffer.writeByte(law.ordinal()); }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            var teamData = CTeamDataManager.get(player);
            teamData.getOptional(FHSpecialDataTypes.TOWN_DATA).ifPresent(town -> {
                boolean changed = town.requestCareLaw(law);
                TownPolicyStateUpdatePacket packet =
                        new TownPolicyStateUpdatePacket(town.getPolicyState());
                if (changed) teamData.sendToOnline(FHNetwork.INSTANCE, packet);
                else FHNetwork.INSTANCE.sendPlayer(player, packet);
            });
        });
        context.get().setPacketHandled(true);
    }

    public TownCareLaw law() { return law; }
}
