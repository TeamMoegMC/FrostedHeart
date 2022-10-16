/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research.gui.editor;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchEditorDialog;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;

public class EditUtils {

    private EditUtils() {
    }

    public static void editResearch(Widget p, Research r) {
    	if(r!=null) {
    		FHResearch.load(r);
    	}
        new ResearchEditorDialog(p, r, r.getCategory()).open();
        ;
    }

    public static void saveResearch(Research r) {
    	r.doIndex();
        FHResearch.save(r);
    }

    public static TextField getTitle(Panel p, String title) {
        TextField tf = new TextField(p).setMaxWidth(200).setText(title);

        return tf;
    }
}
