package com.teammoeg.frostedheart.research.gui.editor;

import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;

import dev.ftb.mods.ftblibrary.ui.Widget;

public class EditUtils {

	private EditUtils() {
	}
	public static void editResearch(Widget p,Research r) {
		new ResearchEditorDialog(p,r,r.getCategory()).open();;
	}
	public static void saveResearch(Research r) {
		FHResearch.save(r);
	}

}
