/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.network;

import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalStatusProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Fieldless C2S request: the server always resolves the sender's own team. */
public class TownOperationalStatusRequestPacket implements CMessage {
    private static final Map<UUID, Long> LAST_RESPONSE_GAME_TIME = new ConcurrentHashMap<>();

    public TownOperationalStatusRequestPacket() {
    }

    public TownOperationalStatusRequestPacket(FriendlyByteBuf ignored) {
    }

    @Override
    public void encode(FriendlyByteBuf ignored) {
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;
            long now = player.serverLevel().getGameTime();
            if (!shouldRespond(player.getUUID(), now)) return;
            FHNetwork.INSTANCE.sendPlayer(player,
                    new TownOperationalStatusResponsePacket(TownOperationalStatusProvider.capture(player)));
        });
        context.get().setPacketHandled(true);
    }

    static boolean shouldRespond(UUID playerId, long gameTime) {
        Long last = LAST_RESPONSE_GAME_TIME.get(playerId);
        if (last != null && gameTime >= last && gameTime - last < 20L) return false;
        LAST_RESPONSE_GAME_TIME.put(playerId, gameTime);
        return true;
    }

    static void clearRateLimitsForTests() {
        LAST_RESPONSE_GAME_TIME.clear();
    }
}
