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

package com.teammoeg.frostedheart.content.research.gui.editor;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;

public class LabeledPane<T extends Widget> extends Panel {

    protected TextField label;
    protected T obj;

    public LabeledPane(Panel panel, String lab) {
        super(panel);
        label = new TextField(this).setMaxWidth(200).setTrim().setText(lab).setColor(Color4I.BLACK);

    }

    @Override
    public void addWidgets() {
        add(label);
        if (obj != null) ;
        add(obj);
    }

    @Override
    public void alignWidgets() {
        setSize(super.align(WidgetLayout.HORIZONTAL), 20);
    }
}
