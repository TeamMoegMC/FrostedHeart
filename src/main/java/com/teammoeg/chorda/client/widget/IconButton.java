/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.client.widget;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;

import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class IconButton extends Button {
    @Setter
    private FlatIcon icon;
    public int color;
    // 为什么是int? 混素达咩(
    private int scale;

    /**
     * @param icon 按钮的图标 {@link FlatIcon}
     */
    public IconButton(int x, int y, FlatIcon icon, int color, Component title, OnPress pressedAction) {
        this(x, y, icon, color, 1, title, pressedAction);
    }

    public IconButton(int x, int y, FlatIcon icon, int color, int scale, Component title, OnPress pressedAction) {
        super(x, y, icon.size.width, icon.size.height, title, pressedAction, Button.DEFAULT_NARRATION);
        this.scale = Mth.clamp(scale, 1, Integer.MAX_VALUE);
        width *= scale;
        height *= scale;
        this.color = color;
        this.icon = icon;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int color = isActive() ? this.color : 0xFF666666;
        int backgroundColor = Colors.makeDark(color, 0.3F);
        float alpha = 0.5F;
        if (isHoveredOrFocused()) {
            graphics.fill(getX(), getY(), getX()+getWidth(), getY()+getHeight(), Colors.setAlpha(backgroundColor, alpha));
            if (!getMessage().getString().isBlank() && isHovered()) {
                int textWidth = ClientUtils.font().width(getMessage());
                int renderX = getX()-textWidth+8;
                if (renderX < 0) {
                    graphics.fill(getX(),
                            getY()-12,
                            getX()+2 + textWidth,
                            getY(),
                            Colors.setAlpha(backgroundColor, alpha));
                    graphics.drawString(ClientUtils.font(), getMessage(), getX()+2, getY()-10, color);
                } else {
                    graphics.fill(getX()-textWidth+getWidth()-1,
                            getY()-12,
                            getX()+getWidth(),
                            getY(),
                            Colors.setAlpha(backgroundColor, alpha));
                    graphics.drawString(ClientUtils.font(), getMessage(), getX()-textWidth+getWidth(), getY()-10, color);
                }
            }
        }

        CGuiHelper.bindTexture(FlatIcon.ICON_LOCATION);
        CGuiHelper.blitColored(graphics.pose(), getX(), getY(), getWidth(), getHeight(), icon.x*scale, icon.y*scale, getWidth(), getHeight(), FlatIcon.TEXTURE_WIDTH*scale, FlatIcon.TEXTURE_HEIGHT*scale, color, this.alpha);
    }

    public void setScale(int scale) {
        int w = this.width / this.scale;
        int h = this.height / this.scale;
        this.scale = scale;
        this.width = w * scale;
        this.height = h * scale;
    }
}
