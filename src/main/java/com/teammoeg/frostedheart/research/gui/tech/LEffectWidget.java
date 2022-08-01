package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class LEffectWidget extends EffectWidget {

    public LEffectWidget(Panel panel, Effect e) {
        super(panel, e);
        super.setSize(36, 36);
    }

    public boolean checkMouseOver(int mouseX, int mouseY) {
        if (parent == null) {
            return true;
        } else if (!parent.isMouseOver()) {
            return false;
        }

        int ax = getX();
        int ay = getY();
        return mouseX >= ax + 2 && mouseY >= ay + 2 && mouseX < ax + width - 4 && mouseY < ay + height - 4;
    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        GuiHelper.setupDrawing();
        TechIcons.LSLOT.draw(matrixStack, x, y, w, h);
        icon.draw(matrixStack, x + 2, y + 2, w - 4, h - 4);
        if (e.isGranted()) {
            matrixStack.push();
            matrixStack.translate(0, 0, 300);
            GuiHelper.setupDrawing();
            TechIcons.FIN.draw(matrixStack, x + 2, y + 2, 32, 32);
            matrixStack.pop();
        }
    }
}
