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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIWidget;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.ResearchEditors;

import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.network.chat.Component;

public class EditUtils {

    private EditUtils() {
    }

    public static <T> void edit(Editor<T> editor,Component title, T oldVlaue,Consumer<T> onChange) {
    	editor.open(openEditorScreen(), title, oldVlaue, onChange);
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    public static UIWidget openEditorScreen() {
    	CUIScreen wrapper=new CUIScreen(new PrimaryLayer());
    	
    	ClientUtils.mc().setScreen(wrapper);
    	return wrapper.getPrimaryLayer();
    }
    public static TextField getTitle(UIWidget p, String title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(0xFF000000);
    }
    public static TextField getTitle(UIWidget p, Component title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(0xFF000000);
    }
  
}
