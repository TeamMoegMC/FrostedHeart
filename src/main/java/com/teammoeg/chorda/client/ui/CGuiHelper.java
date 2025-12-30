/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.client.ui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.Layer;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.archive.Alignment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.util.Size2i;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Convenience functions for gui rendering
 */
public class CGuiHelper {
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

		public static final RenderType BOLD_LINE_TYPE = RenderType.create("line_bold",
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
		guiGraphics.pose().translate(x, y, zindex + 150);
		guiGraphics.pose().scale(scaleX, scaleY, scaleX); 
		if (!stack.isEmpty()) {
			BakedModel bakedmodel = ClientUtils.getMc().getItemRenderer().getModel(stack, ClientUtils.getMc().level,
				ClientUtils.getMc().player, 0);
			
			boolean flag = !bakedmodel.usesBlockLight();
			Matrix4f matrix4f = null;
			if (!flag) {
				matrix4f = new Matrix4f(guiGraphics.pose().last().pose()).rotationYXZ(1.0821041F, 3.2375858F, 0.0F).rotateYXZ((-(float) Math.PI / 8F), 2.3561945F, 0.0F);
			}
			guiGraphics.pose().pushPose();
			guiGraphics.pose().translate(8f, 8f, 0f);

			guiGraphics.pose().mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
			guiGraphics.pose().scale(16.0F, 16.0F, 16.0F);

			if (flag) {
				Lighting.setupForFlatItems();
			} else {
				Lighting.setupLevel(matrix4f);
			}

			ClientUtils.getMc().getItemRenderer().render(stack, ItemDisplayContext.GUI, false, guiGraphics.pose(),
				guiGraphics.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
			guiGraphics.flush();
			Lighting.setupFor3DItems();
			guiGraphics.pose().popPose();
		}
		if (drawDecorations)
			guiGraphics.renderItemDecorations(ClientUtils.getMc().font, stack, 0, 0, countReplacement);
		guiGraphics.pose().popPose();
	}

	public static void drawTextShadow(GuiGraphics guiGraphics, Component text, Point point, int color) {
		guiGraphics.drawString(Minecraft.getInstance().font, text, point.getX(), point.getY(), color);
	}

	public static void drawTextShadow(GuiGraphics guiGraphics, String text, Point point, int color) {
		guiGraphics.drawString(Minecraft.getInstance().font, text, point.getX(), point.getY(), color);
	}
	// TODO fix line drawing

	private static void drawVertexLine(Matrix4f mat, VertexConsumer renderBuffer, int color, int startX, int startY,
		int endX, int endY, float z) {
		// RenderSystem.disableTexture();
		// RenderSystem.enableColorLogicOp();
		// RenderSystem.colorMask(false, false, false, false);
		renderBuffer.vertex(mat, startX, startY, z).color(color).endVertex();
		/*
		 * renderBuffer.vertex(mat, startX, startY, z).color(color.redi(),
		 * color.greeni(), color.bluei(), color.alphai()) .endVertex();
		 */
		renderBuffer.vertex(mat, endX, endY, z).color(color).endVertex();
		/*
		 * renderBuffer.vertex(mat, endX, endY, z).color(color.redi(), color.greeni(),
		 * color.bluei(), color.alphai()) .endVertex();
		 */
		// RenderSystem.enableTexture();
	}

	private static void drawVertexLine2(Matrix4f mat, VertexConsumer renderBuffer, int color, int startX, int startY,
		int endX, int endY, float z) {
		// RenderSystem.disableTexture();
		// RenderSystem.enableColorMaterial();
		// RenderSystem.colorMask(false, false, false, false);

		renderBuffer.vertex(mat, startX, 0, z).color(color).endVertex();
		renderBuffer.vertex(mat, endX, 0, z).color(color).endVertex();
		renderBuffer.vertex(mat, 0, startY, z).color(color).endVertex();
		renderBuffer.vertex(mat, 0, endY, z).color(color).endVertex();
		// RenderSystem.enableTexture();
	}

	// draw a line from start to end by color, ABSOLUTE POSITION

	public static void drawLine(PoseStack matrixStack, int color, int startX,
		int startY, int endX, int endY, float z) {
		Tesselator t = Tesselator.getInstance();

		BufferBuilder vertexBuilderLines = t.getBuilder();
		vertexBuilderLines.begin(VertexFormat.Mode.LINES,
			DefaultVertexFormat.POSITION_COLOR);
		drawVertexLine(matrixStack.last().pose(), vertexBuilderLines, color, startX,
			startY, endX, endY, z);
		t.end();
	}

	private static class ShaderSetter implements Supplier<ShaderInstance> {
		ShaderInstance inst;

		@Override
		public ShaderInstance get() {
			return inst;
		}

	}

	public static ThreadLocal<ShaderSetter> shaderSetterCache = ThreadLocal.withInitial(() -> new ShaderSetter());

	// draw a line from start to end by color, ABSOLUTE POSITION
	// TODO: THE LINES IS NOT SHOWING
	public static void drawLine(GuiGraphics graphics, int color, int startX,
		int startY, int endX, int endY) {
		ShaderSetter ss = shaderSetterCache.get();
		ss.inst = RenderSystem.getShader();

		VertexConsumer vertexBuilderLines = graphics.bufferSource()
			.getBuffer(RenderStateAccess.BOLD_LINE_TYPE);
		drawVertexLine(graphics.pose().last().pose(), vertexBuilderLines, color,
			startX, startY, endX, endY, 0f);
		RenderSystem.setShader(ss);

	}

	public static void drawRect(GuiGraphics graphics, Rect rect, int color) {
		graphics.fill(rect.getX(), rect.getY(), rect.getX2(), rect.getY2(), color);
	}

	private static void drawRect(Matrix4f mat, BufferBuilder renderBuffer, int x, int y, int w, int h, int color) {
		renderBuffer.vertex(mat, x, y, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
			.endVertex();
		renderBuffer.vertex(mat, x + w, y, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
			.endVertex();
		renderBuffer.vertex(mat, x + w, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
			.endVertex();
		renderBuffer.vertex(mat, x, y + h, 0F).color(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), FastColor.ARGB32.alpha(color))
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

	public record LineDrawingContext(int lineSize, int maxWidth) {}
	public static Pair<List<FormattedCharSequence>, LineDrawingContext> split(Component text, Font font, int w) {
		var lines = font.split(text, w);
		int maxW = 0;
		for (FormattedCharSequence line : lines) {
			maxW = Math.max(font.width(line), maxW);
		}
		return new Pair<>(lines, new LineDrawingContext(lines.size(), maxW));
	}

	/**
	 * 渲染列表中的所有文本
	 *
	 * @param texts		 支持 {@link FormattedCharSequence} {@link Component} {@link Object}
	 * @param lineSpace  行间距
	 * @param shadow     是否添加文本阴影
	 * @param background 是否渲染背景(参考F3显示的文本)
	 */
	public static void drawStringLines(GuiGraphics graphics, Font font, List<?> texts, int x, int y,
									   int color, int lineSpace, boolean shadow, boolean background) {
		drawStringLines(graphics, font, texts, x, y, color, lineSpace, shadow, background, Alignment.LEFT);
	}

	/**
	 * 渲染列表中的所有文本
	 *
	 * @param texts		 支持 {@link FormattedCharSequence} {@link Component} {@link Object}
	 * @param lineSpace  行间距
	 * @param shadow     是否添加文本阴影
	 * @param background 是否渲染背景(参考F3显示的文本)
	 * @param alignment <p>
	 * 		{@link Alignment#LEFT} 在给予的坐标右侧绘制左对齐文本<p>
	 * 		{@link Alignment#CENTER} 以给予的坐标为中心绘制居中对齐文本<p>
	 *      {@link Alignment#RIGHT} 在给予的坐标左侧绘制右对齐文本<p>
	 */
	public static void drawStringLines(GuiGraphics graphics, Font font, List<?> texts, int x, int y,
									   int color, int lineSpace, boolean shadow, boolean background, Alignment alignment) {
		if (texts.isEmpty()) return;

		int backgroundColor = background ? Colors.setAlpha(Colors.BLACK, 0.5F) : 0;

		int lineOffset = 0;
		for (Object text : texts) {
			if (text == null) {
				lineOffset += (font.lineHeight + lineSpace);
				continue;
			}

			int textWidth;
			if (text instanceof FormattedCharSequence formatted) {
				textWidth = font.width(formatted);
			} else if (text instanceof Component component) {
				textWidth = font.width(component);
			} else {
				textWidth = font.width(text.toString());
			}
			if (textWidth <= 0) {
				lineOffset += (font.lineHeight + lineSpace);
				continue;
			}

			int drawX = switch (alignment) {
				case LEFT -> x;
				case CENTER -> x - textWidth / 2;
				case RIGHT -> x - textWidth;
			};
			int drawY = y + lineOffset;

			// 背景
			if (background) {
				int bgX1 = drawX - 2;
				int bgY1 = drawY - 1;
				int bgX2 = drawX + textWidth + 2;
				int bgY2 = bgY1 + (font.lineHeight + lineSpace);
				graphics.fill(bgX1, bgY1, bgX2, bgY2, backgroundColor);
			}

			// 文本
			if (text instanceof FormattedCharSequence formatted) {
				graphics.drawString(font, formatted, drawX, drawY, color, shadow);
			} else if (text instanceof Component component) {
				graphics.drawString(font, component, drawX, drawY, color, shadow);
			} else {
				graphics.drawString(font, text.toString(), drawX, drawY, color, shadow);
			}

			lineOffset += (font.lineHeight + lineSpace);
		}
	}

	/**
	 * 在指定的区域内渲染文本
	 *
	 * @param lineSpace  行间距
	 * @param shadow     是否添加文本阴影
	 * @param background 是否渲染背景(参考F3显示的文本)
	 * @param alignment <p>
	 * 		{@link Alignment#LEFT} 在区域左侧绘制左对齐文本<p>
	 * 		{@link Alignment#CENTER} 在区域中心绘制居中对齐文本<p>
	 *      {@link Alignment#RIGHT} 在区域右侧绘制右对齐文本<p>
	 */
	public static void drawStringInBound(GuiGraphics graphics, Font font, Component text, int x, int y, int width,
										 int color, int lineSpace, boolean shadow, boolean background, Alignment alignment){

		int backgroundColor = background ? Colors.setAlpha(Colors.BLACK, 0.5F) : 0;
		var split = font.split(text, width);

		int lineOffset = 0;
		for (var line : split) {
			int textWidth = font.width(line);
			int drawX = switch (alignment) {
				case LEFT -> x;
				case CENTER -> x + (width - textWidth) / 2;
				case RIGHT -> x + (width - textWidth);
			};
			int drawY = y + lineOffset;

			// 背景
			if (background) {
				int bgX1 = drawX - 2;
				int bgY1 = drawY - 1;
				int bgX2 = drawX + textWidth + 2;
				int bgY2 = bgY1 + (font.lineHeight + lineSpace);
				graphics.fill(bgX1, bgY1, bgX2, bgY2, backgroundColor);
			}
			// 文本
			graphics.drawString(font, line, drawX, drawY, color, shadow);

			lineOffset += (font.lineHeight + lineSpace);
		}
	}

	/**
	 * 在指定的区域内渲染文本
	 *
	 * @param texts		 支持 {@link FormattedCharSequence} {@link Component} {@link Object}
	 * @param lineSpace  行间距
	 * @param shadow     是否添加文本阴影
	 * @param backgroundColor 背景颜色，传入{@code 0}禁用
	 * @param alignment <p>
	 * 		{@link Alignment#LEFT} 在区域左侧绘制左对齐文本<p>
	 * 		{@link Alignment#CENTER} 在区域中心绘制居中对齐文本<p>
	 *      {@link Alignment#RIGHT} 在区域右侧绘制右对齐文本<p>
	 */
	public static void drawStringLinesInBound(GuiGraphics graphics, Font font, List<?> texts, int x, int y, int maxWidth,
										 int color, int lineSpace, boolean shadow, int backgroundColor, Alignment alignment) {
		if (texts.isEmpty()) return;

		int lineOffset = 0;
		for (Object text : texts) {
			int textWidth;
			if (text instanceof FormattedCharSequence formatted) {
				textWidth = font.width(formatted);
			} else if (text instanceof Component component) {
				textWidth = font.width(component);
			} else {
				textWidth = font.width(text.toString());
			}

			int drawX = switch (alignment) {
				case LEFT -> x;
				case CENTER -> x + (maxWidth - textWidth) / 2;
				case RIGHT -> x + (maxWidth - textWidth);
			};
			int drawY = y + lineOffset;

			// 背景
			if (backgroundColor != 0) {
				int bgX1 = drawX - 2;
				int bgY1 = drawY - 1;
				int bgX2 = drawX + textWidth + 2;
				int bgY2 = bgY1 + (font.lineHeight + lineSpace);
				graphics.fill(bgX1, bgY1, bgX2, bgY2, backgroundColor);
			}
			// 文本
			if (text instanceof FormattedCharSequence formatted) {
				graphics.drawString(font, formatted, drawX, drawY, color, shadow);
			} else if (text instanceof Component component) {
				graphics.drawString(font, component, drawX, drawY, color, shadow);
			} else {
				graphics.drawString(font, text.toString(), drawX, drawY, color, shadow);
			}

			lineOffset += (font.lineHeight + lineSpace);
		}
	}

	public static void resetGuiDrawing() {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(770, 771);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.enableDepthTest();

	}
	private static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");

	public static void drawLayerBackground(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.fill(x, y, x+w, y+h, 0xFF8B8B8B);
	}
	public static void drawUIBackground(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, w, h, 4, 32, 32, 82, 208);
	}
	public static void drawUIBackgroundWithSearch(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, w, h, 24,28, 0, 0, 148, 167);
	}
	public static void drawUISlot(GuiGraphics graphics,int x,int y,int w,int h) {
		graphics.blitNineSliced(RECIPE_BOOK_LOCATION, x, y, w, h, 2, 24, 24, 29, 206);
	}
	public static int getFluidColor(FluidStack fluid) {
		if(fluid.isEmpty()||fluid.getAmount()==0)return 0xffffffff;
		IClientFluidTypeExtensions ext=IClientFluidTypeExtensions.of(fluid.getFluid());
		int tint=ext.getTintColor(fluid);
		if((tint&0xffffff)!=0xffffff) {
			return tint;
		}
		TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
			.getSprite(ext.getStillTexture(fluid));
		int abgr=sprite.getPixelRGBA(0, 0, 0);
		return FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr),FastColor.ABGR32.red(abgr),FastColor.ABGR32.green(abgr),FastColor.ABGR32.blue(abgr));
	}
	private static Map<Fluid,Integer> fluidColor=new HashMap<>();
	public static int getColorReadingTexture(FluidStack fluid) {
		Integer color=fluidColor.get(fluid.getFluid());
		if(color!=null)return color;
		IClientFluidTypeExtensions ext=IClientFluidTypeExtensions.of(fluid.getFluid());
		TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
			.getSprite(ext.getStillTexture(fluid));
		int realcolor=getColorReadingTexture(sprite);
		return realcolor;
	}
	public static int getColorReadingTexture(TextureAtlasSprite sprite) {
		return Minecraft.getInstance().getResourceManager().getResource(sprite.atlasLocation()).map(t->{
			try(InputStream i=t.open()){
				return ImageIO.read(i).getRGB(sprite.getX(), sprite.getY());
			} catch (IOException e) {
				e.printStackTrace();
			}return 0xffffffff;
			
		}).orElse(0xffffffff);
	}
	public static void blitNineSliced(GuiGraphics graphics,ResourceLocation pAtlasLocation, int pTargetX, int pTargetY, int pTargetWidth, int pTargetHeight, int pCorner, int pSourceWidth, int pSourceHeight, int pSourceX, int pSourceY,int textureWidth,int textureHeight) {
		blitNineSliced(graphics,pAtlasLocation,pTargetX,pTargetY,pTargetWidth,pTargetHeight,pCorner,pCorner,pCorner,pCorner,pSourceWidth,pSourceHeight,pSourceX,pSourceY,textureWidth,textureHeight);
	}
    public static void blitNineSliced(GuiGraphics graphics,ResourceLocation pAtlasLocation, int pTargetX, int pTargetY, int pTargetWidth, int pTargetHeight, int pCornerWidth, int pCornerHeight, int pEdgeWidth, int pEdgeHeight, int pSourceWidth, int pSourceHeight, int pSourceX, int pSourceY,int textureWidth,int textureHeight) {
        pCornerWidth = Math.min(pCornerWidth, pTargetWidth / 2);
        pEdgeWidth = Math.min(pEdgeWidth, pTargetWidth / 2);
        pCornerHeight = Math.min(pCornerHeight, pTargetHeight / 2);
        pEdgeHeight = Math.min(pEdgeHeight, pTargetHeight / 2);
        if (pTargetWidth == pSourceWidth && pTargetHeight == pSourceHeight) {
        	graphics.blit(pAtlasLocation, pTargetX, pTargetY, pSourceX, pSourceY, pTargetWidth, pTargetHeight,textureWidth,textureHeight);
        } else if (pTargetHeight == pSourceHeight) {
           graphics.blit(pAtlasLocation, pTargetX, pTargetY, pSourceX, pSourceY, pCornerWidth, pTargetHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX + pCornerWidth, pTargetY, pTargetWidth - pEdgeWidth - pCornerWidth, pTargetHeight, pSourceX + pCornerWidth, pSourceY, pSourceWidth - pEdgeWidth - pCornerWidth, pSourceHeight,textureWidth,textureHeight);
           graphics.blit(pAtlasLocation, pTargetX + pTargetWidth - pEdgeWidth, pTargetY, pSourceX + pSourceWidth - pEdgeWidth, pSourceY, pEdgeWidth, pTargetHeight,textureWidth,textureHeight);
        } else if (pTargetWidth == pSourceWidth) {
           graphics.blit(pAtlasLocation, pTargetX, pTargetY, pSourceX, pSourceY, pTargetWidth, pCornerHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX, pTargetY + pCornerHeight, pTargetWidth, pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX, pSourceY + pCornerHeight, pSourceWidth, pSourceHeight - pEdgeHeight - pCornerHeight,textureWidth,textureHeight);
           graphics.blit(pAtlasLocation, pTargetX, pTargetY + pTargetHeight - pEdgeHeight, pSourceX, pSourceY + pSourceHeight - pEdgeHeight, pTargetWidth, pEdgeHeight,textureWidth,textureHeight);
        } else {
           graphics.blit(pAtlasLocation, pTargetX, pTargetY, pSourceX, pSourceY, pCornerWidth, pCornerHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX + pCornerWidth, pTargetY, pTargetWidth - pEdgeWidth - pCornerWidth, pCornerHeight, pSourceX + pCornerWidth, pSourceY, pSourceWidth - pEdgeWidth - pCornerWidth, pCornerHeight,textureWidth,textureHeight);
           graphics.blit(pAtlasLocation, pTargetX + pTargetWidth - pEdgeWidth, pTargetY, pSourceX + pSourceWidth - pEdgeWidth, pSourceY, pEdgeWidth, pCornerHeight,textureWidth,textureHeight);
           graphics.blit(pAtlasLocation, pTargetX, pTargetY + pTargetHeight - pEdgeHeight, pSourceX, pSourceY + pSourceHeight - pEdgeHeight, pCornerWidth, pEdgeHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX + pCornerWidth, pTargetY + pTargetHeight - pEdgeHeight, pTargetWidth - pEdgeWidth - pCornerWidth, pEdgeHeight, pSourceX + pCornerWidth, pSourceY + pSourceHeight - pEdgeHeight, pSourceWidth - pEdgeWidth - pCornerWidth, pEdgeHeight,textureWidth,textureHeight);
           graphics.blit(pAtlasLocation, pTargetX + pTargetWidth - pEdgeWidth, pTargetY + pTargetHeight - pEdgeHeight, pSourceX + pSourceWidth - pEdgeWidth, pSourceY + pSourceHeight - pEdgeHeight, pEdgeWidth, pEdgeHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX, pTargetY + pCornerHeight, pCornerWidth, pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX, pSourceY + pCornerHeight, pCornerWidth, pSourceHeight - pEdgeHeight - pCornerHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX + pCornerWidth, pTargetY + pCornerHeight, pTargetWidth - pEdgeWidth - pCornerWidth, pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX + pCornerWidth, pSourceY + pCornerHeight, pSourceWidth - pEdgeWidth - pCornerWidth, pSourceHeight - pEdgeHeight - pCornerHeight,textureWidth,textureHeight);
           graphics.blitRepeating(pAtlasLocation, pTargetX + pTargetWidth - pEdgeWidth, pTargetY + pCornerHeight, pCornerWidth, pTargetHeight - pEdgeHeight - pCornerHeight, pSourceX + pSourceWidth - pEdgeWidth, pSourceY + pCornerHeight, pEdgeWidth, pSourceHeight - pEdgeHeight - pCornerHeight,textureWidth,textureHeight);
        }
     }

