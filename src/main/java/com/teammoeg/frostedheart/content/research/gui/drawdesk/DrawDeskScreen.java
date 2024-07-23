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

package com.teammoeg.frostedheart.content.research.gui.drawdesk;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.research.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedheart.content.research.gui.ResearchGui;
import com.teammoeg.frostedheart.content.research.gui.editor.EditDialog;
import com.teammoeg.frostedheart.content.research.gui.tech.ResearchPanel;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;

public class DrawDeskScreen extends BaseScreen implements ResearchGui {
    DrawDeskContainer cx;
    DrawDeskPanel p;
    ResearchPanel r;
    EditDialog dialog;

    public DrawDeskScreen(DrawDeskContainer cx) {
        super();
        this.cx = cx;
        p = new DrawDeskPanel(this);
        p.setEnabled(true);

    }

    @Override
    public void addWidgets() {
        if (p != null && p.isEnabled())
            add(p);
        if (r != null && r.isEnabled())
            add(r);
        if (getDialog() != null)
            add(getDialog());
    }

    public void closeDialog(boolean refresh) {
        this.dialog = null;
        r.setEnabled(true);
        if (refresh)
            this.refreshWidgets();
    }

    @Override
    public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
    }

    public EditDialog getDialog() {
        return dialog;
    }

    public DrawingDeskTileEntity getTile() {
        return cx.tile;
    }

    public void hideTechTree() {
        p.setEnabled(true);
        r.setEnabled(false);
        cx.setEnabled(true);
        this.refreshWidgets();
    }

    @Override
    public boolean onInit() {
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
        return super.onInit();
    }

    public void openDialog(EditDialog dialog, boolean refresh) {
        this.dialog = dialog;
        r.setEnabled(false);
        if (refresh)
            this.refreshWidgets();
    }

    public void showTechTree() {
        if (r == null) {
            r = new ResearchPanel(this) {
                @Override
                public void onDisabled() {
                    hideTechTree();
                }
            };
            r.setPos(0, 0);
        }
        r.setEnabled(true);
        p.setEnabled(false);
        cx.setEnabled(false);
        this.refreshWidgets();

    }

}
