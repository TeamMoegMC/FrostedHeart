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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.item.MagnifyingGlassItem;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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
    public static final int MAGNIFIED_ITEM_TICKS = DURATION_TICKS / 2;
    public static final double MAX_DISTANCE = 8;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Session(ResourceKey<Level> dimension, BlockPos block, Block originalBlock, UUID entity, int entityId,
                           long started, ObservationActionPacket request, AbstractContainerMenu menu,
                           int selectedSlot, ItemStack item, int duration) {
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
            case BLOCK, ENTITY, HELD_ITEM, SLOT_ITEM -> start(player, packet);
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
        ItemStack item = ItemStack.EMPTY;
        if (packet.action() == ObservationActionPacket.Action.ENTITY) {
            if (entity == null || entity == player || !entity.isAlive() || !player.canReach(entity, 1)) {
                cancel(player, "interrupted");
                return;
            }
            if (entity instanceof ItemEntity dropped) item = dropped.getItem();
        } else if (packet.action() == ObservationActionPacket.Action.BLOCK) {
            if (player.level().getBlockState(block).isAir() || !player.canReach(block, 1)) {
                cancel(player, "interrupted");
                return;
            }
        } else {
            item = itemTarget(player, packet, player.containerMenu);
        }
        boolean itemTarget = packet.action() == ObservationActionPacket.Action.HELD_ITEM
                || packet.action() == ObservationActionPacket.Action.SLOT_ITEM || entity instanceof ItemEntity;
        if (!hasTool(player, packet) || (itemTarget && !ItemRetrieval.canObserve(item))) {
            cancel(player, "interrupted");
            return;
        }
        // The packet already identifies the client's outlined target. A ray to its center
        // can hit a neighbouring block even when the target's visible face is selectable.
        int duration = itemTarget && packet.method() != ObservationActionPacket.Method.KEY ? MAGNIFIED_ITEM_TICKS : DURATION_TICKS;
        Session session = new Session(player.level().dimension(), block.immutable(), packet.action() == ObservationActionPacket.Action.BLOCK ? player.level().getBlockState(block).getBlock() : null,
                entity == null ? null : entity.getUUID(), entity == null ? -1 : entity.getId(), player.level().getGameTime(),
                packet, player.containerMenu, player.getInventory().selected, item.copy(), duration);
        SESSIONS.put(player.getUUID(), session);
        FRNetwork.INSTANCE.sendPlayer(player, new ObservationStatePacket(ObservationStatePacket.State.STARTED, block, session.entityId(), new CompoundTag(), "", duration, itemTarget));
    }

    private static ItemStack itemTarget(ServerPlayer player, ObservationActionPacket request, AbstractContainerMenu menu) {
        if (request.action() == ObservationActionPacket.Action.HELD_ITEM) {
            if (request.entityId() < 0 || request.entityId() > 1) return ItemStack.EMPTY;
            return player.getItemInHand(request.entityId() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
        if (request.action() == ObservationActionPacket.Action.SLOT_ITEM && player.containerMenu == menu
                && menu.containerId == request.containerId() && menu.stillValid(player)
                && request.entityId() >= 0 && request.entityId() < menu.slots.size())
            return menu.getSlot(request.entityId()).getItem();
        return ItemStack.EMPTY;
    }

    private static boolean hasTool(ServerPlayer player, ObservationActionPacket request) {
        return switch (request.method()) {
            case KEY -> true;
            case MAIN_HAND_GLASS -> player.getMainHandItem().getItem() instanceof MagnifyingGlassItem
                    && request.action() != ObservationActionPacket.Action.SLOT_ITEM
                    && !(request.action() == ObservationActionPacket.Action.HELD_ITEM && request.entityId() == 0);
            case OFF_HAND_GLASS -> player.getOffhandItem().getItem() instanceof MagnifyingGlassItem
                    && request.action() != ObservationActionPacket.Action.SLOT_ITEM
                    && !(request.action() == ObservationActionPacket.Action.HELD_ITEM && request.entityId() == 1);
            case CURSOR_GLASS -> request.action() == ObservationActionPacket.Action.SLOT_ITEM
                    && player.containerMenu.getCarried().getItem() instanceof MagnifyingGlassItem;
            // Creative cursor stacks exist only on the client, just like vanilla creative slot editing.
            case CREATIVE_CURSOR_GLASS -> request.action() == ObservationActionPacket.Action.SLOT_ITEM && player.isCreative();
        };
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        Entity entity = session.entity() == null ? null : player.serverLevel().getEntity(session.entity());
        boolean invalid = !player.isAlive() || player.isSpectator() || player.level().dimension() != session.dimension()
                || !hasTool(player, session.request());
        Vec3 target = entity == null ? Vec3.atCenterOf(session.block()) : entity.getBoundingBox().getCenter();
        boolean portable = session.request().action() == ObservationActionPacket.Action.HELD_ITEM
                || session.request().action() == ObservationActionPacket.Action.SLOT_ITEM;
        if (session.entity() != null) invalid |= entity == null || !entity.isAlive();
        else if (!portable)
            invalid |= !player.level().hasChunkAt(session.block()) || player.level().getBlockState(session.block()).getBlock() != session.originalBlock();
        ItemStack currentItem = entity instanceof ItemEntity dropped ? dropped.getItem()
                : portable ? itemTarget(player, session.request(), session.menu()) : ItemStack.EMPTY;
        if (!session.item().isEmpty()) invalid |= !ItemStack.matches(session.item(), currentItem);
        if (session.request().action() == ObservationActionPacket.Action.HELD_ITEM
                || session.request().method() == ObservationActionPacket.Method.MAIN_HAND_GLASS)
            invalid |= player.getInventory().selected != session.selectedSlot();
        double range = Math.max(MAX_DISTANCE, session.entity() == null ? player.getBlockReach() + 2 : player.getEntityReach() + 2);
        boolean inRange = portable || (session.entity() == null ? target.distanceToSqr(player.getEyePosition()) <= range * range
                : entity != null && player.isCloseEnough(entity, range));
        if (invalid || !inRange) {
            cancel(player, "interrupted");
            return;
        }
        if (player.level().getGameTime() - session.started() < session.duration()) return;
        SESSIONS.remove(player.getUUID());
        Observation record = !currentItem.isEmpty() ? ItemRetrieval.observe(player, currentItem).observation().orElseThrow()
                : entity == null ? ObservationSampling.block(player, session.block()) : ObservationSampling.entity(player, entity);
        com.teammoeg.frostedresearch.knowledge.furnace.RefractoryObservationIndex.observed(player,record);
        KnowledgeService.forPlayer(player).observeResearchTarget(player,record,
            record.type()==Observation.Type.BLOCK ? session.block() : null,currentItem);
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
