package com.teammoeg.frostedheart.scenario.client.gui.layered;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.scenario.client.ClientScene;

import net.minecraft.client.gui.RenderComponentsUtil;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class TextContent extends LayerContent{

	public TextContent(ITextComponent text, float x, float y, float w, float h, boolean b) {
		super(x, y, w, h);
		this.shadow = b;
		this.text = text;
	}

	@Override
	public RenderableContent copy() {
		return new TextContent(text,x, y, width, height, z, shadow,resize);
	}
	public TextContent(ITextComponent text,float x, float y, float width, float height, int z, boolean shadow, float resize) {
		super(x, y, width, height, z);
		this.shadow = shadow;
		this.resize = resize;
		this.text = text;
	}
	boolean shadow=false;
	float resize=1;
	public boolean centerH;
	public boolean centerV;
	public int color=0xFFFFFF;
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

		int y=0;
		int ty=params.getContentY();
		if(centerV)
			ty=Math.max((int) ((params.getContentHeight()-li.size()*9*resize)/2),0)+params.getContentY();
		// RenderSystem.enableBlend();
		 //RenderSystem.enableAlphaTest();
		 params.getMatrixStack().push();
		 params.getMatrixStack().translate(params.getContentX(),ty, 0);
		 params.getMatrixStack().scale(resize, resize, resize);
		for(IReorderingProcessor i:li) {
			int w=params.getMinecraft().fontRenderer.getStringWidth(ClientScene.toString(i));
			int tx=0;
			if(centerH) {
				tx=(int) Math.max(0,(params.getContentWidth()/resize-w)/2);
			}
			if(shadow)
				params.getMinecraft().fontRenderer.drawTextWithShadow(params.getMatrixStack(), i, tx, y, color|(((int)(0xFF*params.getOpacity()))<<24));
			else
				params.getMinecraft().fontRenderer.func_238422_b_(params.getMatrixStack(), i,tx, y, color|(((int)(0xFF*params.getOpacity()))<<24));
			y+=9;
			if(y>params.getContentHeight()+params.getContentY())break;
		}
		params.getMatrixStack().pop();
		 
		//RenderSystem.disableBlend();
	}
	@Override
	public void tick() {
	}
	public float getResize() {
		return resize;
	}
	public void setResize(float resize) {
		this.resize = resize;
	}



	
}