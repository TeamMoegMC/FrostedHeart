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

package com.teammoeg.frostedresearch.knowledge.observation;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ObservationActionPacket(Action action, BlockPos position, int entityId) implements CMessage {
    public enum Action {BLOCK, ENTITY, CANCEL, ACCEPT, DISCARD}

    public ObservationActionPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(Action.class), buffer.readBlockPos(), buffer.readVarInt());
    }

    public ObservationActionPacket(Action action) {
        this(action, BlockPos.ZERO, -1);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeBlockPos(position);
        buffer.writeVarInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) ObservationSessions.handle(player, this);
        });
        context.setPacketHandled(true);
    }
}
