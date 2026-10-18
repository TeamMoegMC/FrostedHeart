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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;

class WarehouseLevelEmitterPersistenceTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        CUtils.startTestEnvironment();
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
