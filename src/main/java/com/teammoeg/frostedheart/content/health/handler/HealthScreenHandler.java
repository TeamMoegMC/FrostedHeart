/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.health.handler;

import com.teammoeg.chorda.menu.DummyMenuProvider;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.screen.HealthStatMenu;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

/** Opens health data on the current player; requests made while dead follow the respawn. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HealthScreenHandler {
    // Packet handling and these lifecycle events all run on the server thread.
    private static final Set<UUID> WAITING_FOR_RESPAWN = new HashSet<>();

    private HealthScreenHandler() {
    }

    public static void open(ServerPlayer requested) {
        if (requested == null) return;
        ServerPlayer player = requested.server.getPlayerList().getPlayer(requested.getUUID());
        if (player == null) return;
        if (!player.isAlive() || player.isRemoved()) {
            WAITING_FOR_RESPAWN.add(player.getUUID());
            return;
        }
        WAITING_FOR_RESPAWN.remove(player.getUUID());
        NetworkHooks.openScreen(player, new DummyMenuProvider(
                (id, inventory, owner) -> new HealthStatMenu(id, inventory)));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && WAITING_FOR_RESPAWN.remove(player.getUUID())) {
            open(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WAITING_FOR_RESPAWN.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WAITING_FOR_RESPAWN.clear();
    }
}
