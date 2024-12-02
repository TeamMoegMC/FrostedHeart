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

import com.teammoeg.frostedheart.base.menu.FHBlockEntityContainer;
import com.teammoeg.frostedheart.FHMenuTypes;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class RelicChestContainer extends FHBlockEntityContainer<RelicChestTileEntity> {
    public RelicChestContainer(int id, Inventory inventoryPlayer, RelicChestTileEntity tile) {
        super(FHMenuTypes.RELIC_CHEST.get(), tile, id,inventoryPlayer.player, 15);

        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 5; ++k) {
                this.addSlot(new Slot(tile, k + j * 5, 44 + k * 18, 19 + j * 18));
            }
        }
//        super.addPlayerInventory(inventoryPlayer, 9, id, id);
        
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
    }

}
