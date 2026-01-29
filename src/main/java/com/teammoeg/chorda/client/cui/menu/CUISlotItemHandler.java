package com.teammoeg.chorda.client.cui.menu;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CUISlotItemHandler extends SlotItemHandler implements DeactivatableSlot {
	boolean enabled = true;

	public CUISlotItemHandler(IItemHandler inv, int id, int x, int y) {
		super(inv, id, x, y);
	}

	public boolean isActive() {
		return enabled;
	}

	public void setActived(boolean enabled) {
		this.enabled = enabled;
	}
}