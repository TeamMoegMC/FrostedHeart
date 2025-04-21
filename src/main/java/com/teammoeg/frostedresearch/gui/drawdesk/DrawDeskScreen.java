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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.cui.editor.EditDialog;
import com.teammoeg.chorda.client.ui.ScreenAcceptor;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.ResearchGui;
import com.teammoeg.frostedresearch.gui.tech.ResearchPanel;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class DrawDeskScreen extends BaseScreen implements ResearchGui, ScreenAcceptor {
    DrawDeskContainer cx;
    DrawDeskPanel p;
    ResearchPanel r;
    EditDialog dialog;
    @Setter
    AbstractContainerScreen screen;
    public DrawDeskScreen(DrawDeskContainer cx) {
        super();
        this.cx = cx;
        p = new DrawDeskPanel(this);
        p.setEnabled(true);
        this.screen=screen;
    }

    @Override
	public boolean shouldAddMouseOverText() {
		return true;
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		if (this.cx.getCarried().isEmpty() && screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().hasItem()) {
			AbstractContainerScreen.getTooltipFromItem(ClientUtils.mc(), screen.getSlotUnderMouse().getItem()).forEach(list::add);
		}
		super.addMouseOverText(list);
	}

	@Override
    public void addWidgets() {
        if (p != null && p.isEnabled())
            add(p);
        if (r != null && r.isEnabled())
            add(r);
        //if (getDialog() != null)
        //    add(getDialog());
    }


    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
    }


    public DrawingDeskTileEntity getTile() {
        return cx.getBlock();
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
    public void closeDialog(boolean refresh) {
        this.dialog = null;
        r.setEnabled(true);
        if (refresh)
            this.refreshWidgets();
    }

    public EditDialog getDialog() {
        return dialog;
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
