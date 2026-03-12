package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.math.Colors;
import net.minecraft.client.gui.GuiGraphics;

public class TipTheme implements Theme {
    public TipLayer tipLayer;

    public int getFontColor() {
        return tipLayer != null ? tipLayer.display.fontColor() : Colors.CYAN;
    }

    public int getBackgroundColor() {
        float alpha = ClientUtils.getMc().screen == null ? 0.55F : 0.8F;
        return tipLayer != null ? Colors.setAlpha(tipLayer.display.backgroundColor(), alpha) : Colors.setAlpha(UIBGColor(), 0.5F);
    }

    public TipTheme(TipLayer tipLayer) {
        this.tipLayer = tipLayer;
    }
    public TipTheme() {}

    @Override
    public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled) {
        if (isHighlight) {
            graphics.fill(x, y, x+w, y+h, Colors.setAlpha(Colors.makeDark(getFontColor(), 0.3F), 0.5F));
        }
    }

    @Override
    public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {}

    @Override
    public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {}

    @Override
    public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {}

    @Override
    public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {}

    @Override
    public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {}

    @Override
    public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x-4, y-4, x+w+4, y+h+4, getBackgroundColor());
    }

    @Override
    public int UITextColor() {
        return getFontColor();
    }

    @Override
    public int UIAltTextColor() {
        return getFontColor();
    }

    @Override
    public int UIBGColor() {
        return Colors.BLACK;
    }

    @Override
    public int UIBGBorderColor() {
        return 0;
    }

    @Override
    public int buttonTextColor() {
        return getFontColor();
    }

    @Override
    public int buttonTextOverColor() {
        return getFontColor();
    }

    @Override
    public int buttonTextDisabledColor() {
        return Colors.ChatColors.GRAY;
    }

    @Override
    public int errorColor() {
        return Colors.RED;
    }

    @Override
    public int successColor() {
        return 0xFFC1E52F;
    }

    @Override
    public boolean isUITextShadow() {
        return false;
    }

    @Override
    public boolean isButtonTextShadow() {
        return true;
    }
}
