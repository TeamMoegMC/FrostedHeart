/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.chorda.util;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.chorda.widget.IconButton;
import com.teammoeg.chorda.util.client.ClientUtils;
import com.teammoeg.chorda.util.client.ColorHelper;
import com.teammoeg.chorda.util.client.Point;
import net.minecraft.client.gui.Font;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;

/**
 * Convenience functions for gui rendering
 */
public class CGuiHelper {
	public static final Function<Direction, Quaternionf> DIR_TO_FACING = Util
			.memoize(dir -> new Quaternionf().rotateAxis(-(float) (dir.toYRot() / 180 * Math.PI), 0, 1, 0));

	// hack to access render state protected members
	public static class RenderStateAccess extends RenderStateShard {
		public static RenderType.CompositeState getLineState(double width) {
			return RenderType.CompositeState.builder()
					.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(width)))// width
					.setLayeringState(VIEW_OFFSET_Z_LAYERING).setOutputState(MAIN_TARGET)
					.setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(true);
		}

		public static RenderType.CompositeState getRectState() {
			return RenderType.CompositeState.builder()
					// width
					.setLayeringState(VIEW_OFFSET_Z_LAYERING).setOutputState(MAIN_TARGET)
					.setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(true);
		}

		public RenderStateAccess(String p_i225973_1_, Runnable p_i225973_2_, Runnable p_i225973_3_) {
			super(p_i225973_1_, p_i225973_2_, p_i225973_3_);
		}

		public static RenderType createTempType(ResourceLocation texture) {
			RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
					.setShaderState(RenderType.RENDERTYPE_TEXT_SHADER)
					.setTextureState(
							new TextureStateShard(texture, ForgeRenderTypes.enableTextTextureLinearFiltering, false))
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(false);
			return RenderType.create("frostedheart_prerendered", DefaultVertexFormat.POSITION_COLOR_TEX,
					VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
		}

		public static final RenderType BOLD_LINE_TYPE = RenderType.create("fh_line_bold",
				DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES, 256, false, false,
				RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
						.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(4)))
						// .setLayeringState(VIEW_OFFSET_Z_LAYERING)
						// .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setOutputState(MAIN_TARGET)
						// .setWriteMaskState(COLOR_DEPTH_WRITE)
						// .setCullState(NO_CULL)
						.createCompositeState(false));
	}

	public static void drawItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int zindex, float scaleX,
			float scaleY, boolean drawDecorations, @Nullable String countReplacement) {
		guiGraphics.pose().pushPose();
		guiGraphics.pose().translate(x, y, zindex+150);
		guiGraphics.pose().scale(scaleX, scaleY, scaleX);
		/*
		 * guiGraphics.renderItem(stack, 0, 0, 0,300);
		 */
		// if(stack.getItem() instanceof BlockItem bl) {//A hack for item render
		// lighting.
		// BlockState bs=bl.getBlock().defaultBlockState();
		// BlockRenderDispatcher rd = Minecraft.getInstance().getBlockRenderer();
		// rd.renderSingleBlock(bs, guiGraphics.pose(), guiGraphics.bufferSource(),
		// LightTexture.FULL_BRIGHT,
		// OverlayTexture.NO_OVERLAY,ModelData.builder().build(),RenderType.cutout());
		// }else {
		// guiGraphics.renderItem( stack, 0, 0,0,-50);
		// }c
		Matrix4f matrix4f = null;
		if (!stack.isEmpty()) {
			BakedModel bakedmodel = ClientUtils.mc().getItemRenderer().getModel(stack, ClientUtils.mc().level,
					ClientUtils.mc().player, 0);
			boolean flag = !bakedmodel.usesBlockLight();
			if(!flag) {
				matrix4f=new Matrix4f(guiGraphics.pose().last().pose()).rotationYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((-(float)Math.PI / 8F), 2.3561945F, 0.0F);
			}
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(8f, 8f,0f);

			guiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
			guiGraphics.pose().scale(16.0F, 16.0F, 16.0F);
			
			if (flag) {
				Lighting.setupForFlatItems();
			}else {
			       Lighting.setupLevel(matrix4f);
			}

			ClientUtils.mc().getItemRenderer().render(stack, ItemDisplayContext.GUI, false, guiGraphics.pose(),
					guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
			guiGraphics.flush();
			Lighting.setupFor3DItems();
			guiGraphics.pose().popPose();
		}
		if(drawDecorations)
			guiGraphics.renderItemDecorations(ClientUtils.mc().font, stack, 0, 0, countReplacement);
		guiGraphics.pose().popPose();
	}

	public static void drawTextShadow(GuiGraphics guiGraphics, Component text, Point point, int color) {
		guiGraphics.drawString(Minecraft.getInstance().font, text, point.getX(), point.getY(), color);
	}

	public static void drawTextShadow(GuiGraphics guiGraphics, String text, Point point, int color) {
		guiGraphics.drawString(Minecraft.getInstance().font, text, point.getX(), point.getY(), color);
	}
	// TODO fix line drawing
	/*
	 * private static void drawVertexLine(Matrix4f mat, VertexConsumer renderBuffer,
	 * Color4I color, int startX, int startY,
	 * int endX, int endY,float z) {
	 * //RenderSystem.disableTexture();
	 * //RenderSystem.enableColorLogicOp();
	 * //RenderSystem.colorMask(false, false, false, false);
	 * renderBuffer.vertex(mat, startX, startY, z).color(color.redi(),
	 * color.greeni(), color.bluei(), color.alphai())
	 * .endVertex();
	 * renderBuffer.vertex(mat, endX, endY,z).color(color.redi(), color.greeni(),
	 * color.bluei(), color.alphai())
	 * .endVertex();
	 * //RenderSystem.enableTexture();
	 * }
	 * private static void drawVertexLine2(Matrix4f mat, VertexConsumer
	 * renderBuffer, Color4I color, int startX, int startY,
	 * int endX, int endY,float z) {
	 * //RenderSystem.disableTexture();
	 * //RenderSystem.enableColorMaterial();
	 * //RenderSystem.colorMask(false, false, false, false);
	 * 
	 * renderBuffer.vertex(mat, startX, 0, z).color(color.redi(), color.greeni(),
	 * color.bluei(), color.alphai()).endVertex();
	 * renderBuffer.vertex(mat, endX, 0, z).color(color.redi(), color.greeni(),
	 * color.bluei(), color.alphai()).endVertex();
	 * renderBuffer.vertex(mat, 0, startY, z).color(color.redi(), color.greeni(),
	 * color.bluei(), color.alphai()).endVertex();
	 * renderBuffer.vertex(mat, 0, endY , z).color(color.redi(), color.greeni(),
	 * color.bluei(), color.alphai()).endVertex();
	 * //RenderSystem.enableTexture();
	 * }
	 * // draw a line from start to end by color, ABSOLUTE POSITION
	 * public static void drawLine(PoseStack matrixStack, Color4I color, int startX,
	 * int startY, int endX, int endY,float z) {
	 * Tesselator t=Tesselator.getInstance();
	 * 
	 * BufferBuilder vertexBuilderLines = t.getBuilder();
	 * vertexBuilderLines.begin(VertexFormat.Mode.LINES,
	 * DefaultVertexFormat.POSITION_COLOR);
	 * drawVertexLine(matrixStack.last().pose(), vertexBuilderLines, color, startX,
	 * startY, endX, endY,z);
	 * t.end();
	 * }
	 */

	// draw a line from start to end by color, ABSOLUTE POSITION
	/*
	 * public static void drawLine(GuiGraphics graphics, Color4I color, int startX,
	 * int startY, int endX, int endY) {
	 * 
	 * VertexConsumer vertexBuilderLines = graphics.bufferSource()
	 * .getBuffer(RenderStateAccess.BOLD_LINE_TYPE);
	 * drawVertexLine(graphics.pose().last().pose(), vertexBuilderLines, color,
	 * startX, startY, endX, endY,0f);
	 * 
	 * }
	 */

	private static void drawRect(Matrix4f mat, BufferBuilder renderBuffer, int x, int y, int w, int h,int color) {
		renderBuffer.vertex(mat, x,     y,     0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
		renderBuffer.vertex(mat, x + w, y,     0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
		renderBuffer.vertex(mat, x + w, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
		renderBuffer.vertex(mat, x    , y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
	}

	private static void fillGradient(Matrix4f matrix, BufferBuilder builder, int x1, int y1, int x2, int y2, int colorB,
			int colorA) {
		float f = (colorA >> 24 & 255) / 255.0F;
		float f1 = (colorA >> 16 & 255) / 255.0F;
		float f2 = (colorA >> 8 & 255) / 255.0F;
		float f3 = (colorA & 255) / 255.0F;
		float f4 = (colorB >> 24 & 255) / 255.0F;
		float f5 = (colorB >> 16 & 255) / 255.0F;
		float f6 = (colorB >> 8 & 255) / 255.0F;
		float f7 = (colorB & 255) / 255.0F;
		builder.vertex(matrix, x2, y2, 0f).color(f1, f2, f3, f).endVertex();
		builder.vertex(matrix, x2, y1, 0f).color(f1, f2, f3, f).endVertex();
		builder.vertex(matrix, x1, y1, 0f).color(f5, f6, f7, f4).endVertex();
		builder.vertex(matrix, x1, y2, 0f).color(f5, f6, f7, f4).endVertex();
	}

	// draw a rectangle
	public static void fillGradient(PoseStack matrixStack, int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		fillGradient(matrixStack.last().pose(), bufferbuilder, x1, y1, x2, y2, colorFrom, colorTo);
		tessellator.end();
		RenderSystem.disableBlend();
	}
	// draw a rectangle
	public static void fillRect(PoseStack matrixStack, int x1, int y1, int w, int h, int color) {
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		drawRect(matrixStack.last().pose(), bufferbuilder, x1, y1, w, h, color);
		tessellator.end();
		RenderSystem.disableBlend();
	}

	public static void blit(PoseStack matrixStack, int x, int y, int width, int height, float uOffset, float vOffset,
			int uWidth, int vHeight, int textureWidth, int textureHeight, float opacity) {
		innerBlit(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
				textureHeight, opacity);
	}

	public static void innerBlit(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset, int uWidth,
			int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, float opacity) {
		innerBlit(matrixStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth,
				(uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight,
				(vOffset + vHeight) / textureHeight, opacity);
	}

	public static void innerBlit(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU,
			float maxU, float minV, float maxV, float opacity) {
		// RenderSystem.enableAlphaTest();

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.enableBlend();

		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		bufferbuilder.vertex(matrix, x1, y2, blitOffset).color(1, 1, 1, opacity).uv(minU, maxV).endVertex();
		bufferbuilder.vertex(matrix, x2, y2, blitOffset).color(1, 1, 1, opacity).uv(maxU, maxV).endVertex();
		bufferbuilder.vertex(matrix, x2, y1, blitOffset).color(1, 1, 1, opacity).uv(maxU, minV).endVertex();
		bufferbuilder.vertex(matrix, x1, y1, blitOffset).color(1, 1, 1, opacity).uv(minU, minV).endVertex();

		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.disableBlend();
	}

	public static void blitColored(PoseStack matrixStack, int x, int y, int width, int height, float uOffset,
			float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, int color) {

		innerBlitColored(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
				textureHeight, FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
				FastColor.ARGB32.blue(color) / 255f, FastColor.ARGB32.alpha(color) / 255f);
	}

	public static void blitColored(PoseStack matrixStack, int x, int y, int width, int height, float uOffset,
			float vOffset, int uWidth, int vHeight, int textureWidth, int textureHeight, int color, float opacity) {

		innerBlitColored(matrixStack, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
				textureHeight, FastColor.ARGB32.red(color) / 255f, FastColor.ARGB32.green(color) / 255f,
				FastColor.ARGB32.blue(color) / 255f, opacity);
	}

	public static void innerBlitColored(PoseStack matrixStack, int x1, int x2, int y1, int y2, int blitOffset,
			int uWidth, int vHeight, float uOffset, float vOffset, int textureWidth, int textureHeight, float r,
			float g, float b, float opacity) {
		innerBlitColored(matrixStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth,
				(uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight,
				(vOffset + vHeight) / textureHeight, r, g, b, opacity);
	}

	public static void innerBlitColored(Matrix4f matrix, int x1, int x2, int y1, int y2, int blitOffset, float minU,
			float maxU, float minV, float maxV, float r, float g, float b, float opacity) {
		// RenderSystem.enableAlphaTest();

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.enableBlend();

		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		bufferbuilder.vertex(matrix, x1, y2, blitOffset).color(r, g, b, opacity).uv(minU, maxV).endVertex();
		bufferbuilder.vertex(matrix, x2, y2, blitOffset).color(r, g, b, opacity).uv(maxU, maxV).endVertex();
		bufferbuilder.vertex(matrix, x2, y1, blitOffset).color(r, g, b, opacity).uv(maxU, minV).endVertex();
		bufferbuilder.vertex(matrix, x1, y1, blitOffset).color(r, g, b, opacity).uv(minU, minV).endVertex();

		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.disableBlend();
	}

	public static void bindTexture(ResourceLocation showingImage) {
		RenderSystem.setShaderTexture(0, showingImage);

	}

	/**
	 * 换行文本并渲染
	 * @param maxWidth 单行最大宽度
	 * @param lineSpace 行间距
	 * @param shadow 文本阴影
	 * @return 换行后的行数
	 */
	public static int drawWordWarp(GuiGraphics graphics, Font font, FormattedText text, int x, int y, int color,
								   int maxWidth, int lineSpace, boolean shadow, boolean background)
	{
		List<FormattedCharSequence> texts = font.split(text, maxWidth);
		drawStrings(graphics, font, texts, x, y, color, lineSpace, shadow, background);
		return texts.size();
	}

	/**
	 * 渲染列表中的所有文本
	 * @param lineSpace 行间距
	 * @param shadow 文本阴影
	 */
	public static void drawStrings(GuiGraphics graphics, Font font, List<?> texts, int x, int y,
                                   int color, int lineSpace, boolean shadow, boolean background)
	{
		for (int i = 0; i < texts.size(); i++) {
			Object obj = texts.get(i);
			if (obj instanceof FormattedCharSequence formatted) {
				if (background) graphics.fill(x, y-1 + i * lineSpace, x + font.width(formatted), y-1 + (i+1) * lineSpace, ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F));
				graphics.drawString(font, formatted, x, y + i * lineSpace, color, shadow);

			} else if (obj instanceof Component component) {
				if (background) graphics.fill(x, y-1 + i * lineSpace, x + font.width(component), y-1 + (i+1) * lineSpace, ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F));
				graphics.drawString(font, component, x, y + i * lineSpace, color, shadow);

			} else {
				if (background) graphics.fill(x, y-1 + i * lineSpace, x + font.width(obj.toString()), y-1 + (i+1) * lineSpace, ColorHelper.setAlpha(ColorHelper.BLACK, 0.5F));
				graphics.drawString(font, Components.str(obj.toString()), x, y + i * lineSpace, color, shadow);
			}
		}
	}

	/**
	 * 渲染一个图标
	 * @param icon {@link IconButton.Icon}
	 * @param color 图标的颜色
	 */
	public static void renderIcon(PoseStack pose, IconButton.Icon icon, int x, int y, int color) {
		CGuiHelper.bindTexture(IconButton.ICON_LOCATION);
		CGuiHelper.blitColored(pose, x, y, icon.size.width, icon.size.height, icon.x, icon.y, icon.size.width, icon.size.height, IconButton.TEXTURE_WIDTH, IconButton.TEXTURE_HEIGHT, color);
	}

}
