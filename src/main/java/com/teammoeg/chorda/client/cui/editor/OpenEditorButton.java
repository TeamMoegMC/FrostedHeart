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
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.chorda.util.CFunctionUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;


/**
 * 点击后打开编辑器的按钮控件，显示当前值的文本和图标。
 * 支持自定义图标和文本的动态更新函数，编辑完成后自动刷新显示。
 * <p>
 * Button widget that opens an editor on click, displaying the current value's
 * text and icon. Supports custom dynamic icon and text update functions, and
 * automatically refreshes the display after editing completes.
 *
 * @param <T> 被编辑的值类型 / The type of value being edited
 */
public class OpenEditorButton<T> extends TextButton {
    private final Editor<T> edi;
    private T val;
    private final Function<T,CIcon> getIcon;
    private final Function<T,Component> getText;
    private final Component txt;
    private final Consumer<T> onset;
    public OpenEditorButton(UIElement panel, Component txt, Editor<T> edi, T val,Consumer<T> onset) {
		this(panel,txt,edi,val,CIcons.nop(),onset);
	}

    public OpenEditorButton(UIElement panel, Component txt, Editor<T> edi, T val,CIcon icon,Consumer<T> onset) {
		super(panel,txt,icon);
		this.edi = edi;
		this.val = val;
		this.getIcon = t-> CIcons.nop();
		this.getText = t-> txt;
		this.txt=txt;
		this.onset=onset;
		refreshValue();
	}

    public OpenEditorButton(UIElement panel, Component txt, Editor<T> edi, T val, Function<T, CIcon> getIcon, Function<T, Component> getText) {
		super(panel,Components.empty(),CIcons.nop());
		this.edi = edi;
		this.val = val;
		this.getIcon = CFunctionUtils.mapNullable(getIcon, CIcons.nop());
		this.getText = CFunctionUtils.mapNullable(getText, txt);
		this.txt=txt;
		this.onset=t->{};
		refreshValue();
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		super.render(graphics, x, y, w, h, hint);
		graphics.drawString(getFont(), ">", x+w-10, y-4+h/2,textColor.getColorARGB(this, x, y, hint), hint.theme(this).isButtonTextShadow());
	}

	private void refreshValue() {
    	super.setTitle(getText.apply(val));
    	super.setIcon(getIcon.apply(val));
    }

    public T getValue() {
    	return val;
    }
    public void setValue(T nval) {
    	this.val=nval;
    	refreshValue();
    }
	@Override
    public void onClicked(MouseButton arg0) {
        edi.open(this.getParent(), txt, val, v->{
        	this.val=v;
        	onset.accept(v);
        	refreshValue();
        	
        });
    }


}

