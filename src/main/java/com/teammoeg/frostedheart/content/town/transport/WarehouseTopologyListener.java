/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport;

/** Runtime-only notification for a loaded town-owned warehouse automation device. */
@FunctionalInterface
public interface WarehouseTopologyListener {
    void onWarehouseTopologyChanged(WarehouseTopologySnapshot snapshot);
}
