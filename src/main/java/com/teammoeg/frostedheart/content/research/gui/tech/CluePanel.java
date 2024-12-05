/*
 * Copyright (c) 2022-2024 TeamMoeg
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

import com.ibm.icu.text.NumberFormat;
import com.teammoeg.frostedheart.util.Lang;
import net.minecraft.client.gui.GuiGraphics;

import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.gui.TechIcons;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.network.chat.Component;

public class CluePanel extends Panel {
    public static final String sq = "☐";
    public static final String sq_v = "☑";
    public static final String sq_x = "☒";
    Clue c;
    Research r;
    Component hover;
    TextField clueName;
    TextField desc;
    TextField contribute;
    TextField rq;

    public CluePanel(Panel panel, Clue c, Research r) {
        super(panel);
        this.c = c;
        this.r = r;
    }

    @Override
    public void addMouseOverText(TooltipList arg0) {
        super.addMouseOverText(arg0);
        if (hover != null)
            arg0.add(hover);
    }

    @Override
    public void addWidgets() {
        add(clueName);
        if (desc != null)
            add(desc);
        if (rq != null)
            add(rq);
        add(contribute);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        // super.drawBackground(matrixStack, theme, x, y, w, h);
        if (ClientResearchDataAPI.getData().get().isClueCompleted(r, c))
            TechIcons.CHECKBOX_CHECKED.draw(matrixStack, x, y, 9, 9);
        else if (r.isCompleted())
            TechIcons.CHECKBOX_CROSS.draw(matrixStack, x, y, 9, 9);
        else
            TechIcons.CHECKBOX.draw(matrixStack, x, y, 9, 9);
    }

    public void initWidgets() {
        int offset = 1;
        clueName = new TextField(this);
        clueName.setMaxWidth(width - 6).setText(c.getName(r)).setColor(TechIcons.text).setPos(10, offset);

        offset += clueName.height + 2;
        Component itx = c.getDescription(r);
        if (itx != null) {
            desc = new TextField(this);
            desc.setMaxWidth(width).setText(itx).setColor(TechIcons.text).setPos(0, offset);
            offset += desc.height + 2;
        }
        if (c.isRequired()) {
            rq = new TextField(this)
                    .setMaxWidth(width)
                    .setText(Lang.translateGui("research.required"))
                    .setColor(TechIcons.text_red);
            rq.setPos(0, offset);
            offset += rq.height + 2;
        }
        contribute = new TextField(this)
                .setMaxWidth(width)
                .setText(Lang.str("+" + NumberFormat.getPercentInstance().format(c.getResearchContribution())))
                .setColor(TechIcons.text);
        contribute.setPos(0, offset);
        offset += contribute.height + 2;
        offset += 1;
        hover = c.getHint(r);
        this.setHeight(offset);
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (isMouseOver()) {
            if (getWidgetType() != WidgetType.DISABLED) {
                //TODO edit clue
            }

            return true;
        }

        return false;
    }

}
