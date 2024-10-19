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

import com.teammoeg.frostedheart.FHBaseContainer;
import com.teammoeg.frostedheart.FHContainer;

import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class IncubatorT1Container extends FHBaseContainer<IncubatorTileEntity> {

    public IncubatorT1Container(int id, Inventory inventoryPlayer, IncubatorTileEntity tile) {
        super(FHContainer.INCUBATOR_T1.get(), tile, id, 4);

        this.addSlot(new IESlot(this, this.inv, 0, 34, 52) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(0, itemStack);
            }
        });
        this.addSlot(new IESlot(this, this.inv, 1, 16, 17) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(1, itemStack);
            }
        });
        this.addSlot(new IESlot(this, this.inv, 2, 34, 17) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(2, itemStack);
            }
        });
        this.addSlot(new IESlot.Output(this, this.inv, 3, 143, 36));

        this.addPlayerInventory(inventoryPlayer,8,84,142);
    }
}

