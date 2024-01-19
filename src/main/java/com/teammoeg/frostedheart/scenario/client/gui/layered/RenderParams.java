package com.teammoeg.frostedheart.scenario.client.gui.layered;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.scenario.client.ClientScene;

import blusunrize.immersiveengineering.client.ClientUtils;
import net.minecraft.client.Minecraft;

public class RenderParams {
	ImageScreenDialog screen;
	MatrixStack matrixStack;
	int mouseX;
	int mouseY;
	float partialTicks;
	float opacity;
	int x,y,width,height,contentWidth,contentHeight,offsetX,offsetY;
	boolean forceFirst;
	public RenderParams(ImageScreenDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		super();
		this.screen = screen;
		this.matrixStack = matrixStack;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.partialTicks = partialTicks;
		width=this.getScreenWidth();
		height=this.getScreenHeight();
		contentWidth=width;
		contentHeight=height;
		offsetX=offsetY=0;
		x=y=0;
		opacity=1;
	}


	public RenderParams(ImageScreenDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks,
			float opacity, int x, int y, int width, int height, int contentWidth, int contentHeight, int offsetX,
			int offsetY) {
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
		this.contentWidth = contentWidth;
		this.contentHeight = contentHeight;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}
	public RenderParams(ImageScreenDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks,
			float opacity, int x, int y, int width, int height, int contentWidth, int contentHeight) {
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
		this.contentWidth = contentWidth;
		this.contentHeight = contentHeight;
		this.offsetX = 0;
		this.offsetY = 0;
	}
	public int getScreenWidth() {
		return ClientUtils.mc().getMainWindow().getScaledWidth();
	}
	public int getScreenHeight() {
		return ClientUtils.mc().getMainWindow().getScaledHeight();
	}
	public RenderParams copy() {
		return new RenderParams(screen, matrixStack, mouseX, mouseY, partialTicks, opacity, x, y, width, height,contentWidth,contentHeight,offsetX,offsetY);
	}
	public RenderParams copyWithCurrent(GLLayerContent layer) {
		return new RenderParams(screen, matrixStack, mouseX-ClientScene.fromRelativeXW(layer.x), mouseY-ClientScene.fromRelativeYH(layer.y), partialTicks, opacity*layer.opacity, x+ClientScene.fromRelativeXW(layer.x), y+ClientScene.fromRelativeYH(layer.y), Math.min(ClientScene.fromRelativeXW(layer.width),width-ClientScene.fromRelativeXW(layer.x)), Math.min(ClientScene.fromRelativeYH(layer.height),height-ClientScene.fromRelativeYH(layer.y)),ClientScene.fromRelativeXW(layer.width),ClientScene.fromRelativeYH(layer.height));
	}
	public ImageScreenDialog getScreen() {
		return screen;
	}
	public void setScreen(ImageScreenDialog screen) {
		this.screen = screen;
	}
	public MatrixStack getMatrixStack() {
		return matrixStack;
	}
	public void setMatrixStack(MatrixStack matrixStack) {
		this.matrixStack = matrixStack;
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
		this.offsetX = y-this.y;
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
		return screen.getMinecraft();
	}
	public int getContentWidth() {
		return contentWidth;
	}
	public void setContentWidth(int contentWidth) {
		this.contentWidth = contentWidth;
	}
	public int getContentHeight() {
		return contentHeight;
	}
	public void setContentHeight(int contentHeight) {
		this.contentHeight = contentHeight;
	}


	public boolean isForceFirst() {
		return forceFirst;
	}


	public void setForceFirst(boolean forceFirst) {
		this.forceFirst = forceFirst;
	}
}
