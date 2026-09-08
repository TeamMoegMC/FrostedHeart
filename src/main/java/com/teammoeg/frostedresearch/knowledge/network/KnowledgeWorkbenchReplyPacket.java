/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.network;

import java.util.function.Supplier;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedresearch.knowledge.client.KnowledgeClientState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Result of one desk action; candidate replies contain only fully matched outputs.
 */
public final class KnowledgeWorkbenchReplyPacket implements CMessage {
    private final CompoundTag payload;

    public KnowledgeWorkbenchReplyPacket(CompoundTag payload) {
        this.payload = payload;
    }

    public KnowledgeWorkbenchReplyPacket(FriendlyByteBuf buffer) {
        CompoundTag read = buffer.readNbt();
        payload = read == null ? new CompoundTag() : read;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(payload);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> KnowledgeClientState.receiveReply(payload)));
        context.get().setPacketHandled(true);
    }
}
