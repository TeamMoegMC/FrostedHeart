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

import com.teammoeg.chorda.menu.CBlockEntityContainer;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SaunaContainer extends CBlockEntityContainer<SaunaTileEntity> {

    public SaunaContainer(int id, Inventory inventoryPlayer, SaunaTileEntity tile) {
        super(FHMenuTypes.SAUNA.get(), tile, id, inventoryPlayer.player, 1);

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
    }

    @Override
    public boolean quickMoveIn(ItemStack slotStack) {
        return this.moveItemStackTo(slotStack, 0, 1, false);
    }
}
