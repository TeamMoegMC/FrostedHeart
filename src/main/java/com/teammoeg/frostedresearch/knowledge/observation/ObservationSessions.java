package com.teammoeg.frostedresearch.knowledge.observation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端锁定、计时与一次采样；预览本身不增加队伍知识。
 */
@Mod.EventBusSubscriber(modid = FRMain.MODID)
public final class ObservationSessions {
    public static final int DURATION_TICKS = 60;
    public static final double MAX_DISTANCE = 8;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Session(ResourceKey<Level> dimension, BlockPos block, Block originalBlock, UUID entity, int entityId,
                           long started) {
    }

    private record Pending(Observation observation, AcquisitionSource source) {
    }

    private ObservationSessions() {
    }

    public static void handle(ServerPlayer player, ObservationActionPacket packet) {
        switch (packet.action()) {
            case CANCEL -> {
                PENDING.remove(player.getUUID());
                cancel(player, "cancelled");
            }
            case DISCARD -> {
                PENDING.remove(player.getUUID());
                cancel(player, "discarded");
            }
            case ACCEPT -> accept(player);
            case BLOCK, ENTITY -> start(player, packet);
        }
    }

    private static void start(ServerPlayer player, ObservationActionPacket packet) {
        if (SESSIONS.containsKey(player.getUUID()) || PENDING.containsKey(player.getUUID())) return;
        if (!player.isAlive() || player.isSpectator()) {
            cancel(player, "interrupted");
            return;
        }
        BlockPos block = packet.position();
        Entity entity = packet.action() == ObservationActionPacket.Action.ENTITY ? player.level().getEntity(packet.entityId()) : null;
        if (packet.action() == ObservationActionPacket.Action.ENTITY) {
            if (entity == null || entity == player || !entity.isAlive() || !player.canReach(entity, 1)) {
                cancel(player, "interrupted");
                return;
            }
        } else if (player.level().getBlockState(block).isAir() || !player.canReach(block, 1)) {
            cancel(player, "interrupted");
            return;
        }
        // The packet already identifies the client's outlined target. A ray to its center
        // can hit a neighbouring block even when the target's visible face is selectable.
        Session session = new Session(player.level().dimension(), block.immutable(), entity == null ? player.level().getBlockState(block).getBlock() : null,
                entity == null ? null : entity.getUUID(), entity == null ? -1 : entity.getId(), player.level().getGameTime());
        SESSIONS.put(player.getUUID(), session);
        FRNetwork.INSTANCE.sendPlayer(player, new ObservationStatePacket(ObservationStatePacket.State.STARTED, block, session.entityId(), new CompoundTag(), ""));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        Entity entity = session.entity() == null ? null : player.serverLevel().getEntity(session.entity());
        boolean invalid = !player.isAlive() || player.level().dimension() != session.dimension();
        Vec3 target = entity == null ? Vec3.atCenterOf(session.block()) : entity.getBoundingBox().getCenter();
        if (session.entity() != null) invalid |= entity == null || !entity.isAlive();
        else
            invalid |= !player.level().hasChunkAt(session.block()) || player.level().getBlockState(session.block()).getBlock() != session.originalBlock();
        double range = Math.max(MAX_DISTANCE, session.entity() == null ? player.getBlockReach() + 2 : player.getEntityReach() + 2);
        boolean inRange = session.entity() == null ? target.distanceToSqr(player.getEyePosition()) <= range * range
                : entity != null && player.isCloseEnough(entity, range);
        if (invalid || !inRange) {
            cancel(player, "interrupted");
            return;
        }
        if (player.level().getGameTime() - session.started() < DURATION_TICKS) return;
        SESSIONS.remove(player.getUUID());
        Observation record = entity == null ? ObservationSampling.block(player, session.block()) : ObservationSampling.entity(player, entity);
        preview(player, record, AcquisitionSource.of("player_observation", player), true);
    }

    /**
     * NPC/dialogue integrations pass their existing immutable record and actual reception source here.
     */
    public static void preview(ServerPlayer player, Observation record, AcquisitionSource source) {
        preview(player, record, source, false);
    }

    private static void preview(ServerPlayer player, Observation record, AcquisitionSource source, boolean heldObservation) {
        PENDING.put(player.getUUID(), new Pending(record, source));
        CompoundTag encoded = (CompoundTag) Observation.CODEC.encodeStart(NbtOps.INSTANCE, record).result().orElseThrow();
        FRNetwork.INSTANCE.sendPlayer(player, new ObservationStatePacket(heldObservation ? ObservationStatePacket.State.OBSERVED : ObservationStatePacket.State.PREVIEW, BlockPos.ZERO, -1, encoded, ""));
    }

    private static void accept(ServerPlayer player) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) return;
        var result = KnowledgeService.forPlayer(player).receive(KnowledgeElement.observation(pending.observation()), pending.source());
        if (result.succeeded()) {
            PENDING.remove(player.getUUID());
            FRNetwork.INSTANCE.sendPlayer(player, new ObservationStatePacket(ObservationStatePacket.State.ACCEPTED, "received"));
        } else {
            FRNetwork.INSTANCE.sendPlayer(player, new ObservationStatePacket(ObservationStatePacket.State.REJECTED, result.status().name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private static void cancel(ServerPlayer player, String reason) {
        SESSIONS.remove(player.getUUID());
        FRNetwork.INSTANCE.sendPlayer(player, new ObservationStatePacket(ObservationStatePacket.State.CANCELLED, reason));
    }

    @SubscribeEvent
    public static void damaged(LivingDamageEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player && SESSIONS.containsKey(player.getUUID()))
            cancel(player, "interrupted");
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SESSIONS.clear();
        PENDING.clear();
    }
}
