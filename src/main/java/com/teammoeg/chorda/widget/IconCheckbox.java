package com.teammoeg.chorda.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.chorda.util.CGuis;
import com.teammoeg.chorda.util.client.ColorHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class IconCheckbox extends Checkbox {
    private static final IconButton.Icon SELECTED_ICON = IconButton.Icon.CHECK;
    private int scale;

    public IconCheckbox(int pX, int pY, Component pMessage, boolean pSelected) {
        this(pX, pY, 1, pMessage, pSelected);
    }

    public IconCheckbox(int pX, int pY, int scale, Component pMessage, boolean pSelected) {
        super(pX, pY, SELECTED_ICON.size.width, SELECTED_ICON.size.height, pMessage, pSelected, false);
        this.scale = Mth.clamp(scale, 1, Integer.MAX_VALUE);
        width *= scale;
        height *= scale;
    }

    public void setScale(int scale) {
        int w = this.width / this.scale;
        int h = this.height / this.scale;
        this.scale = scale;
        this.width = w * scale;
        this.height = h * scale;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        IconButton.Icon icon = selected() ? SELECTED_ICON : IconButton.Icon.CROSS;
        RenderSystem.enableDepthTest();
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        CGuis.bindTexture(IconButton.ICON_LOCATION);
        CGuis.blitColored(pGuiGraphics.pose(), getX(), getY(), getWidth(), getHeight(), icon.x*scale, icon.y*scale, getWidth(), getHeight(), IconButton.TEXTURE_WIDTH*scale, IconButton.TEXTURE_HEIGHT*scale, ColorHelper.CYAN, alpha);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }


}
