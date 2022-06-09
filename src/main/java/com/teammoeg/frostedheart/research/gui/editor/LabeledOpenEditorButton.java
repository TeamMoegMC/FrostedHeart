package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;

public class LabeledOpenEditorButton<T> extends LabeledPane<OpenEditorButton<T>> {

	public LabeledOpenEditorButton(Panel panel,String master,String label,Editor<T> e,T val,Consumer<T> cb) {
		super(panel,master);
		obj=new OpenEditorButton<T>(this,label,e,val,cb);
	}
	public LabeledOpenEditorButton(Panel panel,String master,String label,Editor<T> e,T val,Icon ic,Consumer<T> cb) {
		super(panel,master);
		obj=new OpenEditorButton<T>(this,label,e,val,ic,cb);
	}
}
