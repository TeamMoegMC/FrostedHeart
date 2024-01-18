package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

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
	public void renderContents(ImageScreenDialog screen, MatrixStack matrixStack,int mouseX,int mouseY, float partialTicks,float opacity) {
		List<IReorderingProcessor> li=RenderComponentsUtil.func_238505_a_(text,this.width ,screen.getMinecraft().fontRenderer);
		int y=this.y;
		
		for(IReorderingProcessor i:li) {
			if(shadow)
				screen.getMinecraft().fontRenderer.drawTextWithShadow(matrixStack, i, x+0, y, 0xFFFFFF|(((int)(0xFF*opacity))<<24));
			else
				screen.getMinecraft().fontRenderer.func_238422_b_(matrixStack, i, x+0, y, 0xFFFFFF|(((int)(0xFF*opacity))<<24));
			y+=9;
		}
	}
	@Override
	public void tick() {
	}



	
}