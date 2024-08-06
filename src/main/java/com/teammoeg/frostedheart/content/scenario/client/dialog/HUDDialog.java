package com.teammoeg.frostedheart.content.scenario.client.dialog;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.LayerManager;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;

import net.minecraft.client.gui.GuiGraphics;

public class HUDDialog implements IScenarioDialog{
	private LayerManager primary=new LayerManager();
	public float handlePt(float partialTicks) {
		float delta=partialTicks-lpartialTicks;
		if(delta<0){
			delta=1-lpartialTicks+partialTicks;
		}
		cpartialTicks+=delta;
		if(cpartialTicks>1)
			cpartialTicks=1;
		lpartialTicks=partialTicks;
		return cpartialTicks;
	}
	float lpartialTicks;
	float cpartialTicks;
	@Override
	public void updateTextLines(List<TextInfo> queue) {
	}

	@Override
	public int getDialogWidth() {
		return 0;
	}
	public void render(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		//AbstractGui.fill(matrixStack, 0, 0, width, height, 0xffffffff);
		partialTicks=handlePt(partialTicks);
		getPrimary().render(new RenderParams(this,matrixStack,mouseX,mouseY,partialTicks));
	}
	@Override
	public void tickDialog() {
		cpartialTicks=0;
		getPrimary().tick();
		//System.out.println(ClientUtils.mc().getMainWindow().getGuiScaleFactor());
	}

	@Override
	public LayerManager getPrimary() {
		return primary;
	}

	@Override
	public void setPrimary(LayerManager primary) {
		cpartialTicks=0;
		this.primary=primary;
	}

	@Override
	public void closeDialog() {
		ClientScene.INSTANCE.dialog=null;
	}

	@Override
	public boolean hasDialog() {
		return false;
	}

}
