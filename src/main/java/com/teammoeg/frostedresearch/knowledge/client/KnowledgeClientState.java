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
