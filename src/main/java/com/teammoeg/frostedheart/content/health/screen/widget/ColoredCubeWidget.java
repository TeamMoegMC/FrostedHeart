package com.teammoeg.frostedheart.content.health.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ColoredCubeWidget extends AbstractWidget {

    public static float progress;

    private static final Font font = Minecraft.getInstance().font;
    private static final int hotColor = 0xFFE60101;
    private static final int warmColor = 0xFFFFA630;
    private static final int coldColor = 0xFF36A0FD;
    private static final int frozenColor = 0xFF130DB8;

    private final Component desc;

    private float temp;

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public ColoredCubeWidget(int pX, int pY, int pWidth, int pHeight, Component desc) {
        super(pX, pY, pWidth, pHeight, Component.empty());
        this.desc = desc;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int i1, float v) {
        int x1 = this.getX();
        int y1 = this.getY();
        int x2 = this.getX() + this.getWidth();
        int y2 = this.getY() + this.getHeight();
        int color = getTempColor(temp+37);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, progress);

        guiGraphics.fill(x1, y1, x2, y2, color);
        if(isHovered()) {
            //guiGraphics.drawString(Minecraft.getInstance().font, desc, i, i1, 0xFFFFFF);
            guiGraphics.drawString(font, (temp+37)+"°", i, i1-9, 0xFFFFFF);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    private static int getTempColor(float temp) {
        if (temp > 39) {
            return hotColor;
        }else if (temp > 36) {
            return mixColor(warmColor, hotColor, (temp - 36) / 3);
        }else if (temp > 34) {
            return mixColor(coldColor,warmColor , (temp - 34) / 2);
        }else if (temp > 32) {
            return mixColor(frozenColor, coldColor, (temp - 32) / 2);
        }else {
            return frozenColor;
        }
    }

    private static int mixColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 * (1 - ratio) + r2 * ratio);
        int g = (int) (g1 * (1 - ratio) + g2 * ratio);
        int b = (int) (b1 * (1 - ratio) + b2 * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
