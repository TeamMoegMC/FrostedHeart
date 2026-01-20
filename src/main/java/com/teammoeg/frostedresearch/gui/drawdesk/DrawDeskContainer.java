/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.menu.CBlockEntityMenu;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class DrawDeskContainer extends CBlockEntityMenu<DrawingDeskTileEntity> {
	public DrawDeskContainer(int id, Inventory inventoryPlayer, DrawingDeskTileEntity tile) {
		super(FRContents.MenuTypes.DRAW_DESK.get(), tile, id, inventoryPlayer.player, 3);
		this.addSlot(new EnableSlotItemHandler(tile.getInventory(), DrawingDeskTileEntity.PAPER_SLOT, 114, 161));
		this.addSlot(new EnableSlotItemHandler(tile.getInventory(), DrawingDeskTileEntity.INK_SLOT, 114, 178));
		this.addSlot(new EnableSlotItemHandler(tile.getInventory(), DrawingDeskTileEntity.EXAMINE_SLOT, 114, 93));
		for (int i = 0; i < 36; i++) {
			int posi = i;
			if (i < 9)
				posi += 27;
			else
				posi -= 9;
			addSlot(new EnableSlot(inventoryPlayer, i, 10 + (posi % 6) * 17, 93 + (posi / 6) * 17));
		}
		// this.inventorySlots.get(0).set

	}

	public void setEnabled(boolean en) {
		for (Slot s : this.slots) {
			if (s instanceof Enabled)
				((Enabled) s).setEnabled(en);
		}
	}

	interface Enabled {
		void setEnabled(boolean enabled);
	}

	public static class EnableSlotItemHandler extends SlotItemHandler implements Enabled {
		boolean enabled = true;

		public EnableSlotItemHandler(ItemStackHandler inv, int id, int x, int y) {
			super(inv, id, x, y);
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

	/*
	 * @Override public boolean quickMoveIn(ItemStack slotStack) {
	 * 
	 * return this.moveItemStackTo(slotStack, 0,1,
	 * false)||this.moveItemStackTo(slotStack, 1, 2,
	 * false)||this.moveItemStackTo(slotStack, 2, 3, false); }
	 */
}
