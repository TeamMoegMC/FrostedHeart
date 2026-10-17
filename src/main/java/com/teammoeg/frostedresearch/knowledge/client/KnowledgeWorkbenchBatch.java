/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import java.util.List;
import java.util.UUID;

import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeWorkbenchActionPacket.Action;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Captures one explicit selection and processes small requests, independent of screen lifetime.
 */
@Mod.EventBusSubscriber(modid = FRMain.MODID, value = Dist.CLIENT)
public final class KnowledgeWorkbenchBatch {
    private static final int KEYS_PER_REQUEST = 128;
    private static KnowledgeWorkbenchBatch current;
    private final BlockPos desk;
    private final Action action;
    private final List<KnowledgeKey> keys;
    private final ListTag results = new ListTag();
    private UUID pending;
    private int sent;

    private KnowledgeWorkbenchBatch(BlockPos desk, Action action, List<KnowledgeKey> keys) {
        this.desk = desk;
        this.action = action;
        this.keys = List.copyOf(keys);
    }

    public static void start(BlockPos desk, Action action, List<KnowledgeKey> keys) {
        if (current != null || keys.isEmpty()) return;
        current = new KnowledgeWorkbenchBatch(desk, action, keys);
        current.sendNext();
    }

    public static boolean running() {
        return current != null;
    }

    public static int completed() {
        return current == null ? 0 : current.results.size();
    }

    public static int total() {
        return current == null ? 0 : current.keys.size();
    }

    /**
     * Null means a chunk completed while further captured entries are still pending.
     */
    public static CompoundTag accept(CompoundTag reply) {
        if (current == null || !reply.hasUUID("request") || !reply.getUUID("request").equals(current.pending))
            return reply;
        current.results.addAll(reply.getList("results", net.minecraft.nbt.Tag.TAG_COMPOUND));
        if (reply.contains("message")) {
            // An unavailable/closed desk leaves all unprocessed entries unchanged.
            for (int index = current.results.size(); index < current.keys.size(); index++) {
                CompoundTag skipped = new CompoundTag();
                skipped.putString("label", current.keys.get(index).id());
                skipped.putString("status", "CANCELLED");
                current.results.add(skipped);
            }
        } else if (current.sent < current.keys.size()) {
            current.sendNext();
            return null;
        }
        CompoundTag complete = reply.copy();
        complete.put("results", current.results);
        current = null;
        return complete;
    }

    private void sendNext() {
        int next = Math.min(sent + KEYS_PER_REQUEST, keys.size());
        var packet = new KnowledgeWorkbenchActionPacket(desk, action, keys.subList(sent, next), "", "", "");
        sent = next;
        pending = packet.requestId();
        FRNetwork.INSTANCE.sendToServer(packet);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        current = null;
    }
}
