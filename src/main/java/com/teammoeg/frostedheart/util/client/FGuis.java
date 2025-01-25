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

package com.teammoeg.frostedheart.util.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.teammoeg.frostedheart.FHShaders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class FGuis {
    /**
     * 圆角矩形
     * @param guiGraphics The GuiGraphics object.
     * @param x1 The x coordinate of the top left corner of the rectangle.
     * @param y1 The y coordinate of the top left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param radius 圆角大小，范围0-1
     * @param color The color of the rectangle.
     */
    public static void fillRoundRect(GuiGraphics guiGraphics, int x1, int y1, int width, int height, float radius, int color) {
        int x2 = x1 + width;
        int y2 = y1 + height;

        final float ratio = (float) height / (float) width;

        RenderSystem.setShader(FHShaders::getRoundRectShader);
        ShaderInstance shader = FHShaders.getRoundRectShader();
        shader.safeGetUniform("Ratio").set(ratio);
        shader.safeGetUniform("Radius").set(radius * ratio);

        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(matrix4f, x1, y1, 0).uv(0, 0).color(color).endVertex();
        builder.vertex(matrix4f, x1, y2, 0).uv(0, 1).color(color).endVertex();
        builder.vertex(matrix4f, x2, y2, 0).uv(1, 1).color(color).endVertex();
        builder.vertex(matrix4f, x2, y1, 0).uv(1, 0).color(color).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    /**
     * 绘制圆环
     * @param guiGraphics The GuiGraphics object.
     * @param x 圆环中心x坐标
     * @param y 圆环中心y坐标
     * @param innerRadius 内半径
     * @param outerRadius 外半径
     * @param startAngle 起始角度
     * @param endAngle 结束角度
     * @param color 颜色
     */
    public static void drawRing(GuiGraphics guiGraphics, int x, int y, float innerRadius, float outerRadius,float startAngle,float endAngle, int color) {
        float x2 = (int) (x + outerRadius);
        float y2 = (int) (y + outerRadius);
        float x1 = (int) (x - outerRadius);
        float y1 = (int) (y - outerRadius);

        RenderSystem.setShader(FHShaders::getRingShader);
        ShaderInstance shader = FHShaders.getRingShader();
        shader.safeGetUniform("innerRadius").set(innerRadius/outerRadius/2);
        shader.safeGetUniform("outerRadius").set(0.5f);


        shader.safeGetUniform("startAngle").set(startAngle);
        shader.safeGetUniform("endAngle").set(endAngle);

        RenderSystem.enableBlend();
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(matrix4f, x1, y1, 0).uv(0, 0).color(color).endVertex();
        builder.vertex(matrix4f, x1, y2, 0).uv(0, 1).color(color).endVertex();
        builder.vertex(matrix4f, x2, y2, 0).uv(1, 1).color(color).endVertex();
        builder.vertex(matrix4f, x2, y1, 0).uv(1, 0).color(color).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    public static void blitRound(GuiGraphics guiGraphics, ResourceLocation atlasLocation, int x, int y, int width, int height) {
        int x2 = x + width;
        int y2 = y + height;

        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(FHShaders::getRoundShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(matrix4f, (float)x, (float)y, 0).uv(0, 0);
        builder.vertex(matrix4f, (float)x, (float)y2, 0).uv(0, 1);
        builder.vertex(matrix4f, (float)x2, (float)y2, 0).uv(1, 1);
        builder.vertex(matrix4f, (float)x2, (float)y, 0).uv(1, 0);
        BufferUploader.drawWithShader(builder.end());
    }
}
