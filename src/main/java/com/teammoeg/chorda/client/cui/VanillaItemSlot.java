package com.teammoeg.chorda.client.cui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class VanillaItemSlot extends ItemSlot {
	
	public VanillaItemSlot(UIElement parent) {
		super(parent);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		VanillaTheme.drawSlot(graphics, x, y, w, h);
		//super.renderBackground(graphics, x, y, w, h);
	}

	public VanillaItemSlot(UIElement parent, ItemStack item) {
		super(parent, item);
	}

	public VanillaItemSlot(UIElement parent, Ingredient item) {
		super(parent, item);
	}

	public VanillaItemSlot(UIElement parent, ItemStack[] item) {
		super(parent, item);
	}

}
