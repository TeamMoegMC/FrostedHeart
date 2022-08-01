package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;

public class ThickLine {
    int x, y, x2, y2;

    // ensure w and h is positive
    public void setPosAndDelta(int x, int y, int dx, int dy) {
        this.x = x;
        this.y = y;
        this.x2 = x + dx;
        this.y2 = y + dy;
    }

    // ensure w and h is positive
    public void setPoints(int x, int y, int x2, int y2) {
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
    }

    public ThickLine() {
    }

    public Color4I color = Color4I.BLACK;

    public void draw(MatrixStack matrixStack, int x, int y) {
        FHGuiHelper.drawLine(matrixStack, color, x + this.x, y + this.y, x + this.x2, y + this.y2);

        // super.draw(matrixStack, theme, x, y, w, h);
    }

}
