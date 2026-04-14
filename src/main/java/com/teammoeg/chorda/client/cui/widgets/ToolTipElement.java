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


import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;

/**
 * 提示框元素控件。一个不可见的UI元素，仅用于在悬停时提供自定义提示信息。
 * <p>
 * Tooltip element widget. An invisible UI element used solely to provide
 * custom tooltip information on hover.
 */
public class ToolTipElement extends UIElement {
    /** 提示信息构建回调 / Tooltip builder callback */
    Consumer<TooltipBuilder> csm;

    /**
     * 创建提示框元素。
     * <p>
     * Creates a tooltip element.
     *
     * @param p 父级UI元素 / Parent UI element
     * @param csm 提示信息构建回调 / Tooltip builder callback
     */
    public ToolTipElement(UIElement p, Consumer<TooltipBuilder> csm) {
        super(p);
        this.csm = csm;
    }

    /** {@inheritDoc} */
    @Override
    public void getTooltip(TooltipBuilder list) {
        super.getTooltip(list);
        if(csm!=null)
        	csm.accept(list);
    }


}
