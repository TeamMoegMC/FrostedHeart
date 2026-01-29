package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import net.minecraft.client.gui.GuiGraphics;

public class StorageChestScreen extends LogisticChestLayer<StorageChestMenu> {


	public StorageChestScreen(StorageChestMenu container) {
		super(container);
	}
	@Override
	public boolean onInit() {
		this.setSize(176, 169);
		return super.onInit();
	}
	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.INV_CHEST.draw(graphics, x+0, y+25, 176, 59);
		LogisticIcons.INV_BACK.draw(graphics, x+0, y+84, 176, 84);
		super.drawBackground(graphics, x, y, w, h);
	}
}
