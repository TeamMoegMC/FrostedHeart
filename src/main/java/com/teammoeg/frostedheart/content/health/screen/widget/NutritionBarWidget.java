package com.teammoeg.frostedheart.content.health.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.util.client.FGuis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.text.DecimalFormat;

public class NutritionBarWidget extends AbstractWidget {

    public static float progress;

    private static final Font font = Minecraft.getInstance().font;
    private static final int w = 32;
    private static final int h = 32;

    private final Component icon;
    private final Component desc;
    private final int color;
    private final float min;
    private final float max;
    private float value;
    private float hoverProgress = 0.0f;

    public void setValue(float value) {
        this.value = value;
    }

    public NutritionBarWidget(int pX, int pY,float min,float max, Component icon,Component desc,int color) {
        super(pX-w/2, pY-h/2, w, h, Component.empty());
        this.icon = icon;
        this.desc = desc;
        this.color = color;
        this.min=Mth.clamp(min, 0, 1.2f)/1.2f;
        this.max=Mth.clamp(max, 0, 1.2f)/1.2f;
    }

    private static final float end = 160;
    private static final float start = 20;

    private void renderBar(GuiGraphics guiGraphics, int mouseX, int mouseY){

        int x = this.getX()+this.getWidth()/2;
        int y = this.getY()+this.getHeight()/2;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.pushPose();
        pose.translate(hoverProgress*3,0,0);
        pose.scale(2.0f,2.0f,1.0f);
        guiGraphics.drawCenteredString(font, icon, x/2, y/2-font.lineHeight/2, 0xFFFFFF);
        pose.popPose();
        pose.translate(hoverProgress*5,0,0);
        if(min>0)
        	FGuis.drawRing(guiGraphics, x,  y, 9, 16, start,start+min*(end-start) * progress, 0xB0d4a31c);
        FGuis.drawRing(guiGraphics, x,  y, 9, 16, start+min*(end-start) * progress,start+max*(end-start) * progress, 0x40FFFFFF);
        if(max<1.2f)
        	FGuis.drawRing(guiGraphics, x,  y, 9, 16, start+max*(end-start) * progress,start+(end-start) * progress, 0xB0FF1111);
        FGuis.drawRing(guiGraphics, x,  y, 10, 15, start,start+(end-start) * progress, 0x80FFFFFF);
        FGuis.drawRing(guiGraphics, x,  y, 10.5f, 14.5f, start,start+Mth.clamp(value, 0, 1.2f)/1.2f *(end-start) * progress, color);
        if(hoverProgress>0) {

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, hoverProgress);
            DecimalFormat df = new DecimalFormat("0.0%");
            Component desc = Screen.hasShiftDown()?Component.literal(df.format(value)):this.desc;
            int fw = font.width(desc);
            RenderSystem.enableBlend();
            FGuis.fillRoundRect(guiGraphics, x + 16, y - font.lineHeight / 2 - 3, fw + 2, font.lineHeight + 5, 0.2f, 0x80000000);
            RenderSystem.disableBlend();
            guiGraphics.drawString(font, desc, x + 18, y - font.lineHeight / 2, 0xFFFFFF);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        pose.popPose();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float v) {

        hoverProgress = isHovered()?Math.min(hoverProgress + 0.05f, 1.0f):Math.max(hoverProgress - 0.05f, 0.0f);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, progress);
        renderBar(guiGraphics,mouseX,mouseY);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
