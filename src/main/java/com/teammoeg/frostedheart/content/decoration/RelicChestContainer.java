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

package com.teammoeg.frostedheart.content.decoration;

import com.teammoeg.frostedheart.FHBaseContainer;
import com.teammoeg.frostedheart.FHContainer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RelicChestContainer extends FHBaseContainer<RelicChestTileEntity> {
    public RelicChestContainer(int id, Inventory inventoryPlayer, RelicChestTileEntity tile) {
        super(FHContainer.RELIC_CHEST.get(), tile, id, 15);

        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 5; ++k) {
                this.addSlot(new Slot(tile, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }
    }

	@Override
	public boolean quickMoveIn(ItemStack slotStack) {
		return true;
	}
}
