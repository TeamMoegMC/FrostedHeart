package com.teammoeg.chorda.client.cui;

import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.client.ClientUtils;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public class MenuPrimaryLayer<T extends AbstractContainerMenu> extends PrimaryLayer {
	protected T container;
	public MenuPrimaryLayer(T container) {
		this.container=container;
	}
	@Nullable
	public Slot getSlotUnderMouse() {
		Screen screen=this.getScreen().getScreen();
		if(screen instanceof AbstractContainerScreen acs)
			return acs.getSlotUnderMouse();
		return null;
	}
	@Override
	public void getTooltip(TooltipBuilder list) {
		@Nullable
		Slot slotUnderMouse = getSlotUnderMouse();
		if (this.container.getCarried().isEmpty() && slotUnderMouse != null && slotUnderMouse.hasItem()) {
			AbstractContainerScreen.getTooltipFromItem(ClientUtils.getMc(), slotUnderMouse.getItem()).forEach(list::accept);
		}
		super.getTooltip(list);
	}

}
