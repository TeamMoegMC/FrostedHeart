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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.cui.base.UIElement;

import lombok.Getter;

public class UIEleWrapperLine extends Line<UIEleWrapperLine> {
    @Getter
    private final UIElement wrappedEle;

    public UIEleWrapperLine(UIElement parent, UIElement wrappedElement) {
        super(parent);
        this.wrappedEle = wrappedElement;
        elements.add(wrappedElement);
    }

    @Override
    public void refresh() {
        super.refresh();
        wrappedEle.setWidth(getWidth());
        wrappedEle.refresh();
        if (wrappedEle.getHeight() == 0) {
            wrappedEle.setHeight(16);
        }
        setHeight(wrappedEle.getHeight());
    }
}
