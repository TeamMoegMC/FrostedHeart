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
	public float dialogX=0.1f;
	public float dialogY;
	public float dialogW;
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

	public void renderBackground(MatrixStack matrixStack) {
		
		
	}
	float lpartialTicks;
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		//AbstractGui.fill(matrixStack, 0, 0, width, height, 0xffffffff);
		partialTicks=Math.max(lpartialTicks, partialTicks);
		this.width=ClientScene.fromRelativeXW(1);
		this.height=ClientScene.fromRelativeYH(1);
		matrixStack.push();
		primary.render(new RenderParams(this,matrixStack,mouseX,mouseY,partialTicks));
		int y=ClientScene.fromRelativeYH(dialogY);
		int h=9*chatlist.size()+4;
		
		RenderSystem.enableAlphaTest();
		RenderSystem.enableBlend();
		matrixStack.translate(0, 0, 1);
		if(!chatlist.isEmpty())
			AbstractGui.fill(matrixStack, ClientScene.fromRelativeXW(dialogX)-2,ClientScene.fromRelativeYH(dialogY)-2, ClientScene.fromRelativeXW(dialogW)+2+ClientScene.fromRelativeXW(dialogX), h+ClientScene.fromRelativeYH(dialogY)-2, 0xC0000000);
		//this.fillGradient(matrixStack, ClientScene.fromRelativeXW(dialogX)-2, ClientScene.fromRelativeYH(dialogY)-2, ClientScene.fromRelativeXW(dialogW)+4, h, 0xC0101010, 0xD0101010);
		for(TextInfo i:chatlist) {
			int x=(ClientScene.fromRelativeXW(dialogW)-i.getCurLen())/2+ClientScene.fromRelativeXW(dialogX);
			this.minecraft.fontRenderer.drawTextWithShadow(matrixStack, i.asFinished(), x, y, 0xffffffff);
			y+=9;
		}
		RenderSystem.disableBlend();
		matrixStack.pop();
	}

	@Override
	public int getDialogWidth() {
		return ClientScene.fromRelativeXW(dialogW);
	}
	@Override
	public void tickDialog() {
		lpartialTicks=0;
		primary.tick();
	}
	@Override
	public void closeScreen() {
		ClientScene.dialog=null;
		primary.close();
		super.closeScreen();
	}
}
