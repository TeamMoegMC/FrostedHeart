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

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ui.Colors;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class ColorEditbox extends EditBox {
    private static final String PREFIX = "0x";
    private static final String PREFIX_WITH_ALPHA = "0xFF";
    protected final Font font;
    protected final boolean withAlpha;

    public ColorEditbox(Font font, int x, int y, int width, int height, Component message) {
        this(font, x, y, width, height, message, true, 0);
    }

    public ColorEditbox(Font font, int x, int y, int width, int height, Component message, boolean withAlpha, int colorValue) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.withAlpha = withAlpha;
        setValue(colorValue);
        setMaxLength(withAlpha ? 6 : 8);
        setResponder(s -> {
            try {
                setTextColor(Colors.WHITE);
                Integer.parseUnsignedInt(s, 16);
            } catch (NumberFormatException e) {
                setTextColor(Colors.RED);
            }
        });
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, getPrefix(), getX()-2-font.width(getPrefix()), getY()+(getHeight()/2)-4, 0xFFFFFFFF);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(1, 1, 0);
        graphics.fill(getX()+getWidth()+2, getY(), getX()+getWidth()+getHeight()+2, getY()+getHeight(), Colors.makeDark(getColorValue(), 0.75F));
        pose.translate(-1, -1, 0);
        graphics.fill(getX()+getWidth()+2, getY(), getX()+getWidth()+getHeight()+2, getY()+getHeight(), getColorValue());
        pose.popPose();
    }

    public int getColorValue() {
        try {
            return withAlpha ? Colors.setAlpha(Integer.parseUnsignedInt(getValue(), 16), 1F) : Integer.parseUnsignedInt(getValue(), 16);
        } catch (NumberFormatException e) {
            return Colors.RED;
        }
    }

    public void setValue(int value) {
        if (withAlpha) {
            setValue(Colors.toHexString(value).substring(2).toUpperCase());
        } else {
            setValue(Colors.toHexString(value).toUpperCase());
        }
    }

    private String getPrefix() {
        return withAlpha ? PREFIX_WITH_ALPHA : PREFIX;
    }

    @Override
    public String getValue() {
        return (withAlpha ? "FF" : "") + super.getValue();
    }
}
