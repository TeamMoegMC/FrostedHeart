package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.UIWidget;
import net.minecraft.client.gui.GuiGraphics;

public class EmptyLine extends Line<EmptyLine> {
    public EmptyLine(UIWidget parent) {
        this(parent, 8);
    }

    public EmptyLine(UIWidget parent, int height) {
        super(parent);
        setHeight(height);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        super.render(graphics, x, y, w, h);
    }
}
