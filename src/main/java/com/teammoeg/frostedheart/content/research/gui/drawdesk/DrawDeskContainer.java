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

package com.teammoeg.frostedheart.content.research.gui.drawdesk;

import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;

import blusunrize.immersiveengineering.common.gui.IEBaseContainer;
import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class DrawDeskContainer extends IEBaseContainer<DrawingDeskTileEntity> {
    interface Enabled {
        void setEnabled(boolean enabled);
    }

    public static class EnableIESlot extends IESlot implements Enabled {
        boolean enabled = true;

        public EnableIESlot(Container containerMenu, IInventory inv, int id, int x, int y) {
            super(containerMenu, inv, id, x, y);
        }

        public boolean isActive() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class EnableSlot extends Slot implements Enabled {
        boolean enabled = true;

        public EnableSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        public boolean isActive() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public DrawDeskContainer(int id, PlayerInventory inventoryPlayer, DrawingDeskTileEntity tile) {
        super(tile, id);

        this.addSlot(new EnableIESlot(this, this.inv, DrawingDeskTileEntity.PAPER_SLOT, 114, 161) {// paper
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(DrawingDeskTileEntity.PAPER_SLOT, itemStack);
            }
        });
        this.addSlot(new EnableIESlot(this, this.inv, DrawingDeskTileEntity.INK_SLOT, 114, 178) {// pen
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(DrawingDeskTileEntity.INK_SLOT, itemStack);
            }
        });
        this.addSlot(new EnableIESlot(this, this.inv, DrawingDeskTileEntity.EXAMINE_SLOT, 114, 93) {// research
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(DrawingDeskTileEntity.EXAMINE_SLOT, itemStack);
            }

        });

        slotCount = 4;

        for (int i = 0; i < 36; i++) {
            int posi = i;
            if (i < 9)
                posi += 27;
            else
                posi -= 9;
            addSlot(new EnableSlot(inventoryPlayer, i, 10 + (posi % 6) * 17, 93 + (posi / 6) * 17));
        }
        //this.inventorySlots.get(0).set

    }

    public void setEnabled(boolean en) {
        for (Slot s : this.slots) {
            if (s instanceof Enabled)
                ((Enabled) s).setEnabled(en);
        }
    }
}
