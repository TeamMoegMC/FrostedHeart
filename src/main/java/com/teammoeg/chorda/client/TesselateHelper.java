package com.teammoeg.chorda.client;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Divisor;
import com.teammoeg.chorda.math.Rect;

import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public abstract class TesselateHelper implements AutoCloseable {
	private static final TextureTesselator TEXTURE = new TextureTesselator();
	private static final ShapeTesslator SHAPE=new ShapeTesslator();
	private static final LineTesslator LINE=new LineTesslator();
	@Override
	public void close() {
		endTesselate();
	}

	private TesselateHelper() {
	}

	protected abstract void beginBuffer();

	@Getter
	protected BufferBuilder bufferbuilder;

	public void beginTesselate() {
		
		bufferbuilder = Tesselator.getInstance().getBuilder();
		beginBuffer();
	}

	public void endTesselate() {
		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.disableBlend();
	}

	public static TextureTesselator getTextureTesselator(ResourceLocation showingImage) {
		RenderSystem.setShaderTexture(0, showingImage);
		TEXTURE.beginTesselate();
		return TEXTURE;
	}
	public static ShapeTesslator getShapeTesslator() {
		SHAPE.beginTesselate();
		return SHAPE;
	}
	public static LineTesslator getLineTesslator() {
		LINE.beginTesselate();
		return LINE;
	}
	public static class LineTesslator extends TesselateHelper{
		@Override
		protected void beginBuffer() {
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
		}

		private LineTesslator drawVertexLine(Matrix4f mat, int color, int startX, int startY,
				int endX, int endY, float z) {
			bufferbuilder.vertex(mat, endX, endY, z).color(color).endVertex();
			bufferbuilder.vertex(mat, startX, startY, z).color(color).endVertex();
			return this;
		}

		private LineTesslator drawVertexLine2(Matrix4f mat, int color, int startX, int startY,
				int endX, int endY, float z) {
			// RenderSystem.disableTexture();
			// RenderSystem.enableColorMaterial();
			// RenderSystem.colorMask(false, false, false, false);

			bufferbuilder.vertex(mat, startX, 0, z).color(color).endVertex();
			bufferbuilder.vertex(mat, endX, 0, z).color(color).endVertex();
			bufferbuilder.vertex(mat, 0, startY, z).color(color).endVertex();
			bufferbuilder.vertex(mat, 0, endY, z).color(color).endVertex();
			// RenderSystem.enableTexture();
			return this;
		}

		// draw a line from start to end by color, ABSOLUTE POSITION

		public LineTesslator drawLine(Matrix4f mat, int color, int startX, int startY, int endX, int endY, float z) {

			return drawVertexLine(mat,  color, startX, startY, endX, endY, z);
		}

		// draw a line from start to end by color, ABSOLUTE POSITION
		// TODO: THE LINES IS NOT SHOWING
		public LineTesslator drawLine(GuiGraphics graphics, int color, int startX, int startY, int endX, int endY) {
			return drawVertexLine(graphics.pose().last().pose(), color, startX, startY, endX, endY, 0f);
		}

	}
	public static class ShapeTesslator extends TesselateHelper{
		@Override
		public void beginBuffer() {
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		}
	
		public ShapeTesslator fillRect(Matrix4f mat, Rect rect, int color) {
			fillRect(mat, rect.getX(), rect.getY(), rect.getX2(), rect.getY2(), color);
			return this;
		}
		public ShapeTesslator drawRect(Matrix4f mat, Rect rect, int color, boolean inner) {
			drawRect(mat, rect.getX(), rect.getY(), rect.getX2(), rect.getY2(), color,inner);
			return this;
		}
		public ShapeTesslator drawRect(Matrix4f mat, int x, int y, int x2, int y2, int color, boolean inner) {
			if (inner) {
				fillRect(mat, x, y, x2, y + 1, color); // top
				fillRect(mat, x, y2 - 1, x2, y2, color); // bottom
				fillRect(mat, x, y, x + 1, y2, color); // left
				fillRect(mat, x2 - 1, y, x2, y2, color); // right
			} else {
				fillRect(mat, x - 1, y - 1, x2 + 1, y, color); // top
				fillRect(mat, x - 1, y2, x2 + 1, y2 + 1, color); // bottom
				fillRect(mat, x - 1, y, x, y2, color); // left
				fillRect(mat, x2, y, x2 + 1, y2, color); // right
			}
			return this;
		}
		public ShapeTesslator drawRectWH(Matrix4f mat, int x, int y, int w, int h, int color, boolean inner) {
			int x2 = x + w;
			int y2 = y + h;
			return drawRect(mat, x, y, x2, y2, color, inner);
		}
		public ShapeTesslator fillRect(Matrix4f mat, int x, int y, int w, int h, int color) {
			bufferbuilder.vertex(mat, x, y, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color),
					FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color)).endVertex();
			bufferbuilder.vertex(mat, x + w, y, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color),
					FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color)).endVertex();
			bufferbuilder.vertex(mat, x + w, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color),
					FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color)).endVertex();
			bufferbuilder.vertex(mat, x, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color),
					FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color)).endVertex();
			return this;
		}

		public ShapeTesslator fillGradient(Matrix4f matrix, int x1, int y1, int x2, int y2, int colorB,
				int colorA) {
			int f  = FastColor.ARGB32.alpha(colorA);
			int f1 = FastColor.ARGB32.red(colorA);
			int f2 = FastColor.ARGB32.green(colorA);
			int f3 = FastColor.ARGB32.blue(colorA);
			int f4 = FastColor.ARGB32.alpha(colorB);
			int f5 = FastColor.ARGB32.red(colorB);
			int f6 = FastColor.ARGB32.green(colorB);
			int f7 = FastColor.ARGB32.blue(colorB);
			bufferbuilder.vertex(matrix, x2, y2, 0f).color(f1, f2, f3, f).endVertex();
			bufferbuilder.vertex(matrix, x2, y1, 0f).color(f1, f2, f3, f).endVertex();
			bufferbuilder.vertex(matrix, x1, y1, 0f).color(f5, f6, f7, f4).endVertex();
			bufferbuilder.vertex(matrix, x1, y2, 0f).color(f5, f6, f7, f4).endVertex();
			return this;
		}


	}
	public static class TextureTesselator extends TesselateHelper {
		private TextureTesselator() {
		};

		public TextureTesselator blit(Matrix4f matrixStack, int pX, int pY, float pUOffset, float pVOffset, int pWidth, int pHeight, int pTextureWidth, int pTextureHeight) {
			return blit(matrixStack, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight);
		}

		public TextureTesselator blit(Matrix4f matrixStack, int pX, int pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight) {
			return blit(matrixStack, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight);
		}

		public TextureTesselator blit(Matrix4f matrixStack, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, int pUWidth, int pVHeight, float pUOffset, float pVOffset, int pTextureWidth, int pTextureHeight) {
			return tesellate(matrixStack, pX1, pX2, pY1, pY2, pBlitOffset, (pUOffset + 0.0F) / (float) pTextureWidth, (pUOffset + (float) pUWidth) / (float) pTextureWidth,
				(pVOffset + 0.0F) / (float) pTextureHeight, (pVOffset + (float) pVHeight) / (float) pTextureHeight, 1);
		}

		public TextureTesselator tesellate(Matrix4f matrixStack, int x, int y, int width, int height, float uOffset, float vOffset,
			int uWidth, int vHeight, int textureWidth, int textureHeight, float opacity) {
			return tesellate(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
				textureHeight, opacity);
		}

		public TextureTesselator tesellate(Matrix4f matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth,
			int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, float opacity) {
			return tesellate(matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth,
				(uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight,
				(vOffset + vHeight) / textureHeight, opacity);
		}

		public TextureTesselator tesellate(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU,
			float maxU, float minV, float maxV, float opacity) {
			bufferbuilder.vertex(matrix, x1, y2, blitOffset).color(1, 1, 1, opacity).uv(minU, maxV).endVertex();
			bufferbuilder.vertex(matrix, x2, y2, blitOffset).color(1, 1, 1, opacity).uv(maxU, maxV).endVertex();
			bufferbuilder.vertex(matrix, x2, y1, blitOffset).color(1, 1, 1, opacity).uv(maxU, minV).endVertex();
			bufferbuilder.vertex(matrix, x1, y1, blitOffset).color(1, 1, 1, opacity).uv(minU, minV).endVertex();
			return this;
		}

		@Override
		public void beginBuffer() {
			RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		}

		public TextureTesselator tesellateColored(Matrix4f matrixStack, int x, int y, int width, int height, float uOffset,
			float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, int color) {

			return tesellateColored(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
				textureHeight, FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
				FastColor.ARGB32.blue(color) / 255f, FastColor.ARGB32.alpha(color) / 255f);
		}

		public TextureTesselator tesellateColored(Matrix4f matrixStack, int x, int y, int width, int height, float uOffset,
			float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, int color, float opacity) {

			return tesellateColored(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
				textureHeight, FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
				FastColor.ARGB32.blue(color) / 255f, opacity);
		}

		public TextureTesselator tesellateColored(Matrix4f matrixStack, int x1, int x2, int y1, int y2, int blitOffset,
			int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, float r,
			float g, float b, float opacity) {
			return tesellateColored(matrixStack, x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth,
				(uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight,
				(vOffset + vHeight) / textureHeight, r, g, b, opacity);
		}

		public TextureTesselator tesellateColored(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU,
			float maxU, float minV, float maxV, float r, float g, float b, float opacity) {
			bufferbuilder.vertex(matrix, x1, y2, blitOffset).color(r, g, b, opacity).uv(minU, maxV).endVertex();
			bufferbuilder.vertex(matrix, x2, y2, blitOffset).color(r, g, b, opacity).uv(maxU, maxV).endVertex();
			bufferbuilder.vertex(matrix, x2, y1, blitOffset).color(r, g, b, opacity).uv(maxU, minV).endVertex();
			bufferbuilder.vertex(matrix, x1, y1, blitOffset).color(r, g, b, opacity).uv(minU, minV).endVertex();
			return this;
		}

		public TextureTesselator tesellateNineSliced(Matrix4f graphics, int pTargetX, int pTargetY,
			int pTargetWidth, int pTargetHeight, int pCorner, int pSourceWidth, int pSourceHeight, int pSourceX,
			int pSourceY, int textureWidth, int textureHeight) {
			return tesellateNineSliced(graphics, pTargetX, pTargetY, pTargetWidth, pTargetHeight, pCorner, pCorner,
				pCorner, pCorner, pSourceWidth, pSourceHeight, pSourceX, pSourceY, textureWidth, textureHeight);
		}

		private static IntIterator slices(int pTarget, int pTotal) {
			int i = Mth.positiveCeilDiv(pTarget, pTotal);
			return new Divisor(pTarget, i);
		}

		public TextureTesselator tesellateRepeating(Matrix4f graphics, int pX, int pY, int pWidth, int pHeight, int pUOffset, int pVOffset, int pSourceWidth, int pSourceHeight, int textureWidth, int textureHeight) {
			int i = pX;

			int j;
			for (IntIterator intiterator = slices(pWidth, pSourceWidth); intiterator.hasNext(); i += j) {
				j = intiterator.nextInt();
				int k = (pSourceWidth - j) / 2;
				int l = pY;

				int i1;
				for (IntIterator intiterator1 = slices(pHeight, pSourceHeight); intiterator1.hasNext(); l += i1) {
					i1 = intiterator1.nextInt();
					int j1 = (pSourceHeight - i1) / 2;
					this.blit(graphics, i, l, pUOffset + k, pVOffset + j1, j, i1, textureWidth, textureHeight);
				}
			}
			return this;
		}

		public TextureTesselator tesellateNineSliced(Matrix4f graphics, int pTargetX, int pTargetY,
			int pTargetWidth, int pTargetHeight, int pCornerWidth, int pCornerHeight, int pEdgeWidth, int pEdgeHeight,
			int pSourceWidth, int pSourceHeight, int pSourceX, int pSourceY, int textureWidth, int textureHeight) {
			pCornerWidth = Math.min(pCornerWidth, pTargetWidth / 2);
			pEdgeWidth = Math.min(pEdgeWidth, pTargetWidth / 2);
			pCornerHeight = Math.min(pCornerHeight, pTargetHeight / 2);
			pEdgeHeight = Math.min(pEdgeHeight, pTargetHeight / 2);
			if (pTargetWidth == pSourceWidth && pTargetHeight == pSourceHeight) {
				blit(graphics, pTargetX, pTargetY, pSourceX, pSourceY, pTargetWidth, pTargetHeight,
					textureWidth, textureHeight);
			} else if (pTargetHeight == pSourceHeight) {
				blit(graphics, pTargetX, pTargetY, pSourceX, pSourceY, pCornerWidth, pTargetHeight,
					textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX + pCornerWidth, pTargetY,
					pTargetWidth - pEdgeWidth - pCornerWidth, pTargetHeight, pSourceX + pCornerWidth, pSourceY,
					pSourceWidth - pEdgeWidth - pCornerWidth, pSourceHeight, textureWidth, textureHeight);
				blit(graphics, pTargetX + pTargetWidth - pEdgeWidth, pTargetY,
					pSourceX + pSourceWidth - pEdgeWidth, pSourceY, pEdgeWidth, pTargetHeight, textureWidth,
					textureHeight);
			} else if (pTargetWidth == pSourceWidth) {
				blit(graphics, pTargetX, pTargetY, pSourceX, pSourceY, pTargetWidth, pCornerHeight,
					textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX, pTargetY + pCornerHeight, pTargetWidth,
					pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX, pSourceY + pCornerHeight, pSourceWidth,
					pSourceHeight - pEdgeHeight - pCornerHeight, textureWidth, textureHeight);
				blit(graphics, pTargetX, pTargetY + pTargetHeight - pEdgeHeight, pSourceX,
					pSourceY + pSourceHeight - pEdgeHeight, pTargetWidth, pEdgeHeight, textureWidth, textureHeight);
			} else {
				blit(graphics, pTargetX, pTargetY, pSourceX, pSourceY, pCornerWidth, pCornerHeight,
					textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX + pCornerWidth, pTargetY,
					pTargetWidth - pEdgeWidth - pCornerWidth, pCornerHeight, pSourceX + pCornerWidth, pSourceY,
					pSourceWidth - pEdgeWidth - pCornerWidth, pCornerHeight, textureWidth, textureHeight);
				blit(graphics, pTargetX + pTargetWidth - pEdgeWidth, pTargetY,
					pSourceX + pSourceWidth - pEdgeWidth, pSourceY, pEdgeWidth, pCornerHeight, textureWidth,
					textureHeight);
				blit(graphics, pTargetX, pTargetY + pTargetHeight - pEdgeHeight, pSourceX,
					pSourceY + pSourceHeight - pEdgeHeight, pCornerWidth, pEdgeHeight, textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX + pCornerWidth, pTargetY + pTargetHeight - pEdgeHeight,
					pTargetWidth - pEdgeWidth - pCornerWidth, pEdgeHeight, pSourceX + pCornerWidth,
					pSourceY + pSourceHeight - pEdgeHeight, pSourceWidth - pEdgeWidth - pCornerWidth, pEdgeHeight,
					textureWidth, textureHeight);
				blit(graphics, pTargetX + pTargetWidth - pEdgeWidth, pTargetY + pTargetHeight - pEdgeHeight,
					pSourceX + pSourceWidth - pEdgeWidth, pSourceY + pSourceHeight - pEdgeHeight, pEdgeWidth,
					pEdgeHeight, textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX, pTargetY + pCornerHeight, pCornerWidth,
					pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX, pSourceY + pCornerHeight, pCornerWidth,
					pSourceHeight - pEdgeHeight - pCornerHeight, textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX + pCornerWidth, pTargetY + pCornerHeight,
					pTargetWidth - pEdgeWidth - pCornerWidth, pTargetHeight - pEdgeHeight - pCornerHeight,
					pSourceX + pCornerWidth, pSourceY + pCornerHeight, pSourceWidth - pEdgeWidth - pCornerWidth,
					pSourceHeight - pEdgeHeight - pCornerHeight, textureWidth, textureHeight);
				tesellateRepeating(graphics, pTargetX + pTargetWidth - pEdgeWidth, pTargetY + pCornerHeight,
					pCornerWidth, pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX + pSourceWidth - pEdgeWidth,
					pSourceY + pCornerHeight, pEdgeWidth, pSourceHeight - pEdgeHeight - pCornerHeight, textureWidth,
					textureHeight);
			}
			return this;
		}
	}

}
