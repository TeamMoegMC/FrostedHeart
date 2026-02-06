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

package com.teammoeg.frostedheart.content.scenario.client.gui.layered;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.content.scenario.client.ClientScene;
import com.teammoeg.frostedheart.content.scenario.client.dialog.IScenarioDialog;
import com.teammoeg.frostedheart.content.scenario.client.gui.layered.gl.GLLayerContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class RenderParams {
	IScenarioDialog screen;
	GuiGraphics matrixStack;
	int mouseX;
	int mouseY;
	float partialTicks;
	float opacity;
	float xzoom=1,yzoom=1;
	int x,y,width,height,offsetX,offsetY;
	boolean forceFirst;
	int contentWidth,contentHeight;
	public RenderParams(IScenarioDialog screen, GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		super();
		this.screen = screen;
		this.matrixStack = matrixStack;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.partialTicks = partialTicks;
		width=this.getScreenWidth();
		height=this.getScreenHeight();
		offsetX=offsetY=0;
		x=y=0;
		opacity=1;
	}


	public RenderParams(IScenarioDialog screen, GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks,
			float opacity, int x, int y, int width, int height, float xzoom, float yzoom, int offsetX,
			int offsetY,int contentWidth,int contentHeight) {
		super();
		this.screen = screen;
		this.matrixStack = matrixStack;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.partialTicks = partialTicks;
		this.opacity = opacity;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.xzoom = xzoom;
		this.yzoom = yzoom;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.contentWidth=contentWidth;
		this.contentHeight=contentHeight;
	}
	public RenderParams(IScenarioDialog screen, GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks,
			float opacity, int x, int y, int width, int height, float xzoom, float yzoom) {
		super();
		this.screen = screen;
		this.matrixStack = matrixStack;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.partialTicks = partialTicks;
		this.opacity = opacity;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.xzoom = xzoom;
		this.yzoom = yzoom;
		this.offsetX = 0;
		this.offsetY = 0;
	}
	public int getScreenWidth() {
		return ClientUtils.getMc().getWindow().getGuiScaledWidth();
	}
	public int getScreenHeight() {
		return ClientUtils.getMc().getWindow().getGuiScaledHeight();
	}
	public RenderParams copy() {
		return new RenderParams(screen, matrixStack, mouseX, mouseY, partialTicks, opacity, x, y, width, height,xzoom,yzoom,offsetX,offsetY,contentWidth,contentHeight);
	}
	public Font getFont() {
		return ClientUtils.getMc().font;
	}
	public RenderParams copyWithCurrent(GLLayerContent layer) {
		return new RenderParams(screen, matrixStack, mouseX-ClientScene.fromRelativeXW(layer.getX()), mouseY-ClientScene.fromRelativeYH(layer.getY()), partialTicks,
				opacity*layer.getOpacity(), x+ClientScene.fromRelativeXW(layer.getX()), y+ClientScene.fromRelativeYH(layer.getY()),
				Math.min(ClientScene.fromRelativeXW(layer.getWidth()),width-ClientScene.fromRelativeXW(layer.getX())),
				Math.min(ClientScene.fromRelativeYH(layer.getHeight()),height-ClientScene.fromRelativeYH(layer.getY())),
				xzoom,yzoom,offsetX,offsetY,ClientScene.fromRelativeXW(layer.getWidth()),ClientScene.fromRelativeYH(layer.getHeight()));
	}
	public IScenarioDialog getScreen() {
		return screen;
	}
	public PoseStack getMatrixStack() {
		return matrixStack.pose();
	}
	public void setGuiGraphics(GuiGraphics matrixStack) {
		this.matrixStack = matrixStack;
	}
	public GuiGraphics getGuiGraphics() {
		return this.matrixStack;
	}
	public int getMouseX() {
		return mouseX;
	}
	public void setMouseX(int mouseX) {
		this.mouseX = mouseX;
	}
	public int getMouseY() {
		return mouseY;
	}
	public void setMouseY(int mouseY) {
		this.mouseY = mouseY;
	}
	public float getPartialTicks() {
		return partialTicks;
	}
	public void setPartialTicks(float partialTicks) {
		this.partialTicks = partialTicks;
	}
	public float getOpacity() {
		return opacity;
	}
	public void setOpacity(float opacity) {
		this.opacity = opacity;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getContentX() {
		return x+offsetX;
	}
	public void setContentX(int x) {
		this.offsetX = x-this.x;
	}
	public int getContentY() {
		return y+offsetY;
	}
	public void setContentY(int y) {
		this.offsetY = y-this.y;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}

	public Minecraft getMinecraft() {
		return ClientUtils.getMc();
	}
	public int getContentWidth() {
		return (int) (xzoom*this.contentWidth);
	}
	public void setContentWidth(int contentWidth) {
		this.xzoom = contentWidth*1f/this.contentWidth;
	}
	public int getContentHeight() {
		return (int) (yzoom*this.contentHeight);
	}
	public void setContentHeight(int contentHeight) {
		this.yzoom = contentHeight*1f/this.contentHeight;
	}


	public boolean isForceFirst() {
		return forceFirst;
	}


	public void setForceFirst(boolean forceFirst) {
		this.forceFirst = forceFirst;
	}
}
