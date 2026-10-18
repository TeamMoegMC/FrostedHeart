/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
