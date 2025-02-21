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

public class ResearchEditUtils {

    private ResearchEditUtils() {
    }

    public static void editResearch(Widget techTextButton, Research r) {
        if (r != null) {
            r=FHResearch.load(r);
        }else {
        	r=new Research();
        }
        final Research old=r;
        ResearchEditors.RESEARCH_EDITOR.open(ResearchEditUtils.openEditorScreen(), Components.str("Edit Research"), r, b->{
        	if(!b.equals(old)) {
        		System.out.println("modified");
        		if(old!=null)
        			old.delete();
				b.getChildren().forEach(t->t.addParent(b));
				ResearchEditUtils.saveResearch(b);
	            
	            FHResearch.load(b);
	            FHResearch.reindex();
	            
        	}
        });
        //new ResearchEditorDialog(EditUtils.openEditorScreen(), r, r.getCategory()).open();
    }
    public static UIWidget openEditorScreen() {
    	CUIScreen wrapper=new CUIScreen(new PrimaryLayer());
    	
    	ClientUtils.mc().setScreen(wrapper);
    	return wrapper.getPrimaryLayer();
    }
    public static TextField getTitle(UIWidget p, String title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(0xFFFFFFFF);
    }
    public static TextField getTitle(UIWidget p, Component title) {

        return new TextField(p).setMaxWidth(200).setText(title).setColor(0xFFFFFFFF);
    }
    public static void saveResearch(Research r) {
        r.doIndex();
        FHResearch.save(r);
    }
  
}
