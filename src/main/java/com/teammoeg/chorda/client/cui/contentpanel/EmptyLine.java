package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.UIWidget;
import net.minecraft.client.gui.GuiGraphics;

public class EmptyLine extends Line<EmptyLine> {
    public EmptyLine(UIWidget parent) {
        super(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
    }
}
