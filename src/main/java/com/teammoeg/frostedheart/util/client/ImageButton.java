package com.teammoeg.frostedheart.util.client;

import com.teammoeg.frostedheart.util.TranslateUtils;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class ImageButton extends Button {
	int xTexStart;
	int yTexStart;
	private final int textureWidth;
	private final int textureHeight;
	int state;
	ResourceLocation TEXTURE;
	public ImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
			Button.OnPress onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, null, onPressIn);
	}

	public ImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
		Tooltip tt, Button.OnPress onPressIn) {
	
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, 256, 256, onPressIn, tt,
			TranslateUtils.empty());
	}

	public ImageButton(ResourceLocation texture,int x, int y, int width, int height, int xTexStart, int yTexStart, int textureWidth,
			int textureHeight, Button.OnPress onPress, Component title) {
		this(texture,x, y, width, height, xTexStart, yTexStart, textureWidth, textureHeight, onPress, null, title);
	}

	public ImageButton(ResourceLocation texture,int p_i244513_1_, int p_i244513_2_, int p_i244513_3_, int p_i244513_4_, int p_i244513_5_,
			int p_i244513_6_, int p_i244513_9_, int p_i244513_10_, Button.OnPress p_i244513_11_,
			Tooltip p_i244513_12_, Component p_i244513_13_) {
		super(Button.builder(p_i244513_13_, p_i244513_11_).bounds(p_i244513_2_, p_i244513_3_, p_i244513_4_, p_i244513_5_).tooltip(p_i244513_12_));
		this.TEXTURE=texture;
		this.textureWidth = p_i244513_9_;
		this.textureHeight = p_i244513_10_;
		this.xTexStart = p_i244513_5_;
		this.yTexStart = p_i244513_6_;
	}

	public void setPosition(int xIn, int yIn) {
		this.setX(xIn);
		this.setY(yIn);
	}

	public void renderButton(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
		int i = 0, j = state * this.height;

		if (this.isHovered()) {
			i += this.width;
		}
		RenderSystem.enableDepthTest();
		matrixStack.blit(TEXTURE, this.getX(), this.getY(), this.xTexStart + i, this.yTexStart + j, this.width, this.height,
				this.textureWidth, this.textureHeight);

	}
}