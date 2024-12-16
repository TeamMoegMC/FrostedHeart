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

package com.teammoeg.frostedheart.util.client;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Convenience functions for rendering
 */
public class FHGuiHelper {
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
		renderBuffer.vertex(mat, x, y, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
		renderBuffer.vertex(mat, x + w, y, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
		renderBuffer.vertex(mat, x, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
				.endVertex();
		renderBuffer.vertex(mat, x + w, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
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

	public static int drawSplitTexts(GuiGraphics graphics, FormattedText text, int x, int y, int color, int maxWidth,
			int lineSpace, boolean shadow) {
		List<FormattedCharSequence> texts = ClientUtils.font().split(text, maxWidth);
		for (int i = 0; i < texts.size(); i++) {
			graphics.drawString(ClientUtils.font(), texts.get(i), x, y + i * lineSpace, color, shadow);
		}
		return texts.size();
	}

	/**
	 * 绘制一个不完整的圆
	 * 
	 * @param radius  半径
	 * @param partial 圆的完整度 {@code 0.0 ~ 1.0}
	 */
	public static void drawPartialCircle(int x, int y, double radius, float partial, int color) {

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

		bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.vertex(x, y, 0).endVertex();
		for (int i = -180; i <= 360 * partial - 180; i++) { // 为了让圆顺时针绘制
			double angle = i * Math.PI / 180;
			double x2 = x + Math.sin(-angle) * radius;
			double y2 = y + Math.cos(angle) * radius;
			bufferBuilder.vertex(x2, y2, 0).color(color).endVertex();
		}

		tessellator.end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	/**
	 * 绘制一个多边形
	 * 
	 * @param radius 半径
	 * @param sides  多边形的边数，大部分情况下 50 已经够圆了
	 */
	public static void drawPolygon(int x, int y, double radius, int sides, int color) {
		sides = Mth.clamp(sides, 3, 360);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

		bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.vertex(x, y, 0).endVertex();
		for (int i = 0; i <= sides; i++) {
			double angle = i * (360F / sides) * Math.PI / 180;
			double x2 = x + Math.sin(angle) * radius;
			double y2 = y + Math.cos(angle) * radius;
			bufferBuilder.vertex(x2, y2, 0).color(color).endVertex();
		}

		tessellator.end();

		RenderSystem.disableBlend();
	}
}
