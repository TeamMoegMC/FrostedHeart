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
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.TextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;


/**
 * 带标签的面板，将标签文本和一个子UI元素水平排列。
 * 是编辑器表单中各种带标签输入控件的基类。
 * <p>
 * Labeled pane arranging a label text and a child UI element horizontally.
 * Serves as the base class for various labeled input widgets in editor forms.
 *
 * @param <T> 子UI元素类型 / The child UI element type
 */
public class LabeledPane<T extends UIElement> extends UILayer {

    protected TextField label;
    protected T obj;

    public LabeledPane(UIElement panel, Component lab) {
        super(panel);
        label = new TextField(this).setMaxWidth(200).setTrim().setText(lab).setColor(theme().UITextColor());

    }

    @Override
    public void addUIElements() {
        add(label);
        add(obj);
    }

    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
    	//System.out.println("render");
		super.render(graphics, x, y, w, h, hint);
	}

	@Override
    public void alignWidgets() {
        if (getWidth()==0) setWidth(parent.getWidth());
        int w = getWidth();
        int w2 = (int)(w*0.45F);
        obj.setX(w-w2);
        obj.setWidth(w2);

        label.setPos(4, (this.getContentHeight() - 8) / 2);
        setHeight(getContentHeight());
        //System.out.println(this.getX()+","+this.getY()+":"+this.getWidth()+","+this.getHeight());
    }
}
