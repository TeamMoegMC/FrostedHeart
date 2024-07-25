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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.teammoeg.frostedheart.content.tips.client.gui.widget.IconButton;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;

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
    // hack to access render state protected members
    public static class RenderStateAccess extends RenderState {
        public static RenderType.State getLineState(double width) {
            return RenderType.State.getBuilder().line(new RenderState.LineState(OptionalDouble.of(width)))// width
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

    public static Minecraft MC = Minecraft.getInstance();

    public static final RenderType BOLD_LINE_TYPE = RenderType.makeType("fh_line_bold",
            DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 128, RenderStateAccess.getLineState(4));

    private static void drawVertexLine(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int startX, int startY,
                                 int endX, int endY,float z) {
    	//RenderSystem.disableTexture();
        RenderSystem.enableColorMaterial();
        //RenderSystem.colorMask(false, false, false, false);
        renderBuffer.pos(mat, startX, startY, z).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
        renderBuffer.pos(mat, endX, endY,z).color(color.redi(), color.greeni(), color.bluei(), color.alphai())
                .endVertex();
        //RenderSystem.enableTexture();
    }
    private static void drawVertexLine2(Matrix4f mat, IVertexBuilder renderBuffer, Color4I color, int startX, int startY,
            int endX, int endY,float z) {
		RenderSystem.disableTexture();
		RenderSystem.enableColorMaterial();
		//RenderSystem.colorMask(false, false, false, false);
	
		renderBuffer.pos(mat, startX, 0, z).color(color.redi(), color.greeni(), color.bluei(), color.alphai()).endVertex();
		renderBuffer.pos(mat, endX,   0, z).color(color.redi(), color.greeni(), color.bluei(), color.alphai()).endVertex();
		renderBuffer.pos(mat, 0, startY, z).color(color.redi(), color.greeni(), color.bluei(), color.alphai()).endVertex();
		renderBuffer.pos(mat, 0, endY  , z).color(color.redi(), color.greeni(), color.bluei(), color.alphai()).endVertex();
		RenderSystem.enableTexture();
	}
    // draw a line from start to end by color, ABSOLUTE POSITION
    public static void drawLine(MatrixStack matrixStack, Color4I color, int startX, int startY, int endX, int endY,float z) {
    	Tessellator t=Tessellator.getInstance();
        BufferBuilder vertexBuilderLines = t.getBuffer();
        vertexBuilderLines.begin(GL11C.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        drawVertexLine(matrixStack.getLast().getMatrix(), vertexBuilderLines, color, startX, startY, endX, endY,z);
        t.draw();
    }

    // draw a line from start to end by color, ABSOLUTE POSITION
    public static void drawLine(MatrixStack matrixStack, Color4I color, int startX, int startY, int endX, int endY) {
        IVertexBuilder vertexBuilderLines = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource()
                .getBuffer(BOLD_LINE_TYPE);
        drawVertexLine(matrixStack.getLast().getMatrix(), vertexBuilderLines, color, startX, startY, endX, endY,0f);
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

    /**
     * 绘制一个不完整的圆
     * @param radius 半径
     * @param partial 圆的完整度 {@code 0.0 ~ 1.0}
     */
    public static void drawPartialCircle(int x, int y, double radius, float partial, Color4I color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        RenderSystem.disableCull();

        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(x, y, 0).color(color.redf(), color.greenf(), color.bluef(), color.alphaf()).endVertex();
        for (int i = -180; i <= 360*partial-180; i++) { //为了让圆顺时针绘制
            double angle = i * Math.PI / 180;
            double x2 = x + Math.sin(-angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.pos(x2, y2, 0).color(color.redf(), color.greenf(), color.bluef(), color.alphaf()).endVertex();
        }

        tessellator.draw();
        RenderSystem.enableCull();
    }

    /**
     * 绘制一个多边形
     * @param radius 半径
     * @param sides 边数
     */
    public static void drawPolygon(int x, int y, double radius, int sides, Color4I color) {
        sides = MathHelper.clamp(sides, 3, 360);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        bufferBuilder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        bufferBuilder.pos(x, y, 0).color(color.redf(), color.greenf(), color.bluef(), color.alphaf()).endVertex();
        for (int i = 0; i <= sides; i++) {
            double angle = i * (360F/sides) * Math.PI / 180;
            double x2 = x + Math.sin(angle) * radius;
            double y2 = y + Math.cos(angle) * radius;
            bufferBuilder.pos(x2, y2, 0).color(color.redf(), color.greenf(), color.bluef(), color.alphaf()).endVertex();
        }

        tessellator.draw();
    }

    /**
     * 渲染一个图标按钮
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     * @param BGColor 未被选中时的背景颜色，为 0 时不显示
     * @return 是否被按下
     */
    public static boolean renderIconButton(MatrixStack matrixStack, Point icon, int mouseX, int mouseY, int x, int y, int color, int BGColor) {
        if (color != 0 && RawMouseHelper.isMouseIn(mouseX, mouseY, x, y, 10, 10)) {
            AbstractGui.fill(matrixStack, x, y, x+10, y+10, 50 << 24 | color & 0x00FFFFFF);
        } else if (BGColor != 0) {
            AbstractGui.fill(matrixStack, x, y, x+10, y+10, BGColor);
        }
        return renderButton(matrixStack, mouseX, mouseY, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80, color, IconButton.ICON_LOCATION);
    }

    public static boolean renderButton(MatrixStack matrixStack, int mouseX, int mouseY, int x, int y, int w, int h,
                                       float uOffset, float vOffset, int uWidth, int vHeight, int textureW, int textureH, int color, ResourceLocation resourceLocation) {
        if (color != 0) {
            float alpha = (color >> 24 & 0xFF) / 255F;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(r, g, b, alpha);
            MC.getTextureManager().bindTexture(resourceLocation);
            AbstractGui.blit(matrixStack, x, y, w, h, uOffset, vOffset, uWidth, vHeight, textureW, textureH);
            RenderSystem.disableBlend();
        } else {
            MC.getTextureManager().bindTexture(resourceLocation);
            AbstractGui.blit(matrixStack, x, y, w, h, uOffset, vOffset, uWidth, vHeight, textureW, textureH);
        }

        return RawMouseHelper.isMouseIn(mouseX, mouseY, x, y, w, h) && RawMouseHelper.isLeftClicked();
    }

    /**
     * 渲染一个图标
     * @param icon {@link IconButton}
     * @param color 图标的颜色
     */
    public static void renderIcon(MatrixStack matrixStack, Point icon, int x, int y, int color) {
        if (color != 0) {
            float alpha = (color >> 24 & 0xFF) / 255F;
            float r = (color >> 16 & 0xFF) / 255F;
            float g = (color >> 8 & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(r, g, b, alpha);
            MC.getTextureManager().bindTexture(IconButton.ICON_LOCATION);
            AbstractGui.blit(matrixStack, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80);
            RenderSystem.disableBlend();
            RenderSystem.color4f(1, 1, 1, 1);
        } else {
            MC.getTextureManager().bindTexture(IconButton.ICON_LOCATION);
            AbstractGui.blit(matrixStack, x, y, 10, 10, icon.getX(), icon.getY(), 10, 10, 80, 80);
        }
    }

    private static final FontRenderer font = MC.fontRenderer;
    private static final Map<String, List<String>> textWrapCache = new HashMap<>();

    public static int formatAndDraw(ITextComponent component, MatrixStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        String text = component.getString().replaceAll("&(?!&)", "\u00a7")
                .replaceAll("\\$configPath\\$", FMLPaths.CONFIGDIR.get().toString().replaceAll("\\\\", "\\\\\\\\"));

        return wrapAndDraw(text, ms, x, y, maxWidth, color, lineSpace, shadow);
    }

    public static int wrapAndDraw(String text, MatrixStack ms, float x, float y, int maxWidth, int color, int lineSpace, boolean shadow) {
        List<String> lines = wrapString(text, maxWidth);

        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                if (shadow) {
                    font.drawStringWithShadow(ms, lines.get(i), x, y, color);
                } else {
                    font.drawString(ms, lines.get(i), x, y, color);
                }
            } else {
                if (shadow) {
                    font.drawStringWithShadow(ms, lines.get(i), x, y + (i * lineSpace), color);
                } else {
                    font.drawString(ms, lines.get(i), x, y + (i * lineSpace), color);
                }
            }
        }

        return lines.size();
    }

    public static List<String> wrapString(String text, int maxWidth) {
        //因为整不明白原版的方法所以搞了个傻子都会用的换行
        List<String> lines = new ArrayList<>();
        maxWidth = Math.max(1, maxWidth);

        if (textWrapCache.containsKey(text + maxWidth)) {
            lines = new ArrayList<>(textWrapCache.get(text + maxWidth));
            return lines;
        } else if (font.getStringWidth(text) < maxWidth) {
            lines.add(text);
            return lines;
        } else {
            StringBuilder line = new StringBuilder();
            String[] words = text.split(" ");
            for (String word : words) {
                if (font.getStringWidth(word) > maxWidth) {
                    for (char c : word.toCharArray()) {
                        String potentialLine = line.toString() + c;
                        int width = font.getStringWidth(potentialLine);

                        if (width > maxWidth) {
                            if (line.toString().endsWith("\u00A7")) {
                                line = new StringBuilder(line.substring(0, line.length() - 1));
                                lines.add(line.toString());
                                line = new StringBuilder("\u00A7" + c);
                            } else {
                                lines.add(line.toString());
                                line = new StringBuilder(String.valueOf(c));
                            }
                        } else {
                            line = new StringBuilder(potentialLine);
                        }
                    }
                    line.append(" ");
                } else {
                    String potentialLine = line + word + " ";
                    int width = font.getStringWidth(potentialLine);

                    if (width > maxWidth) {
                        if (line.toString().endsWith("\u00A7")) {
                            line = new StringBuilder(line.substring(0, line.length() - 1));
                            lines.add(line.toString());
                            line = new StringBuilder("\u00A7" + word + " ");
                        } else {
                            lines.add(line.toString());
                            line = new StringBuilder(word + " ");
                        }
                    } else {
                        line = new StringBuilder(potentialLine);
                    }
                }

            }

            if (line.length() > 0) {
                lines.add(line.toString());
            }

            //为每行开头添加生效的格式化代码
            Pattern pattern = Pattern.compile("\u00A7.");
            StringBuilder formattingCode = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                lines.set(i, formattingCode + lines.get(i));

                Matcher matcher = pattern.matcher(lines.get(i).substring(formattingCode.length()));
                while (matcher.find() && formattingCode.length() < 32) {
                    if (matcher.group().equals("\u00A7r")) {
                        formattingCode = new StringBuilder();
                    } else {
                        formattingCode.append(matcher.group());
                    }
                }
            }

            textWrapCache.put(text + maxWidth, lines);
            return lines;
        }
    }
}
