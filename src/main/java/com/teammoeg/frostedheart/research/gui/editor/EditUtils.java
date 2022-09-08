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
