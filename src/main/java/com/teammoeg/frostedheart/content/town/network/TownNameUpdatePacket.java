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

import java.util.function.Supplier;

import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TownNamingModel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Lightweight authoritative town-name update. */
public class TownNameUpdatePacket implements TownViewPacket {
    private final String name;

    public TownNameUpdatePacket(String name) {
        this.name = name;
    }

    public TownNameUpdatePacket(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf(TownNamingModel.MAX_TOWN_NAME_LENGTH);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(name, TownNamingModel.MAX_TOWN_NAME_LENGTH);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!com.teammoeg.frostedheart.content.town.client.TownClientUpdates.canApply(context.get().getNetworkManager())) return;
            com.teammoeg.frostedheart.content.town.client.TownClientUpdates.apply(town -> town.applyNameUpdate(name));
        });
        context.get().setPacketHandled(true);
    }
}
