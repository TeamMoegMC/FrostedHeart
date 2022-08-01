package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.gui.FHIcons.FHIcon;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class EffectWidget extends Widget {
    List<ITextComponent> tooltips;
    ITextComponent title;
    FHIcon icon;
    Effect e;

    public EffectWidget(Panel panel, Effect e) {
        super(panel);
        tooltips = e.getTooltip();
        title = e.getName();
        icon = e.getIcon();
        this.e = e;
        this.setSize(16, 16);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        list.add(title);
        tooltips.forEach(list::add);
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            if (getWidgetType() != WidgetType.DISABLED) {
                //TODO edit effect
            }

            return true;
        }

        return false;
    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        GuiHelper.setupDrawing();
        TechIcons.SLOT.draw(matrixStack, x - 4, y - 4, 24, 24);
        icon.draw(matrixStack, x, y, w, h);
        if (e.isGranted()) {
            matrixStack.push();
            matrixStack.translate(0, 0, 300);
            GuiHelper.setupDrawing();
            TechIcons.FIN.draw(matrixStack, x, y, w, h);
            matrixStack.pop();
        }
    }
}
