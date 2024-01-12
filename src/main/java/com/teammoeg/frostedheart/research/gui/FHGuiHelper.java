/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.gui;

import java.util.OptionalDouble;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

public class FHGuiHelper {
    public static final RenderType BOLD_LINE_TYPE = RenderType.makeType("fh_line_bold",
            DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 128, RenderStateAccess.getLineState(4));

    // hack to access render state protected members
    public static class RenderStateAccess extends RenderState {
        public static RenderType.State getLineState(double width) {
            return RenderType.State.getBuilder().line(new RenderState.LineState(OptionalDouble.of(width)))// this is
                    // line
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

    // draw a rectangle
    public static void fillGradient(MatrixStack matrixStack, int x1, int y1, int x2, int y2, int colorFrom, int colorTo) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        fillGradient(matrixStack.getLast().getMatrix(), bufferbuilder, x1, y1, x2, y2, colorFrom, colorTo);
        tessellator.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
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
        builder.pos(matrix, x2, y2, 0f).color(f1, f2, f3, f).endVertex();
        builder.pos(matrix, x2, y1, 0f).color(f1, f2, f3, f).endVertex();
        builder.pos(matrix, x1, y1, 0f).color(f5, f6, f7, f4).endVertex();
        builder.pos(matrix, x1, y2, 0f).color(f5, f6, f7, f4).endVertex();
    }

    private static void drawLine(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int startX, int startY,
                                 int endX, int endY) {
        RenderSystem.enableColorMaterial();
        renderBuffer.pos(mat, startX, startY, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
        renderBuffer.pos(mat, endX, endY, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
    }

    private static void drawRect(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int x, int y, int w, int h) {
        renderBuffer.pos(mat, x, y, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai()).endVertex();
        renderBuffer.pos(mat, x + w, y, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
        renderBuffer.pos(mat, x, y + h, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
        renderBuffer.pos(mat, x + w, y + h, 0F).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
    }
}
