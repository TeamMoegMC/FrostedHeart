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

package com.teammoeg.frostedresearch.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradePrototypeItemTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void fabricationFreezesProfileRevisionOwnerAndUniqueSerial() {
        ResourceLocation profile = new ResourceLocation("test", "efficiency");
        UUID owner = UUID.randomUUID();
        UpgradePrototypeItem.Identity firstId = UpgradePrototypeItem.newIdentity(profile, 4, owner);
        UpgradePrototypeItem.Identity secondId = UpgradePrototypeItem.newIdentity(profile, 4, owner);
        ItemStack shell = new ItemStack(Items.PAPER);
        UpgradePrototypeItem.writeIdentity(shell, firstId);
        assertEquals(firstId, UpgradePrototypeItem.readIdentityTag(shell).orElseThrow());
        assertEquals(profile, firstId.profile());
        assertEquals(4, firstId.profileRevision());
        assertEquals(owner, firstId.ownerTeam());
        assertNotEquals(firstId.serial(), secondId.serial());
    }

    @Test
    void uninitializedShellRemainsAnOrdinaryInvalidItem() {
        ItemStack shell = new ItemStack(Items.PAPER);
        assertTrue(UpgradePrototypeItem.readIdentityTag(shell).isEmpty());
        assertTrue(UpgradePrototypeItem.identity(shell).isEmpty());
        assertEquals(Items.PAPER, shell.getItem());
        assertEquals(1, shell.getCount());
    }
}
