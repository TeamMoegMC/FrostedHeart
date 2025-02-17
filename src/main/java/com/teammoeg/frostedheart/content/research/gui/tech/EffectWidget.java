/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.research.gui.tech;

import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class EffectWidget extends Widget {
    List<Component> tooltips;
    Component title;
    CIcon icon;
    Effect e;
    Research r;

    public EffectWidget(Panel panel, Effect e, Research r) {
        super(panel);
        tooltips = e.getTooltip(r);
        title = e.getName(r);
        icon = e.getIcon();
        this.e = e;
        this.r = r;
        this.setSize(16, 16);
    }

    @Override
    public void addMouseOverText(TooltipList list) {
        list.add(title);
        tooltips.forEach(list::add);
    }

    @Override
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        GuiHelper.setupDrawing();
        TechIcons.SLOT.draw(matrixStack, x - 4, y - 4, 24, 24);
        icon.draw(matrixStack, x, y, w, h);
        if (ClientResearchDataAPI.getData().get().isEffectGranted(r, e)) {
            matrixStack.pose().pushPose();
            matrixStack.pose().translate(0, 0, 300);
            GuiHelper.setupDrawing();
            TechIcons.FIN.draw(matrixStack, x, y, w, h);
            matrixStack.pose().popPose();
        }
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            if (getWidgetType() != WidgetType.DISABLED) {
                //TODO edit effect
                e.onClick(r.getData());
            }

            return true;
        }

        return false;
    }
}
