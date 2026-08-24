/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class P2PTerminalBufferTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void externalCapabilityOnlyInsertsPendingAndExtractsReceived() {
        P2PTerminalBuffer buffer = new P2PTerminalBuffer(() -> { });
        IItemHandler external = buffer.externalView();

        assertTrue(external.insertItem(0, new ItemStack(Items.STONE, 8), false).isEmpty());
        assertEquals(8, buffer.inventory().getStackInSlot(0).getCount());
        assertEquals(8, external.insertItem(4, new ItemStack(Items.STONE, 8), false).getCount());
        assertTrue(external.extractItem(0, 8, false).isEmpty());

        assertTrue(buffer.p2pTargetView().insertItem(0,
                new ItemStack(Items.DIRT, 6), false).isEmpty());
        assertEquals(6, external.extractItem(4, 6, false).getCount());
    }

    @Test
    void locksHideAutomationButNeverDeleteExistingItems() {
        P2PTerminalBuffer buffer = new P2PTerminalBuffer(() -> { });
        buffer.inventory().setStackInSlot(0, new ItemStack(Items.STONE, 5));
        buffer.inventory().setStackInSlot(4, new ItemStack(Items.DIRT, 7));

        buffer.setLocks(true, true);

        assertTrue(buffer.p2pSourceView().extractItem(0, 5, false).isEmpty());
        assertTrue(buffer.externalView().extractItem(4, 7, false).isEmpty());
        assertEquals(5, buffer.inventory().extractItem(0, 5, false).getCount());
        assertEquals(7, buffer.inventory().extractItem(4, 7, false).getCount());
    }

    @Test
    void saveLoadKeepsBothRangesAndLockIsRuntimeDerived() {
        P2PTerminalBuffer original = new P2PTerminalBuffer(() -> { });
        original.inventory().setStackInSlot(1, new ItemStack(Items.STONE, 3));
        original.inventory().setStackInSlot(6, new ItemStack(Items.DIRT, 4));
        original.setLocks(true, false);
        CompoundTag saved = original.serializeNBT();

        P2PTerminalBuffer restored = new P2PTerminalBuffer(() -> { });
        restored.deserializeNBT(saved);

        assertEquals(3, restored.inventory().getStackInSlot(1).getCount());
        assertEquals(4, restored.inventory().getStackInSlot(6).getCount());
        assertFalse(restored.isPendingLocked());
        assertFalse(restored.isReceivedLocked());
    }

    @Test
    void filtersDefaultAllowAndSupportExactFuzzyWhitelistAndBlacklist() {
        P2PItemFilter filter = new P2PItemFilter();
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.getOrCreateTag().putString("variant", "named");
        ItemStack plainStone = new ItemStack(Items.STONE);

        assertTrue(filter.matches(plainStone));
        filter.setEntry(0, namedStone);
        assertTrue(filter.matches(namedStone));
        assertFalse(filter.matches(plainStone));
        filter.setFuzzy(true);
        assertTrue(filter.matches(plainStone));
        filter.setWhitelist(false);
        assertFalse(filter.matches(plainStone));
        assertTrue(filter.matches(new ItemStack(Items.DIRT)));

        P2PItemFilter restored = new P2PItemFilter();
        restored.deserializeNBT(filter.serializeNBT());
        assertFalse(restored.isWhitelist());
        assertTrue(restored.isFuzzy());
        assertFalse(restored.matches(plainStone));
    }
}
