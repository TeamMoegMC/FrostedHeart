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

package com.teammoeg.frostedheart.content.research.gui.editor;

import com.teammoeg.chorda.client.cui.PrimaryLayer;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.cui.CUIScreen;
import com.teammoeg.chorda.client.cui.TextField;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.ResearchEditorDialog;

import dev.ftb.mods.ftblibrary.ui.Widget;

public class EditUtils {

    private EditUtils() {
    }

    public static void editResearch(Widget techTextButton, Research r) {
        if (r != null) {
            r=FHResearch.load(r);
        }
        new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    public static UIElement openEditorScreen() {
    	CUIScreen wrapper=new CUIScreen();
    	wrapper.setPrimaryLayer(new PrimaryLayer(wrapper));
    	
    	ClientUtils.mc().setScreen(wrapper);
    	return wrapper.getPrimaryLayer();
    }
    public static TextField getTitle(UIElement p, String title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(0xFFFFFFFF);
    }

    public static void saveResearch(Research r) {
        r.doIndex();
        FHResearch.save(r);
    }
}
