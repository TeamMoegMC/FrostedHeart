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

import com.teammoeg.chorda.client.cui.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.editor.EditDialog;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.ResearchGui;
import com.teammoeg.frostedresearch.gui.tech.ResearchPanel;

public class DrawDeskScreen extends MenuPrimaryLayer<DrawDeskContainer> implements ResearchGui {
    DrawDeskPanel p;
    ResearchPanel r;
    EditDialog dialog;
    public DrawDeskScreen(DrawDeskContainer cx) {
        super(cx);
        p = new DrawDeskPanel(this);
        p.setVisible(true);
    }



	@Override
    public void addUIElements() {
        add(p);
        if (r != null)
            add(r);
        if (getDialog() != null)
            add(getDialog());
    }


	@Override
	public void setSizeToContentSize() {
	}

    public DrawingDeskTileEntity getTile() {
        return container.getBlock();
    }

    public void hideTechTree() {
        p.setVisible(true);
        r.setVisible(false);
        container.setEnabled(true);
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
        r.setVisible(true);
        if (refresh)
            this.refreshElements();
    }

    public EditDialog getDialog() {
        return dialog;
    }
    public void openDialog(EditDialog dialog, boolean refresh) {
        this.dialog = dialog;
        r.setVisible(false);
        if (refresh)
            this.refreshElements();
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
            add(r);
            this.refreshElements();
        }
        r.setVisible(true);
        p.setVisible(false);
        container.setEnabled(false);

    }



}
