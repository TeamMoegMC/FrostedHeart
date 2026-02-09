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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl;

import java.util.List;

import com.teammoeg.chorda.math.Rect;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderParams;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.RenderableContent;

import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;

public class GLTextContent extends GLLayerContent{
	
	public GLTextContent(Component text, float x, float y, float w, float h, boolean b) {
		super(x, y, w, h);
		this.shadow = b;
		this.text = text;
	}

	@Override
	public RenderableContent copy() {
		return new GLTextContent(text,x, y, width, height, z, shadow,resize);
	}
	public GLTextContent(Component text,float x, float y, float width, float height, int z, boolean shadow, float resize) {
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
	public Component text;
	@Override
	public void renderContents(RenderParams params) {
		
		List<FormattedCharSequence> li=ComponentRenderUtils.wrapComponents(text,params.getContentWidth() ,params.getMinecraft().font);

		int y=0;
		int ty=params.getContentY();
		if(centerV)
			ty=Math.max((int) ((params.getContentHeight()-li.size()*9*resize)/2),0)+params.getContentY();
		// RenderSystem.enableBlend();
		 //RenderSystem.enableAlphaTest();
		 params.getMatrixStack().pushPose();
		 params.getMatrixStack().translate(params.getContentX(),ty, 0);
		 params.getMatrixStack().scale(resize, resize, resize);
		for(FormattedCharSequence i:li) {
			int w=params.getFont().width(ClientScene.toString(i));
			int tx=0;
			if(centerH) {
				tx=(int) Math.max(0,(params.getContentWidth()/resize-w)/2);
			}
			if(shadow)
				params.getGuiGraphics().drawString(params.getFont(), i, tx, y, color|(((int)(0xFF*params.getOpacity()))<<24));
				//params.getMinecraft().font.drawShadow(params.getMatrixStack(), i, tx, y, color|(((int)(0xFF*params.getOpacity()))<<24));
			else
				params.getGuiGraphics().drawString(params.getFont(), i, tx, y, color|(((int)(0xFF*params.getOpacity()))<<24),false);
			y+=9;
			if(y>params.getContentHeight()+params.getContentY())break;
		}
		params.getMatrixStack().popPose();
		 
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