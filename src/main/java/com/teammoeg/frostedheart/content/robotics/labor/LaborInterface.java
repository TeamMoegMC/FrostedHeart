package com.teammoeg.frostedheart.content.robotics.labor;

import java.util.function.BiConsumer;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.LevelSpinLayer;
import com.teammoeg.frostedheart.content.robotics.labor.LaborSlot.TypePool;

public class LaborInterface extends UILayer {
	LaborSlot slot;
	BiConsumer<LaborType,Integer> levelSetter;
	public LaborInterface(UIElement panel,LaborSlot slot,BiConsumer<LaborType,Integer> levelSetter) {
		super(panel);
		this.slot=slot;
		this.levelSetter=levelSetter;
	}

	@Override
	public void addUIElements() {
		for(LaborType type:slot.getType().getLaborTypes()) {
			LevelSpinLayer spinner;
			this.add(spinner=new LevelSpinLayer(this,slot.getType().maxLevel(type)) {
				@Override
				public void onValueChanged() {
					levelSetter.accept(type,this.getGhostValue());
				}
			});
			TypePool pool=slot.getSlot(type);
			pool.setLevel.bind(spinner::setGhostValue);
			pool.currentLevel.bind(spinner::setValue);
		}
	}

	@Override
	public void alignWidgets() {
		this.align(true);
		this.setSizeToContentSize();
		
	}

}
