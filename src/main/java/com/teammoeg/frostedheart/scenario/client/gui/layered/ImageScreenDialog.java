package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.util.ArrayList;
import java.util.List;

import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.client.ClientScene.TextInfo;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.scenario.client.IScenarioDialog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

public class ImageScreenDialog extends Screen implements IScenarioDialog {
	public int dialogX=30;
	public int dialogY;
	public int dialogW;
	public boolean alignMiddle=true;
	public LayerManager primary=new LayerManager();
	public int escapes;
	@Override
	public boolean shouldCloseOnEsc() {
		// TODO Auto-generated method stub
		escapes++;
		return escapes>5;
	}
	public ImageScreenDialog(ITextComponent titleIn) {
		super(titleIn);
		
		
		// TODO Auto-generated constructor stub
	}
	@Override
	public void init(Minecraft minecraft, int width, int height) {
		// TODO Auto-generated method stub
		super.init(minecraft, width, height);
		dialogW=width-60;
		dialogY=height-60;
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

	public void renderBackground(MatrixStack matrixStack) {
		
		
	}

	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		AbstractGui.fill(matrixStack, 0, 0, width, height, 0xffffffff);
		primary.render(new RenderParams(this,matrixStack,mouseX,mouseY,partialTicks));
		int y=dialogY;
		int h=9*chatlist.size()+4;
		 RenderSystem.enableBlend();
		 RenderSystem.enableAlphaTest();
		this.fillGradient(matrixStack, dialogX-2, dialogY-2, dialogW+4, h, 0xC0101010, 0xD0101010);
		for(TextInfo i:chatlist) {
			int x=(dialogW-i.getCurLen())/2+dialogX;
			this.minecraft.fontRenderer.drawTextWithShadow(matrixStack, i.asFinished(), x, y, 0xffffffff);
			y+=9;
		}
		RenderSystem.disableBlend();
	}

	@Override
	public int getDialogWidth() {
		return dialogW;
	}
	@Override
	public void tickDialog() {
		primary.tick();
	}
}
