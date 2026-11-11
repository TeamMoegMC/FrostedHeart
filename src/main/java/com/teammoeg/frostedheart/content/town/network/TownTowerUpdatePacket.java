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

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.content.town.client.TownClientUpdates;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * 能量塔搬了、建好了或拆了：把新位置告诉队伍成员，界面上的建筑方位跟着变。
 * <p>
 * The energy tower moved, was built or was torn down: tells the team where it stands now, so the building directions
 * on screen follow.
 */
public class TownTowerUpdatePacket implements TownViewPacket {
    @Nullable
    private final BlockPos pos;

    public TownTowerUpdatePacket(@Nullable BlockPos pos) {
        this.pos = pos == null ? null : pos.immutable();
    }

    public TownTowerUpdatePacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBoolean() ? buffer.readBlockPos() : null;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(pos != null);
        if (pos != null) buffer.writeBlockPos(pos);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!TownClientUpdates.canApply(context.get().getNetworkManager())) return;
            TownClientUpdates.apply(town -> town.applyTowerUpdate(pos));
        });
        context.get().setPacketHandled(true);
    }
}
