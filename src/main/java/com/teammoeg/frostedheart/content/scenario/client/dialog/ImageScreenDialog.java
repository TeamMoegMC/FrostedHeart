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

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.client.PartialTickTracker;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ImageScreenDialog extends Screen implements IScenarioDialog {
	public float dialogX=0.1f;
	public float dialogY;
	public float dialogW;
	
	private LayerManager primary=new LayerManager();
	public boolean alignMiddle=true;
	public static final int MAX_ESCAPE=5;
	public int escapes=MAX_ESCAPE;
	int ticksSinceLastSpace=0;
	@Override
	public boolean shouldCloseOnEsc() {
		escapes--;
		ticksSinceLastSpace=20;
		return escapes<=0;
	}
	public ImageScreenDialog(Component titleIn) {
		super(titleIn);
		dialogW=0.8f;
		dialogY=0.85f;
	}
	List<TextInfo> chatlist=new ArrayList<>();
	@Override
	public void updateTextLines(List<TextInfo> queue) {
		chatlist.clear();
		for(TextInfo ti:queue) {
			if(ti.hasText()) {
				chatlist.add(ti);
			}
		}
	}

	public boolean isPauseScreen() {
		return false;

	}

	public void renderBackground(PoseStack matrixStack) {
		
		
	}
	public void render(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		//AbstractGui.fill(matrixStack, 0, 0, width, height, 0xffffffff);
		partialTicks=PartialTickTracker.getTickAlignedPartialTicks();
		this.width=ClientScene.fromRelativeXW(1);
		this.height=ClientScene.fromRelativeYH(1);
		matrixStack.pose().pushPose();
		getPrimary().render(new RenderParams(this,matrixStack,mouseX,mouseY,partialTicks));
		int y=ClientScene.fromRelativeYH(dialogY);
		int h=9*chatlist.size()+4;
		
		//RenderSystem.enableAlphaTest();
		
		RenderSystem.enableBlend();
		if(!chatlist.isEmpty())
			matrixStack.fill(ClientScene.fromRelativeXW(dialogX)-2,ClientScene.fromRelativeYH(dialogY)-2, ClientScene.fromRelativeXW(dialogW)+2+ClientScene.fromRelativeXW(dialogX), h+ClientScene.fromRelativeYH(dialogY)-2, 0xC0000000);
		//this.fillGradient(matrixStack, ClientScene.fromRelativeXW(dialogX)-2, ClientScene.fromRelativeYH(dialogY)-2, ClientScene.fromRelativeXW(dialogW)+4, h, 0xC0101010, 0xD0101010);
		for(TextInfo i:chatlist) {
			int x=(ClientScene.fromRelativeXW(dialogW)-i.getCurLen())/2+ClientScene.fromRelativeXW(dialogX)+12;
			matrixStack.drawString(this.minecraft.font, i.asFinished(), x, y, 0xffffffff);
			y+=9;
		}
		RenderSystem.disableBlend();
		matrixStack.pose().popPose();
		if(escapes!=MAX_ESCAPE) {
			matrixStack.drawString(this.minecraft.font, Lang.translateMessage("escape_count",escapes), 10, 10, 0xFFAAAAAA);
		}
	}


	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int y=ClientScene.fromRelativeYH(dialogY);
		int h=9*chatlist.size();
		int dy=(int) (mouseY-y);
		if(dy>0&&dy<h) {
			int iline=dy/font.lineHeight;
			 TextInfo line = chatlist.get(iline);
			 int crlen=line.getCurLen();
			 int x=(ClientScene.fromRelativeXW(dialogW)-crlen)/2+ClientScene.fromRelativeXW(dialogX)+12;
			 int dx=(int) (mouseX-x);
			 if(dx>0&&dx<crlen) {
				 return this.handleComponentClicked(font.getSplitter().componentStyleAtWidth(line.asFinished(), dx));

			 }
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

    @Override
	public int getDialogWidth() {
		return ClientScene.fromRelativeXW(dialogW);
	}
	@Override
	public void tickDialog() {
		getPrimary().tick();
		if(escapes!=MAX_ESCAPE) {
			ticksSinceLastSpace--;
			if(ticksSinceLastSpace<=0)
				escapes=MAX_ESCAPE;
		}
	}
	@Override
	public void onClose() {
		ClientScene.INSTANCE.dialog=null;
		getPrimary().close();
		super.onClose();
	}
	@Override
	public void closeDialog() {
		onClose();
	}
	public LayerManager getPrimary() {
		return primary;
	}
	public void setPrimary(LayerManager primary) {
		this.primary = primary;
	}
	@Override
	public boolean hasDialog() {
		return true;
	}

}
