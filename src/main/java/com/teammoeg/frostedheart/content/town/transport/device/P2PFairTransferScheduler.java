/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.frostedheart.content.town.transport.P2PDirectedBinding;
import net.minecraft.core.GlobalPos;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, weight-free round robin for a receiver with multiple sources. */
public final class P2PFairTransferScheduler {
    private P2PFairTransferScheduler() {
    }

    public static boolean isSenderTurn(
            List<P2PDirectedBinding> incoming,
            GlobalPos sender,
            long gameTime
    ) {
        if (incoming == null || incoming.isEmpty() || sender == null) {
            return false;
        }
        List<P2PDirectedBinding> ordered = new ArrayList<>(incoming);
        ordered.sort(P2PDirectedBinding.STABLE_COMPARATOR);
        int turn = Math.floorMod(gameTime, ordered.size());
        return ordered.get(turn).sender().pos().equals(sender);
    }
}
