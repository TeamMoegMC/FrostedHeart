package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.cui.UILayer;

import net.minecraft.client.gui.GuiGraphics;

public class FilterLayer extends UILayer {
	RequesterChestScreen layer;
	public FilterLayer(RequesterChestScreen panel) {
		super(panel);
		layer=panel;
	}

	@Override
	public void addUIElements() {
		for(int i=0;i<layer.getMenu().list.size();i++) {
			UIFilterSlot slot=new UIFilterSlot(this,layer.getMenu(),i);
			slot.setPosAndSize(8+18*i, 3, 16, 16);
			add(slot);
			
		}
		setSize(176,31);
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.INV_FILTER.draw(graphics, x, y, w, h);
	}

	@Override
	public void alignWidgets() {

		
	}

}
