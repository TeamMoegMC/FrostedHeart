/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * Only the team's visible knowledge, discovered relations, and revealed hints live here.
 */
public final class KnowledgeClientState {
    private static CompoundTag snapshot = new CompoundTag();
    private static long serial;

    public static long serial() {
        return serial;
    }

    private KnowledgeClientState() {
    }

    public static CompoundTag snapshot() {
        return snapshot;
    }

    public static void setSnapshot(CompoundTag value) {
        snapshot = value.copy();
        serial++;
        KnowledgeLayer layer = KnowledgeScreen.current();
        if (layer != null) layer.refreshSnapshot();
    }

    public static void receiveReply(CompoundTag value) {
        value = KnowledgeWorkbenchBatch.accept(value);
        if (value == null) {
            if (KnowledgeScreen.current() != null) KnowledgeScreen.current().batchProgress();
            return;
        }
        if (value.contains("snapshot")) setSnapshot(value.getCompound("snapshot"));
        if (KnowledgeScreen.current() != null) KnowledgeScreen.current().receiveReply(value);
    }
}
