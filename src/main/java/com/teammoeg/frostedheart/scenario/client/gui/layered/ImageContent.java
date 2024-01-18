package com.teammoeg.frostedheart.scenario.client.gui.layered;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import blusunrize.immersiveengineering.client.ClientUtils;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;

public class ImageContent extends LayerContent {
	public ResourceLocation showingImage;
	int u, v, uw, uh, tw, th;

	public ImageContent(int x, int y, int width, int height, int z, ResourceLocation showingImage, int u, int v, int uw, int uh, int tw, int th) {
		super(x, y, width, height, z);
		this.showingImage = showingImage;
		this.u = u;
		this.v = v;
		this.uw = uw;
		this.uh = uh;
		this.tw = tw;
		this.th = th;
	}

	@Override
	public RenderableContent copy() {
		return new ImageContent(x, y, width, height, z, showingImage, u, v, uw, uh, tw, th);
	}

	@Override
	public void renderContents(ImageScreenDialog screen, MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, float opacity) {

		ClientUtils.bindTexture(showingImage);

		blit(matrixStack, x, y, width, height, u, v, uw, uh, tw, th, opacity);
	}

	public static void blit(MatrixStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight,float opacity) {
		innerBlit(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight,opacity);
	}

	public static void innerBlit(MatrixStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight,
		float opacity) {
		innerBlit(matrixStack.getLast().getMatrix(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / (float) textureWidth, (uOffset + (float) uWidth) / (float) textureWidth,
			(vOffset + 0.0F) / (float) textureHeight, (vOffset + (float) vHeight) / (float) textureHeight, opacity);
	}

	public static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU, float maxU, float minV, float maxV, float opacity) {
		BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
		bufferbuilder.pos(matrix, (float) x1, (float) y2, (float) blitOffset).color(1, 1, 1, opacity).tex(minU, maxV).endVertex();
		bufferbuilder.pos(matrix, (float) x2, (float) y2, (float) blitOffset).color(1, 1, 1, opacity).tex(maxU, maxV).endVertex();
		bufferbuilder.pos(matrix, (float) x2, (float) y1, (float) blitOffset).color(1, 1, 1, opacity).tex(maxU, minV).endVertex();
		bufferbuilder.pos(matrix, (float) x1, (float) y1, (float) blitOffset).color(1, 1, 1, opacity).tex(minU, minV).endVertex();
		bufferbuilder.finishDrawing();
		RenderSystem.enableAlphaTest();
		WorldVertexBufferUploader.draw(bufferbuilder);
	}

	public ImageContent(ResourceLocation showingImage, int x, int y, int w, int h, int u, int v, int uw, int uh, int tw, int th) {
		super(x, y, w, h);
		this.showingImage = showingImage;
		this.u = u;
		this.v = v;
		this.uw = uw;
		this.uh = uh;
		this.tw = tw;
		this.th = th;
	}

	public ImageContent(ResourceLocation showingImage, int uw, int uh, int tw, int th) {
		super(0, 0, -1, -1);
		this.showingImage = showingImage;
		this.u = 0;
		this.v = 0;
		this.uw = uw;
		this.uh = uh;
		this.tw = tw;
		this.th = th;
	}

	@Override
	public void tick() {
	}

}