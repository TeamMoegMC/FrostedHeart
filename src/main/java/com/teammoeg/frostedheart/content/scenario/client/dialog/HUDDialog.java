/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.scenario.client.dialog;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class HUDDialog implements IScenarioDialog{
	private LayerManager primary=new LayerManager();
	@Override
	public void updateTextLines(List<TextInfo> queue) {
	}

	@Override
	public int getDialogWidth() {
		return 0;
	}
	public void render(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		//AbstractGui.fill(matrixStack, 0, 0, width, height, 0xffffffff);
		getPrimary().render(new RenderParams(this,matrixStack,mouseX,mouseY,partialTicks));
	}
	@Override
	public void tickDialog() {
		getPrimary().tick();
		//System.out.println(ClientUtils.mc().getMainWindow().getGuiScaleFactor());
	}

	@Override
	public LayerManager getPrimary() {
		return primary;
	}

	@Override
	public void setPrimary(LayerManager primary) {
		this.primary=primary;
	}

	@Override
	public void closeDialog() {
		ClientScene.INSTANCE.dialog=null;
		ClientScene.INSTANCE.onTransitionComplete.setFinished();
	}

	@Override
	public boolean hasDialog() {
		return false;
	}

}
