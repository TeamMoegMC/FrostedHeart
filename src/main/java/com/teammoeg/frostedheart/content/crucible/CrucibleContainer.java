/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.crucible;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class CrucibleContainer extends IEBaseContainer<CrucibleTileEntity> {
    public CrucibleTileEntity.CrucibleData data;

    public CrucibleContainer(int id, PlayerInventory inventoryPlayer, CrucibleTileEntity tile) {
        super(inventoryPlayer, tile, id);

        // input
        this.addSlot(new IESlot(this, this.inv, 0, 30, 12) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return CrucibleRecipe.isValidRecipeInput(itemStack, true);
            }
        });
        this.addSlot(new IESlot(this, this.inv, 1, 51, 12) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return CrucibleRecipe.isValidRecipeInput(itemStack, false);
            }
        });
        // output
        this.addSlot(new IESlot.Output(this, this.inv, 2, 106, 12));
        // input fuel
        this.addSlot(new IESlot(this, this.inv, 3, 80, 51) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return itemStack.getItem().getTags().contains(CrucibleTileEntity.coal_coke);
            }
        });
        slotCount = 3;

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
        data = tile.guiData;
        trackIntArray(data);
    }
}

