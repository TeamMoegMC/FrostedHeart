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

import com.teammoeg.frostedheart.FHBaseContainer;
import com.teammoeg.frostedheart.FHContainer;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;

import blusunrize.immersiveengineering.common.gui.IESlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DrawDeskContainer extends FHBaseContainer<DrawingDeskTileEntity> {
    interface Enabled {
        void setEnabled(boolean enabled);
    }

    public static class EnableIESlot extends IESlot implements Enabled {
        boolean enabled = true;

        public EnableIESlot(AbstractContainerMenu containerMenu, Container inv, int id, int x, int y) {
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

        public EnableSlot(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        public boolean isActive() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public DrawDeskContainer(int id, Inventory inventoryPlayer, DrawingDeskTileEntity tile) {
        super(FHContainer.DRAW_DESK.get(), tile, id, 4);

        this.addSlot(new EnableIESlot(this, tile, DrawingDeskTileEntity.PAPER_SLOT, 114, 161) {// paper
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(DrawingDeskTileEntity.PAPER_SLOT, itemStack);
            }
        });
        this.addSlot(new EnableIESlot(this, tile, DrawingDeskTileEntity.INK_SLOT, 114, 178) {// pen
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(DrawingDeskTileEntity.INK_SLOT, itemStack);
            }
        });
        this.addSlot(new EnableIESlot(this, tile, DrawingDeskTileEntity.EXAMINE_SLOT, 114, 93) {// research
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return tile.isStackValid(DrawingDeskTileEntity.EXAMINE_SLOT, itemStack);
            }

        });

        //this.inventorySlots.get(0).set

    }

    public void setEnabled(boolean en) {
        for (Slot s : this.slots) {
            if (s instanceof Enabled)
                ((Enabled) s).setEnabled(en);
        }
    }

	@Override
	public boolean quickMoveIn(ItemStack slotStack) {
		return true;
	}
}
