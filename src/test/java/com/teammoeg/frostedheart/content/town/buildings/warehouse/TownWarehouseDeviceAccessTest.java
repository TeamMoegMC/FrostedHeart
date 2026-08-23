/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownWarehouseDeviceAccessTest {
    @BeforeAll
    static void registerTownProviderCodec() {
        ITownProviderSerializable.registerAll();
    }

    @Test
    void ownerAndGeneratorDimensionMustBothMatch() {
        UUID owner = UUID.randomUUID();

        assertTrue(TownWarehouseDeviceAccess.ownershipAndDimensionMatch(
                owner, owner, Level.OVERWORLD, Level.OVERWORLD));
        assertFalse(TownWarehouseDeviceAccess.ownershipAndDimensionMatch(
                owner, UUID.randomUUID(), Level.OVERWORLD, Level.OVERWORLD));
        assertFalse(TownWarehouseDeviceAccess.ownershipAndDimensionMatch(
                owner, owner, Level.OVERWORLD, Level.NETHER));
        assertFalse(TownWarehouseDeviceAccess.ownershipAndDimensionMatch(
                owner, owner, null, Level.OVERWORLD));
    }

    @Test
    void menuGuardRejectsOldMenusAndReplacedOrDistantBlockEntities() {
        UUID owner = UUID.randomUUID();

        assertTrue(TownWarehouseDeviceAccess.menuAccessFactsMatch(
                true, true, owner, owner, Level.OVERWORLD, Level.OVERWORLD));
        assertFalse(TownWarehouseDeviceAccess.menuAccessFactsMatch(
                false, true, owner, owner, Level.OVERWORLD, Level.OVERWORLD));
        assertFalse(TownWarehouseDeviceAccess.menuAccessFactsMatch(
                true, false, owner, owner, Level.OVERWORLD, Level.OVERWORLD));
        assertFalse(TownWarehouseDeviceAccess.menuAccessFactsMatch(
                true, true, owner, UUID.randomUUID(), Level.OVERWORLD, Level.OVERWORLD));
        assertFalse(TownWarehouseDeviceAccess.menuAccessFactsMatch(
                true, true, owner, owner, Level.OVERWORLD, Level.NETHER));
    }

    @Test
    void providerNbtContainsTownOwnerButNeverWarehouseCore() {
        UUID owner = UUID.randomUUID();
        CompoundTag nbt = new CompoundTag();

        TownWarehouseDeviceAccess.writeProvider(nbt, new TeamTownProvider(owner));

        assertTrue(nbt.contains("townProvider"));
        assertFalse(nbt.contains("warehousePos"));
        assertEquals(owner, TownWarehouseDeviceAccess.readProvider(nbt).ownerUUID);
        assertNull(TownWarehouseDeviceAccess.readProvider(new CompoundTag()));
    }

    @Test
    void emitterThresholdModesAreComplementaryAtTheBoundary() {
        assertFalse(WarehouseLevelEmitterBlockEntity.shouldEmit(
                WarehouseRedstoneMode.HIGH_SIGNAL, 9, 10));
        assertTrue(WarehouseLevelEmitterBlockEntity.shouldEmit(
                WarehouseRedstoneMode.HIGH_SIGNAL, 10, 10));
        assertTrue(WarehouseLevelEmitterBlockEntity.shouldEmit(
                WarehouseRedstoneMode.LOW_SIGNAL, 9, 10));
        assertFalse(WarehouseLevelEmitterBlockEntity.shouldEmit(
                WarehouseRedstoneMode.LOW_SIGNAL, 10, 10));
        assertFalse(WarehouseLevelEmitterBlockEntity.shouldEmit(
                WarehouseRedstoneMode.IGNORE, Long.MAX_VALUE, 1));
    }
}
