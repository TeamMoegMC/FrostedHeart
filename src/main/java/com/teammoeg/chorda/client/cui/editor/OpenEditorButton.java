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


import java.util.function.Consumer;
import java.util.function.Function;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TextButton;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.util.CFunctionHelper;

import net.minecraft.network.chat.Component;

public class OpenEditorButton<T> extends TextButton {
    private final Editor<T> edi;
    private T val;
    private final Function<T,CIcon> getIcon;
    private final Function<T,Component> getText;
    private final Component txt;
    private final Consumer<T> onset;
    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, T val,Consumer<T> onset) {
		this(panel,txt,edi,val,CIcons.nop(),onset);
	}

    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, T val,CIcon icon,Consumer<T> onset) {
		super(panel,txt,icon);
		this.edi = edi;
		this.val = val;
		this.getIcon = t-> CIcons.nop();
		this.getText = t->txt;
		this.txt=txt;
		this.onset=onset;
	}

    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, T val, Function<T, CIcon> getIcon, Function<T, Component> getText) {
		super(panel,Components.empty(),CIcons.nop());
		this.edi = edi;
		this.val = val;
		this.getIcon = CFunctionHelper.mapNullable(getIcon, CIcons.nop());
		this.getText = CFunctionHelper.mapNullable(getText, Components.empty());
		this.txt=txt;
		this.onset=t->{};
		refreshValue();
	}
    private void refreshValue() {
    	super.setTitle(getText.apply(val));
    	super.setIcon(getIcon.apply(val));
    }

    public T getValue() {
    	return val;
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
