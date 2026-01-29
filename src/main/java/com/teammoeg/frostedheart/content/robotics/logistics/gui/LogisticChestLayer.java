package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.cui.MenuPrimaryLayer;

public class LogisticChestLayer<T extends LogisticChestMenu<?>> extends MenuPrimaryLayer<T> {
	BotDockStatus statusLayer;
	public LogisticChestLayer(T container) {
		super(container);

		statusLayer=new BotDockStatus(this);
	}
	@Override
	public void alignWidgets() {
	}
	@Override
	public void addUIElements() {
		super.addUIElements();
		this.add(statusLayer);
		
		
	}
}
