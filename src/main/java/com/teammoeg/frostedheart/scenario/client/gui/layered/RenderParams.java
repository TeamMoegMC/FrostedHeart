package com.teammoeg.frostedheart.scenario.client.gui.layered;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.scenario.client.ClientScene;
import com.teammoeg.frostedheart.scenario.client.dialog.IScenarioDialog;
import com.teammoeg.frostedheart.scenario.client.dialog.ImageScreenDialog;
import com.teammoeg.frostedheart.scenario.client.gui.layered.gl.GLLayerContent;

import com.teammoeg.frostedheart.client.util.ClientUtils;
import net.minecraft.client.Minecraft;

public class RenderParams {
	IScenarioDialog screen;
	MatrixStack matrixStack;
	int mouseX;
	int mouseY;
	float partialTicks;
	float opacity;
	float xzoom=1,yzoom=1;
	int x,y,width,height,offsetX,offsetY;
	boolean forceFirst;
	int contentWidth,contentHeight;
	public RenderParams(IScenarioDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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


	public RenderParams(IScenarioDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks,
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
	public RenderParams(IScenarioDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks,
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
		return ClientUtils.mc().getMainWindow().getScaledWidth();
	}
	public int getScreenHeight() {
		return ClientUtils.mc().getMainWindow().getScaledHeight();
	}
	public RenderParams copy() {
		return new RenderParams(screen, matrixStack, mouseX, mouseY, partialTicks, opacity, x, y, width, height,xzoom,yzoom,offsetX,offsetY,contentWidth,contentHeight);
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
		return ClientUtils.mc();
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
