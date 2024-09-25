package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class EmptyScreen extends Screen {
    public EmptyScreen() {
        super(TranslateUtils.str(""));
    }

    @Override
    public void render(GuiGraphics matrixStack, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