	public static void drawBox(GuiGraphics graphics, int x, int y, int w, int h, int color, boolean inner) {
		int x2 = x+w;
		int y2 = y+h;
		if (inner) {
			graphics.fill(x, y, x2, y+1, color); 						// top
			graphics.fill(x, y2-1, x2, y2, color); 						// bottom
			graphics.fill(x, y, x+1, y2, color); 						// left
			graphics.fill(x2-1, y, x2, y2, color); 						// right
		} else {
			graphics.fill(x-1, y-1, x2+1, y, color); 		// top
			graphics.fill(x-1, y2, x2+1, y2+1, color); 	// bottom
			graphics.fill(x-1, y, x, y2, color); 						// left
			graphics.fill(x2, y, x2+1, y2, color); 						// right
		}
	}

	public static void drawBox(GuiGraphics graphics, Rect box, int color, boolean inner) {
		drawBox(graphics, box.getX(), box.getY(), box.getW(), box.getH(), color, inner);
	}

	public static Rect getWidgetBounds(UIWidget widget, PrimaryLayer primaryLayer) {
		int x = widget.getScreenX();
		int y = widget.getScreenY();
		if (primaryLayer.getManager() instanceof CUIScreen) {
			x += (ClientUtils.getMc().getWindow().getGuiScaledWidth() - primaryLayer.getWidth())/2;
			y += (ClientUtils.getMc().getWindow().getGuiScaledHeight() - primaryLayer.getHeight())/2;
		} else {
			x += ClientUtils.screenCenterX();
			y += ClientUtils.screenCenterY();
		}

		var w1 = widget.getParent();
		while (w1.getParent() != null) {
			if (w1 instanceof Layer l) {
				if (l.isSmoothScrollEnabled()) {
					x += (int)l.getDisplayOffsetX();
					y += (int)l.getDisplayOffsetY();
				} else {
					x += l.getOffsetX();
					y += l.getOffsetY();
				}
			}
			w1 = w1.getParent();
		}

		return new Rect(x, y, widget.getWidth(), widget.getHeight());
	}

	@Nullable
	public static Size2i getImgSize(ResourceLocation location) {
		if (location != null) {
			var resource = ClientUtils.getMc().getResourceManager().getResource(location);
			if (resource.isPresent()) {
				try (InputStream stream = resource.get().open()) {
					BufferedImage image= ImageIO.read(stream);
					return new Size2i(image.getWidth(), image.getHeight());
				} catch (IOException e) {
					FHMain.LOGGER.warn(e);
				}
			}
		}
		return null;
	}
}
