package com.teammoeg.frostedheart.content.tips.client.gui;

import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public class EmptyScreen extends Screen {
    public EmptyScreen() {
        super(Lang.str(""));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
