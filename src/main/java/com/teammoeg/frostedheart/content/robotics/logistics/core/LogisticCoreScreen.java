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
import com.teammoeg.chorda.client.cui.widgets.LimitedTextField;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.robotics.labor.LaborInterface;
import com.teammoeg.frostedheart.content.robotics.labor.LaborType;

import net.minecraft.client.gui.GuiGraphics;

public class LogisticCoreScreen extends MenuPrimaryLayer<LogisticCoreMenu>{
	LimitedTextField effeciency;
	LaborInterface spinner;
	LimitedTextField netStat;
	
	public LogisticCoreScreen(LogisticCoreMenu menu) {
		super(menu);
		spinner=new LaborInterface(this,menu.laborSlot,this::setValue);
		effeciency=new LimitedTextField(this,Components.empty(),180);
		effeciency.setTitle(Components.literal("Effeciency"));
		netStat=new LimitedTextField(this,Components.empty(),180);
		menu.taskSize.bind(n->this.updateQueueSize(n, menu.workerSize.getValue(), menu.networkSize.getValue()));
		menu.workerSize.bind(n->this.updateQueueSize(menu.taskSize.getValue(), n, menu.networkSize.getValue()));
		menu.networkSize.bind(n->this.updateQueueSize(menu.taskSize.getValue(), menu.workerSize.getValue(), n));
	}
	public void setValue(LaborType type,int value) {
		menu.sendMessage(LaborType.registry.idOf(type), value);
	}
	public void updateQueueSize(int queue,int worker,int size) {
		netStat.setTitle(Components.literal("queued:"+queue+" working:"+worker+" terminals:"+size));
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
		this.add(netStat);
		
	}

	@Override
	public void alignWidgets() {
		this.align(true);
		this.setSize(176, 200);
	}


    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h, RenderingHint hint) {
    	hint.theme(this).drawUIBackground(matrixStack, x-5, y-5, w+10, h+10);
    }
}
