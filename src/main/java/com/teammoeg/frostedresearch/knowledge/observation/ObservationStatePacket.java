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
import com.teammoeg.frostedresearch.knowledge.client.observation.ObservationClient;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ObservationStatePacket(State state, BlockPos position, int entityId, CompoundTag record,
                                     String message) implements CMessage {
    public enum State {STARTED, CANCELLED, PREVIEW, ACCEPTED, REJECTED, OBSERVED}

    public ObservationStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(State.class), buffer.readBlockPos(), buffer.readVarInt(), buffer.readNbt(), buffer.readUtf());
    }

    public ObservationStatePacket(State state, String message) {
        this(state, BlockPos.ZERO, -1, new CompoundTag(), message);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(state);
        buffer.writeBlockPos(position);
        buffer.writeVarInt(entityId);
        buffer.writeNbt(record);
        buffer.writeUtf(message);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ObservationClient.receive(this)));
        context.setPacketHandled(true);
    }
}
