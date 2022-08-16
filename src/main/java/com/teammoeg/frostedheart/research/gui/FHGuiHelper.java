package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

public class FHGuiHelper {
    public static final RenderType BOLD_LINE_TYPE = RenderType.makeType("fh_line_bold", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 128,
    		RenderStateAccess.getLineState(4));
    public static final RenderType FORE_LINE_TYPE= RenderType.makeType("fh_rect_forecast", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 128,
    		RenderStateAccess.getLineState(14));
    // hack to access render state protected members
    public static class RenderStateAccess extends RenderState {
        public static RenderType.State getLineState(double width) {
            return RenderType.State.getBuilder().line(new RenderState.LineState(OptionalDouble.of(width)))// this is line
                    // width
                    .layer(VIEW_OFFSET_Z_LAYERING).target(MAIN_TARGET).writeMask(COLOR_DEPTH_WRITE).build(true);
        }
        public static RenderType.State getRectState() {
            return RenderType.State.getBuilder()
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
    // draw a rectangle, ABSOLUTE POSITION
    public static void drawRect(MatrixStack matrixStack, Color4I color, int x, int y, int w, int h) {
        IVertexBuilder vertexBuilderLines = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource()
                .getBuffer(FORE_LINE_TYPE);
        drawRect(matrixStack.getLast().getMatrix(), vertexBuilderLines, color, x,y,w,h);
    }

    private static void drawLine(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int startX, int startY,
                                 int endX, int endY) {
    	RenderSystem.enableColorMaterial();
        renderBuffer.pos(mat, startX, startY, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
        renderBuffer.pos(mat, endX, endY, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
    }
    private static void drawRect(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int x, int y,
            int w, int h) {
    	
		renderBuffer.pos(mat,x,y, 255F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
		.endVertex();
		renderBuffer.pos(mat,x+w,y, 255F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
		.endVertex();
		renderBuffer.pos(mat,x,y+h, 255F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
		.endVertex();
		renderBuffer.pos(mat,x+w,y+h, 255F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
		.endVertex();
	}
}
