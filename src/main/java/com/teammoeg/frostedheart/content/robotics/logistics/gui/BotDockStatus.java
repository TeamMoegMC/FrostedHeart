package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.cui.TextBoxNoBackground;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.editor.Verifier;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.gui.GuiGraphics;

public class BotDockStatus extends UILayer {
	TextBoxNoBackground textbox;
	LogisticChestLayer<? extends LogisticChestMenu<?>> layer;
	public BotDockStatus(LogisticChestLayer<? extends LogisticChestMenu<?>> panel) {
		super(panel);
		textbox=new TextBoxNoBackground(this);
		textbox.setFilter(b->{
			try {
			Integer.parseInt(b);
			}catch(Exception ex) {
				return Verifier.error(Components.literal("Number only"));
			}
			return Verifier.SUCCESS;
			
		});
		layer=panel;
	}

	@Override
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		LogisticIcons.INV_STATUS.draw(graphics, x, y, w, h);
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
		super.render(graphics, x, y, w, h);
		//upload status
		LogisticIcons.STATUS_LIGHTS[layer.getMenu().uplinkStatus.getValue()].draw(graphics, x+58, y+8, 10, 10);
		//network status
		LogisticIcons.STATUS_LIGHTS[layer.getMenu().networkStatus.getValue()].draw(graphics, x+75, y+8, 10, 10);
		
	}

	@Override
	public void addUIElements() {
		setSize(176,25);
		textbox.setPosAndSize(116, 8, 48, 8);
		this.add(textbox);
	}

	@Override
	public void alignWidgets() {
		
	}

}
