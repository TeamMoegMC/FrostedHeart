package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class TextContent extends LayerContent{

	public TextContent(ITextComponent text, int x, int y, int w, int h, boolean b) {
		super(x, y, w, h);
		this.shadow = b;
		this.text = text;
	}
	public TextContent(ITextComponent text,int x, int y, int width, int height, int z, boolean shadow) {
		super(x, y, width, height,z);
		this.shadow = shadow;
		this.text = text;
	}
	@Override
	public RenderableContent copy() {
		return new TextContent(text,x, y, width, height, z, shadow);
	}
	boolean shadow=false;
	public boolean isShadow() {
		return shadow;
	}
	public void setShadow(boolean shadow) {
		this.shadow = shadow;
	}
	public ITextComponent text;
	@Override
	public void renderContents(RenderParams params) {
		List<IReorderingProcessor> li=RenderComponentsUtil.func_238505_a_(text,params.getContentWidth() ,params.getMinecraft().fontRenderer);
		int y=params.getContentY();
		 RenderSystem.enableBlend();
		 RenderSystem.enableAlphaTest();
		for(IReorderingProcessor i:li) {
			if(shadow)
				params.getMinecraft().fontRenderer.drawTextWithShadow(params.getMatrixStack(), i, params.getContentX()+0, y, 0xFFFFFF|(((int)(0xFF*params.getOpacity()))<<24));
			else
				params.getMinecraft().fontRenderer.func_238422_b_(params.getMatrixStack(), i, params.getContentX()+0, y, 0xFFFFFF|(((int)(0xFF*params.getOpacity()))<<24));
			y+=9;
			if(y>params.getContentHeight()+params.getContentY())break;
		}
		RenderSystem.disableBlend();
	}
	@Override
	public void tick() {
	}



	
}