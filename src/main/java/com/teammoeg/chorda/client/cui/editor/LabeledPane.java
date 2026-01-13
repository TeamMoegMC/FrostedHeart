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

package com.teammoeg.chorda.client.cui.editor;

import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIElement;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;


public class LabeledPane<T extends UIElement> extends UILayer {

    protected TextField label;
    protected T obj;

    public LabeledPane(UIElement panel, Component lab) {
        super(panel);
        label = new TextField(this).setMaxWidth(200).setTrim().setText(lab).setColor(0xFF000000);

    }

    @Override
    public void addUIElements() {
        add(label);
        if (obj != null)
        add(obj);
    }

    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
    	//System.out.println("render");
		super.render(graphics, x, y, w, h);
	}

	@Override
    public void alignWidgets() {
        setSize(super.align(true), this.getContentHeight());
        
        label.setY((this.getContentHeight() - 8) / 2);
        //System.out.println(this.getX()+","+this.getY()+":"+this.getWidth()+","+this.getHeight());
    }
}
