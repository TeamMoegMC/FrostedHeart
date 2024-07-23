/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class IncubatorT2Container extends IEBaseContainer<HeatIncubatorTileEntity> {

    public IncubatorT2Container(int id, Inventory inventoryPlayer, HeatIncubatorTileEntity tile) {
        super(tile, id);

        /*this.addSlot(new IESlot(this, this.inv, 0, 34, 52) {
            @Override
            public boolean isItemValid(ItemStack itemStack) {
                return tile.isStackValid(0, itemStack);
            }
        });*/
        this.addSlot(new IESlot(this, this.inv, 1, 65, 44) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(1, itemStack);
            }
        });
        this.addSlot(new IESlot(this, this.inv, 2, 65, 26) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(2, itemStack);
            }
        });
        this.addSlot(new IESlot.Output(this, this.inv, 3, 147, 35));

        slotCount = 4;

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
    }
}

