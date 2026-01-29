package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import net.minecraft.client.gui.GuiGraphics;

public class RequesterChestScreen extends LogisticChestLayer<RequesterChestMenu>{
	FilterLayer filters;
	public RequesterChestScreen(RequesterChestMenu menu) {
		super(menu);
		filters=new FilterLayer(this);
	}

	@Override
	public boolean onInit() {
		this.setSize(176, 200);
		return super.onInit();
	}

	@Override
	public void addUIElements() {
		super.addUIElements();
		this.add(filters);
		filters.setPos(0, 84);
		
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.INV_CHEST.draw(graphics, x+0, y+25, 176, 59);
		LogisticIcons.INV_BACK.draw(graphics, x+0, y+115, 176, 84);
		super.drawBackground(graphics, x, y, w, h);
	}

}
