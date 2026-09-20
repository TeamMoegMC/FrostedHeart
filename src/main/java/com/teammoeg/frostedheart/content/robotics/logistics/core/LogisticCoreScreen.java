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

package com.teammoeg.frostedheart.content.robotics.logistics.core;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.widgets.LevelSpinLayer;
import com.teammoeg.chorda.client.cui.widgets.LimitedTextField;
import com.teammoeg.chorda.text.Components;

import net.minecraft.client.gui.GuiGraphics;

public class LogisticCoreScreen extends MenuPrimaryLayer<LogisticCoreMenu>{
	LimitedTextField effeciency;
	LevelSpinLayer spinner;
	LimitedTextField actual;
	LimitedTextField labour;
	
	public LogisticCoreScreen(LogisticCoreMenu menu) {
		super(menu);
		spinner=new LevelSpinLayer(this,10) {

			@Override
			public void onValueChanged() {
				setValue(this.getValue());
			}
			
		};
		effeciency=new LimitedTextField(this,Components.empty(),180);
		effeciency.setTitle(Components.literal("Effeciency"));
		actual=new LimitedTextField(this,Components.empty(),180);
		labour=new LimitedTextField(this,Components.empty(),180);
		menu.setLevel.bind(spinner::setValue);
		menu.currentLevel.bind(this::updateLevel);
		menu.totalLabour.bind(n->this.updateLabour(menu.currentLabour.getValue(), menu.desiredLabour.getValue(), n));
		menu.desiredLabour.bind(n->this.updateLabour(menu.currentLabour.getValue(), n, menu.totalLabour.getValue()));
		menu.currentLabour.bind(n->this.updateLabour(n, menu.desiredLabour.getValue(), menu.totalLabour.getValue()));
	}
	public void setValue(int value) {
		menu.sendMessage(0, value);
	}
	public void updateLabour(int actual,int set,int total) {
		labour.setTitle(Components.literal("labour:"+actual+"("+set+") / "+total));
	}
	public void updateLevel(int level) {
		actual.setTitle(Components.literal("level:"+level+" / "+10));
	}
	@Override
	public boolean onInit() {
		
		return super.onInit();
	}
	
	@Override
	public void addChildUIElements() {
		super.addChildUIElements();
		this.add(effeciency);
		this.add(spinner);
		this.add(actual);
		this.add(labour);
		
	}

	@Override
	public void alignWidgets() {
		this.setSize(176, 200);
		effeciency.setPos(20, 8);
		spinner.setPos(20, 20);
		actual.setPos(20, 32);
		labour.setPos(20, 44);
	}


    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h, RenderingHint hint) {
    	hint.theme(this).drawUIBackground(matrixStack, x-5, y-5, w+10, h+10);
    }
}
