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

package com.teammoeg.chorda.client.cui.widgets;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

/**
 * 标签页图像按钮控件。用于标签页切换的按钮，根据当前激活的标签页显示不同图标。
 * 当此按钮对应的标签页处于激活状态时显示激活图标，否则显示非激活图标。
 * <p>
 * Tab image button widget. A button used for tab switching that displays different icons
 * based on the currently active tab. Shows the active icon when this button's tab is
 * selected, and the inactive icon otherwise.
 */
public abstract class TabImageButtonElement extends Button{
    /** 此按钮对应的标签页索引 / Tab index this button corresponds to */
    final int tab;
    /** 当前激活标签页索引的供应器 / Supplier of the currently active tab index */
    Supplier<Integer> currentTab;
    /** 非激活状态图标 / Inactive state icon */
    private final CIcons.CIcon inactiveIcon;

    /**
     * 创建标签页图像按钮。
     * <p>
     * Creates a tab image button.
     *
     * @param parent 父级UI元素 / Parent UI element
     * @param xIn X坐标 / X coordinate
     * @param yIn Y坐标 / Y coordinate
     * @param widthIn 按钮宽度 / Button width
     * @param heightIn 按钮高度 / Button height
     * @param tab 对应的标签页索引 / Corresponding tab index
     * @param icon1 激活状态图标 / Active state icon
     * @param icon2 非激活状态图标 / Inactive state icon
     */
    public TabImageButtonElement(UIElement parent, int xIn, int yIn, int widthIn, int heightIn, int tab,
                          CIcons.CIcon icon1, CIcons.CIcon icon2) {
        super(parent);
        setPos(xIn, yIn);
        this.tab=tab;
        super.setIcon(icon1);
        this.inactiveIcon = icon2;
        this.setWidth(widthIn);
        this.setHeight(heightIn);
    }

    /**
     * 标签页按钮不自动调整尺寸（空实现）。
     * <p>
     * Tab button does not auto-fit size (no-op implementation).
     */
    protected void fitSize() {

    }

    /**
     * 绑定当前激活标签页索引的供应器。
     * <p>
     * Binds a supplier for the currently active tab index.
     *
     * @param supp 标签页索引供应器 / Tab index supplier
     * @return 当前实例（链式调用） / This instance (for chaining)
     */
    public TabImageButtonElement bind(Supplier<Integer> supp) {
        this.currentTab=supp;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        int current=0;
        if(currentTab!=null)
            current=currentTab.get();

        if (tab == current) {
            drawIcon(graphics, x, y, w, h);
        }
        else inactiveIcon.draw(graphics, x, y, w, h);

    }
}
