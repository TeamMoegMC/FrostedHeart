/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.client.cui.editor;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;

public abstract class BaseEditDialog extends EditDialog {

    public BaseEditDialog(UIElement panel) {
        super(panel);
        setWidth(300);
        setHeight(200);
    }

	@Override
    public void alignWidgets() {
        this.setHeight(super.align(false));
        //setSizeToContentSize();
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
    	CGuiHelper.drawUIBackground(matrixStack, x-5, y-5, w+10, h+10);
    }
}
