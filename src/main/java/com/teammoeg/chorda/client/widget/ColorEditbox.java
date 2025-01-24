package com.teammoeg.chorda.client.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.ui.ColorHelper;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
                setTextColor(ColorHelper.WHITE);
                Integer.parseUnsignedInt(s, 16);
            } catch (NumberFormatException e) {
                setTextColor(ColorHelper.RED);
            }
        });
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, getPrefix(), getX()-2-font.width(getPrefix()), getY()+(getHeight()/2)-4, 0xFFFFFFFF);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(1, 1, 0);
        graphics.fill(getX()+getWidth()+2, getY(), getX()+getWidth()+getHeight()+2, getY()+getHeight(), ColorHelper.makeDark(getColorValue(), 0.75F));
        pose.translate(-1, -1, 0);
        graphics.fill(getX()+getWidth()+2, getY(), getX()+getWidth()+getHeight()+2, getY()+getHeight(), getColorValue());
        pose.popPose();
    }

    public int getColorValue() {
        try {
            return withAlpha ? ColorHelper.setAlpha(Integer.parseUnsignedInt(getValue(), 16), 1F) : Integer.parseUnsignedInt(getValue(), 16);
        } catch (NumberFormatException e) {
            return ColorHelper.RED;
        }
    }

    public void setValue(int value) {
        if (withAlpha) {
            setValue(ColorHelper.toHexString(value).substring(2).toUpperCase());
        } else {
            setValue(ColorHelper.toHexString(value).toUpperCase());
        }
    }

    private String getPrefix() {
        return withAlpha ? PREFIX_WITH_ALPHA : PREFIX;
    }

    @Override
    public @NotNull String getValue() {
        return (withAlpha ? "FF" : "") + super.getValue();
    }
}
