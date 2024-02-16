package com.teammoeg.frostedheart.client.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ImageButton extends Button {
	int xTexStart;
	int yTexStart;
	private final int textureWidth;
	private final int textureHeight;
	int state;
	ResourceLocation TEXTURE;
	public ImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
			Button.IPressable onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, EMPTY_TOOLTIP, onPressIn);
	}

	public ImageButton(ResourceLocation texture,int xIn, int yIn, int widthIn, int heightIn, int xTexStartIn, int yTexStartIn,
			Button.ITooltip tt, Button.IPressable onPressIn) {
		this(texture,xIn, yIn, widthIn, heightIn, xTexStartIn, yTexStartIn, 256, 256, onPressIn, tt,
				StringTextComponent.EMPTY);
	}

	public ImageButton(ResourceLocation texture,int x, int y, int width, int height, int xTexStart, int yTexStart, int textureWidth,
			int textureHeight, Button.IPressable onPress, ITextComponent title) {
		this(texture,x, y, width, height, xTexStart, yTexStart, textureWidth, textureHeight, onPress, EMPTY_TOOLTIP, title);
	}

	public ImageButton(ResourceLocation texture,int p_i244513_1_, int p_i244513_2_, int p_i244513_3_, int p_i244513_4_, int p_i244513_5_,
			int p_i244513_6_, int p_i244513_9_, int p_i244513_10_, Button.IPressable p_i244513_11_,
			Button.ITooltip p_i244513_12_, ITextComponent p_i244513_13_) {
		super(p_i244513_1_, p_i244513_2_, p_i244513_3_, p_i244513_4_, p_i244513_13_, p_i244513_11_, p_i244513_12_);
		this.TEXTURE=texture;
		this.textureWidth = p_i244513_9_;
		this.textureHeight = p_i244513_10_;
		this.xTexStart = p_i244513_5_;
		this.yTexStart = p_i244513_6_;
	}

	public void setPosition(int xIn, int yIn) {
		this.x = xIn;
		this.y = yIn;
	}

	public void renderWidget(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		int i = 0, j = state * this.height;

		if (this.isHovered()) {
			i += this.width;
		}
		Minecraft.getInstance().getTextureManager().bindTexture(TEXTURE);
		RenderSystem.enableDepthTest();
		blit(matrixStack, this.x, this.y, this.xTexStart + i, this.yTexStart + j, this.width, this.height,
				this.textureWidth, this.textureHeight);
		if (this.isHovered()) {
			this.renderToolTip(matrixStack, mouseX, mouseY);
		}

	}
}