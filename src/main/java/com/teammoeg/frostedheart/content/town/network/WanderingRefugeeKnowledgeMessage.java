/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.town.resident.PersonKnowledgeDialogue;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugee;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-validated request to ask a refugee about their persistent experience. */
public final class WanderingRefugeeKnowledgeMessage implements CMessage {
    private final int refugeeId;

    public WanderingRefugeeKnowledgeMessage(int refugeeId) { this.refugeeId = refugeeId; }
    public WanderingRefugeeKnowledgeMessage(FriendlyByteBuf buffer) { this.refugeeId = buffer.readVarInt(); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(refugeeId); }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            Entity entity = player.level().getEntity(refugeeId);
            if (!(entity instanceof WanderingRefugee refugee) || !refugee.isAlive()
                    || player.distanceTo(refugee) > 16.0D) return;
            PersonKnowledgeDialogue.shareFirst(player, refugee.getKnowledgeOverlay(),
                    "person:" + refugee.getUUID());
        });
        context.get().setPacketHandled(true);
    }
}
