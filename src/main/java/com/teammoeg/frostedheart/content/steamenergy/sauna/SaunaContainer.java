/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class SaunaContainer extends IEBaseContainer<SaunaTileEntity> {
    public SaunaContainer(int id, PlayerInventory inventoryPlayer, SaunaTileEntity tile) {
        super(tile, id);
        // medicine slot
        addSlot(new Slot(this.inv, 0, 98, 26) {
            @Override
            public int getMaxStackSize() {
                return 4;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return tile.isStackValid(0, stack);
            }
        });

        // player inventory
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        // hotbar
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));

        this.slotCount = 1;
        this.tile = tile;
    }
}
