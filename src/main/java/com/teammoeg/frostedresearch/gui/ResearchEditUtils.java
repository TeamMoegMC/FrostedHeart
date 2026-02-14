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

package com.teammoeg.frostedresearch.gui;

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.editor.EditUtils;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.ResearchCategory;
import com.teammoeg.frostedresearch.research.ResearchEditors;

public class ResearchEditUtils {

    private ResearchEditUtils() {
    }
    public static void editResearch(UIElement techTextButton, Research r) {
    	editResearch(techTextButton,r,null);
    }
    public static void editResearch(UIElement techTextButton, Research r,ResearchCategory categoryPeferred) {
        if (r != null) {
            r=FHResearch.load(r);
        }else {
        	r=new Research();
        	if(categoryPeferred!=null)
        		r.setCategory(categoryPeferred);
        }
        final Research old=r;
        ResearchEditors.RESEARCH_EDITOR.open(EditUtils.openEditorScreen(DrawDeskTheme.INSTANCE), Components.str("Edit Research"), r, b->{
        	if(!b.equals(old)) {
        		//System.out.println("modified");
        		if(old!=null)
        			old.delete();
				b.getChildren().forEach(t->{
					if(!t.hasParent(b)) {
						t.addParent(b);
						ResearchEditUtils.saveResearch(t);
					}
				});
				ResearchEditUtils.saveResearch(b);
	            
	            FHResearch.load(b);
	            FHResearch.reindex();
	            
        	}
        });
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    public static void saveResearch(Research r) {
        r.doIndex();
        FHResearch.save(r);
    }
  
}
