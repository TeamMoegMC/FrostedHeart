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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 基础编辑对话框，提供带背景绘制和垂直自动布局的对话框基类。
 * <p>
 * Base edit dialog providing a dialog base class with background drawing
 * and automatic vertical layout.
 */
public abstract class BaseEditDialog extends EditDialog {

    /**
     * 创建一个基础编辑对话框，默认尺寸为300x200。
     * <p>
     * Creates a base edit dialog with default size 300x200.
     *
     * @param panel 父UI元素 / the parent UI element
     */
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
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h, RenderingHint hint) {
    	hint.theme(this).drawUIBackground(matrixStack, x-5, y-5, w+10, h+10);
    }
}
