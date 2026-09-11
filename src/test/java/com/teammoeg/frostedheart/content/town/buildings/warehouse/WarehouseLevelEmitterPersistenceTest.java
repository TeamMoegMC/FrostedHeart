/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseLevelEmitterPersistenceTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ITownProviderSerializable.registerAll();
    }

    @Test
    void nbtRoundTripKeepsTownOwnershipAndConfigurationWithoutWarehouseCore() {
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("variant", "test");
        UUID owner = UUID.randomUUID();
        WarehouseLevelEmitterPersistence.State source =
                new WarehouseLevelEmitterPersistence.State(
                        new SimpleItemKey(Items.COBBLESTONE, itemTag),
                        640,
                        WarehouseRedstoneMode.LOW_SIGNAL,
                        (long) Integer.MAX_VALUE + 50L,
                        true,
                        new TeamTownProvider(owner));
        CompoundTag encoded = new CompoundTag();

        WarehouseLevelEmitterPersistence.write(encoded, source);
        WarehouseLevelEmitterPersistence.State decoded =
                WarehouseLevelEmitterPersistence.read(encoded);

        assertEquals(source.filter(), decoded.filter());
        assertEquals(640, decoded.threshold());
        assertEquals(WarehouseRedstoneMode.LOW_SIGNAL, decoded.mode());
        assertEquals(source.lastKnownStock(), decoded.lastKnownStock());
        assertTrue(decoded.emitterOn());
        assertEquals(owner, decoded.townProvider().ownerUUID);
        assertFalse(encoded.contains("warehousePos"));
    }

    @Test
    void invalidStoredThresholdAndIgnoreModeNormalizeToEmitterDefaults() {
        CompoundTag encoded = new CompoundTag();
        encoded.putInt("threshold", -5);
        encoded.putInt("redstoneMode", WarehouseRedstoneMode.IGNORE.ordinal());

        WarehouseLevelEmitterPersistence.State decoded =
                WarehouseLevelEmitterPersistence.read(encoded);

        assertEquals(1, decoded.threshold());
        assertEquals(WarehouseRedstoneMode.HIGH_SIGNAL, decoded.mode());
    }
}
