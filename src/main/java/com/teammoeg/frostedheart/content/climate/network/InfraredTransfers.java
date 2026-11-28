/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One queued reply per observer, with at most one infrared part in its TCP connection. */
@Mod.EventBusSubscriber(modid=FHMain.MODID)
public final class InfraredTransfers {
    private static final Map<UUID,Transfer> PENDING=new HashMap<>();
    private InfraredTransfers() { }

    static final class Transfer {
        final int requestId;
        final ResourceKey<Level> dimension;
        final InfraredSnapshot snapshot;
        int cursor;

        Transfer(int requestId,ResourceKey<Level> dimension,InfraredSnapshot snapshot) {
            this.requestId=requestId;this.dimension=dimension;this.snapshot=snapshot;
        }

        FHResponseInfraredViewDataSyncPacket take(int requestId,int part) {
            if(this.requestId!=requestId||part!=cursor||complete())return null;
            return new FHResponseInfraredViewDataSyncPacket(requestId,snapshot,cursor++);
        }

        boolean complete() { return cursor>=Math.max(1,snapshot.brickRecords().length); }
    }

    public static void start(ServerPlayer player,int requestId,InfraredSnapshot snapshot) {
        PENDING.remove(player.getUUID());
        if(snapshot==null) {
            FHNetwork.INSTANCE.sendPlayer(player,FHResponseInfraredViewDataSyncPacket.acknowledge(requestId));
            return;
        }
        var transfer=new Transfer(requestId,player.serverLevel().dimension(),snapshot);
        var first=transfer.take(requestId,0);
        PENDING.put(player.getUUID(),transfer);
        FHNetwork.INSTANCE.sendPlayer(player,first);
    }

    public static void continueTransfer(ServerPlayer player,int requestId,int part) {
        var transfer=PENDING.get(player.getUUID());
        if(part<0) {
            if(transfer!=null&&transfer.requestId==requestId)PENDING.remove(player.getUUID());
            return;
        }
        if(transfer!=null&&transfer.requestId==requestId&&transfer.complete()&&part==transfer.cursor) {
            MinecraftThermalInput.keepInfraredTransferActive(player.serverLevel());
            PENDING.remove(player.getUUID());
            return;
        }
        var reply=transfer!=null&&transfer.dimension.equals(player.serverLevel().dimension())
                ?transfer.take(requestId,part):null;
        if(reply==null) {
            // A cleared or superseded stream releases the client to request a fresh full.
            if(transfer!=null&&transfer.requestId==requestId)PENDING.remove(player.getUUID());
            FHNetwork.INSTANCE.sendPlayer(player,FHResponseInfraredViewDataSyncPacket.acknowledge(requestId));
            return;
        }
        FHNetwork.INSTANCE.sendPlayer(player,reply);
    }

    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if(event.phase==TickEvent.Phase.END&&event.level instanceof net.minecraft.server.level.ServerLevel level
                &&Math.floorMod(level.getGameTime(),20)==0
                &&PENDING.values().stream().anyMatch(t->t.dimension.equals(level.dimension())))
            MinecraftThermalInput.keepInfraredTransferActive(level);
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { PENDING.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { PENDING.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            PENDING.values().removeIf(t->t.dimension.equals(level.dimension()));
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { PENDING.clear(); }
}
