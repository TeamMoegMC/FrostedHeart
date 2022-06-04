package com.teammoeg.frostedheart.research.gui;

import java.util.OptionalDouble;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

public class FHGuiHelper {
	public static RenderType BOLD_LINE_TYPE;
	static {
		RenderType.State renderState;
		renderState = RenderStateAccess.getState();
		BOLD_LINE_TYPE = RenderType.makeType("fh_line_bold", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 128,
				renderState);
	}

	// hack to access render state protected members
	public static class RenderStateAccess extends RenderState {
		public static RenderType.State getState() {
			return RenderType.State.getBuilder().line(new RenderState.LineState(OptionalDouble.of(4)))// this is line
																										// width
					.layer(VIEW_OFFSET_Z_LAYERING).target(MAIN_TARGET).writeMask(COLOR_DEPTH_WRITE).build(true);
		}

		public RenderStateAccess(String p_i225973_1_, Runnable p_i225973_2_, Runnable p_i225973_3_) {
			super(p_i225973_1_, p_i225973_2_, p_i225973_3_);
		}

	}

	// draw a line from start to end by color, ABSOLUTE POSITION
	public static void drawLine(MatrixStack matrixStack, Color4I color, int startX, int startY, int endX, int endY) {
		IVertexBuilder vertexBuilderLines = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource()
				.getBuffer(BOLD_LINE_TYPE);
		drawLine(matrixStack.getLast().getMatrix(), vertexBuilderLines, color, startX, startY, endX, endY);
	}

	private static void drawLine(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int startX, int startY,
			int endX, int endY) {
		renderBuffer.pos(mat, startX, startY, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
				.endVertex();
		renderBuffer.pos(mat, endX, endY, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
				.endVertex();
	}
}
