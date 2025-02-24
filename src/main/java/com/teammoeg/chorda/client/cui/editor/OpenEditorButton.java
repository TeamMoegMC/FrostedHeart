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
import java.util.function.Supplier;

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
    private Supplier<T> val;
    private  T value;
    private final Function<T,CIcon> getIcon;
    private final Function<T,Component> getText;
    private final Component txt;
    private final Consumer<T> onset;
    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, T val,Consumer<T> onset) {
		this(panel,txt,edi,val,CIcons.nop(),onset);
	}

    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, T val,CIcon icon,Consumer<T> onset) {
    	this(panel,txt,edi,()->val,icon,onset);
	}

    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, T val, Function<T, CIcon> getIcon, Function<T, Component> getText) {
		this(panel,txt,edi,()->val,getIcon,getText);
	}
    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, Supplier<T> val,Consumer<T> onset) {
		this(panel,txt,edi,val,CIcons.nop(),onset);
	}

    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, Supplier<T> val,CIcon icon,Consumer<T> onset) {
		super(panel,txt,icon);
		this.edi = edi;
		this.val = val;
		this.getIcon = null;
		this.getText = null;
		this.txt=txt;
		this.onset=onset;
	}

    public OpenEditorButton(UIWidget panel, Component txt, Editor<T> edi, Supplier<T> val, Function<T, CIcon> getIcon, Function<T, Component> getText) {
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
    	if(getText!=null||getIcon!=null)
    		value=val.get();
    	if(getText!=null)
    		super.setTitle(getText.apply(value));
    	if(getIcon!=null)
    		super.setIcon(getIcon.apply(value));
    }

    public T getValue() {
    	return value;
    }
	@Override
    public void onClicked(MouseButton arg0) {
		value=val.get();
        edi.open(this.getParent(), txt, value, v->{
        	this.value=v;
        	onset.accept(v);
        	refreshValue();
        	
        });
    }


}
