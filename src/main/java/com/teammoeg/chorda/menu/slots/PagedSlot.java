package com.teammoeg.chorda.menu.slots;

import java.util.function.Supplier;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class PagedSlot extends Slot {
	Supplier<Integer> pager;
	int page;

	public PagedSlot(Container pContainer, int pSlot, int pX, int pY, Supplier<Integer> pager, int page) {
		super(pContainer, pSlot, pX, pY);
		this.pager = pager;
		this.page = page;
	}

	@Override
	public boolean isActive() {
		return pager.get()==page;
	}

}
