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

import java.util.function.Consumer;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.screenadapter.CUIScreenWrapper;
import com.teammoeg.chorda.client.cui.theme.SimpleTechTheme;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.widgets.TextField;

import net.minecraft.network.chat.Component;

public class EditUtils {

    private EditUtils() {
    }

    public static <T> void edit(Editor<T> editor, Component title, T oldVlaue,Consumer<T> onChange) {
    	editor.open(openEditorScreen(), title, oldVlaue, onChange);
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    public static <T> void edit(Editor<T> editor, Theme theme, Component title, T oldVlaue,Consumer<T> onChange) {
    	editor.open(openEditorScreen(theme), title, oldVlaue, onChange);
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    public static UIElement openEditorScreen() {
    	return openEditorScreen(SimpleTechTheme.INSTANCE);
    }
    
    public static UIElement openEditorScreen(Theme theme) {
    	CUIScreenWrapper wrapper=new CUIScreenWrapper(new PrimaryLayer());
    	wrapper.getPrimaryLayer().setTheme(theme);
    	ClientUtils.getMc().setScreen(wrapper);
    	return wrapper.getPrimaryLayer();
    }
    public static TextField getTitle(UIElement p, String title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(p.getTheme().getUITextColor());
    }
    public static TextField getTitle(UIElement p, Component title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(p.getTheme().getUITextColor());
    }
  
}
