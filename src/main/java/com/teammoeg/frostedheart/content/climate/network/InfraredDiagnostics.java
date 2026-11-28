/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.network;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput.InfraredSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in observations of the existing pipeline; does not change its wire format or baseline. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InfraredDiagnostics {
    private static final Set<UUID> TRACING = new HashSet<>();
    private InfraredDiagnostics() { }

    public record Summary(String state, boolean readable, int materialPages, int fieldPages, int parts,
            int records, long validSamples, long payloadBytes, double minC, double maxC) { }

    public static Summary summarize(InfraredSnapshot snapshot) {
        if (snapshot == null) return new Summary("NO_SNAPSHOT", false, 0, 0, 0, 0, 0, 0, Double.NaN, Double.NaN);
        var decoder = new InfraredBrickCodec.Decoder();
        short[] values = new short[64];
        int records = 0;
        long valid = 0, bytes = 0;
        short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
        for (byte[] part : snapshot.brickRecords()) {
            bytes += part.length;
            var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(part));
            try {
                while (decoder.readRecord(buffer, values) >= 0) {
                    records++;
                    for (short value : values) if (value != InfraredBrickCodec.INVALID_TEMPERATURE) {
                        valid++; min = (short)Math.min(min,value); max = (short)Math.max(max,value);
                    }
                }
            } finally { buffer.release(); }
        }
        return new Summary(valid == 0 ? "NO_VALID_SAMPLES" : "TEMPERATURE_DATA", snapshot.readable(),
                count(snapshot.presence()), count(snapshot.fieldPages()), Math.max(1,snapshot.brickRecords().length),
                records, valid, bytes, valid == 0 ? Double.NaN : min*.25, valid == 0 ? Double.NaN : max*.25);
    }

    private static int count(long[] words) {
        int result=0; for(long word:words)result+=Long.bitCount(word); return result;
    }

    public static void setTracing(ServerPlayer player, boolean enabled) {
        if(enabled)TRACING.add(player.getUUID()); else TRACING.remove(player.getUUID());
    }

    public static void request(ServerPlayer player, FHRequestInfraredViewDataSyncPacket request) {
        if(!TRACING.contains(player.getUUID()))return;
        FHMain.LOGGER.info("IR_SERVER_REQUEST player={} id={} full={} baseline={} dimension={} position={}",
                player.getScoreboardName(),request.requestId(),request.forceFull(),request.knownDisplayRequestId(),
                player.serverLevel().dimension().location(),player.blockPosition());
    }

    public static void response(ServerPlayer player, int requestId, InfraredSnapshot snapshot) {
        if(!TRACING.contains(player.getUUID()))return;
        FHMain.LOGGER.info("IR_SERVER_RESPONSE player={} id={} {}",player.getScoreboardName(),requestId,summarize(snapshot));
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { TRACING.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { TRACING.clear(); }
}
