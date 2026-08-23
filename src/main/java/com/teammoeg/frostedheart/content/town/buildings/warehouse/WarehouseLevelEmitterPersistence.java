/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.jetbrains.annotations.Nullable;

/** NBT contract for a town-owned warehouse level emitter. */
final class WarehouseLevelEmitterPersistence {
    private WarehouseLevelEmitterPersistence() {
    }

    static State read(CompoundTag nbt) {
        SimpleItemKey filter = null;
        if (nbt.contains("filter")) {
            filter = SimpleItemKey.CODEC.parse(NbtOps.INSTANCE, nbt.get("filter"))
                    .resultOrPartial(message -> FHMain.LOGGER.warn(
                            "Failed to read warehouse level emitter filter: {}", message))
                    .orElse(null);
        }
        WarehouseRedstoneMode mode = WarehouseRedstoneMode.byOrdinal(
                nbt.getInt("redstoneMode"), WarehouseRedstoneMode.HIGH_SIGNAL);
        if (mode == WarehouseRedstoneMode.IGNORE) {
            mode = WarehouseRedstoneMode.HIGH_SIGNAL;
        }
        return new State(
                filter,
                Math.max(1, nbt.getInt("threshold")),
                mode,
                nbt.getLong("lastKnownStock"),
                nbt.getBoolean("emitterOn"),
                TownWarehouseDeviceAccess.readProvider(nbt));
    }

    static void write(CompoundTag nbt, State state) {
        if (state.filter() != null) {
            SimpleItemKey.CODEC.encodeStart(NbtOps.INSTANCE, state.filter())
                    .resultOrPartial(message -> FHMain.LOGGER.warn(
                            "Failed to write warehouse level emitter filter: {}", message))
                    .ifPresent(encoded -> nbt.put("filter", encoded));
        }
        nbt.putInt("threshold", state.threshold());
        nbt.putInt("redstoneMode", state.mode().ordinal());
        nbt.putLong("lastKnownStock", state.lastKnownStock());
        nbt.putBoolean("emitterOn", state.emitterOn());
        TownWarehouseDeviceAccess.writeProvider(nbt, state.townProvider());
    }

    record State(
            @Nullable SimpleItemKey filter,
            int threshold,
            WarehouseRedstoneMode mode,
            long lastKnownStock,
            boolean emitterOn,
            @Nullable TeamTownProvider townProvider
    ) {
    }
}
