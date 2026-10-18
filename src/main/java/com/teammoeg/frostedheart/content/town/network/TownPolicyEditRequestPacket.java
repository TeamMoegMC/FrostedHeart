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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownCareLaw;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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
