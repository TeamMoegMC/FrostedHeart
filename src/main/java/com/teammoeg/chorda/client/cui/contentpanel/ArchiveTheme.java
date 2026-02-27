package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.chorda.math.Colors;
import net.minecraft.client.gui.GuiGraphics;

public class ArchiveTheme implements Theme {
    public static final ArchiveTheme INSTANCE = new ArchiveTheme();
    private ArchiveTheme() {}

    @Override
    public void drawButton(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight, boolean enabled) {
        // TODO
    }

    @Override
    public void drawSliderBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {

    }

    @Override
    public void drawTextboxBackground(GuiGraphics graphics, int x, int y, int w, int h, boolean focused) {

    }

    @Override
    public void drawSliderBar(GuiGraphics graphics, int x, int y, int w, int h, boolean isHighlight) {

    }

    @Override
    public void drawPanel(GuiGraphics graphics, int x, int y, int w, int h) {

    }

    @Override
    public void drawSlot(GuiGraphics graphics, int x, int y, int w, int h) {

    }

    @Override
    public void drawUIBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        drawUIBackgroundWithBorder(graphics, x, y, w, h, 0);
    }

    public void drawUIBackgroundWithBorder(GuiGraphics graphics, int x, int y, int w, int h, int border) {
        graphics.fill(x-border, y-border, x+w+border*2, y+h+border, 0xFF444651);
        CGuiHelper.drawBox(graphics, x-border, y-border, w+border*3, h+border*2, Colors.L_BG_GRAY, true);
    }

    @Override
    public int getUITextColor() {
        return Colors.WHITE;
    }

    @Override
    public int getButtonTextColor() {
        return Colors.WHITE;
    }

    @Override
    public int getButtonTextOverColor() {
        return Colors.WHITE;
    }

    @Override
    public int getButtonTextDisabledColor() {
        return Colors.ChatColors.GRAY;
    }

    @Override
    public int getErrorColor() {
        return Colors.RED;
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
