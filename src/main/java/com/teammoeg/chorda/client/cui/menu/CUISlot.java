package com.teammoeg.chorda.client.cui.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class CUISlot extends Slot implements DeactivatableSlot {
	boolean enabled = true;

	public CUISlot(Container inventoryIn, int index, int xPosition, int yPosition) {
		super(inventoryIn, index, xPosition, yPosition);
	}

	public boolean isActive() {
		return enabled;
	}

	public void setActived(boolean enabled) {
		this.enabled = enabled;
	}
}