package com.teammoeg.frostedheart.research.gui.editor;

import com.teammoeg.frostedheart.research.gui.drawdesk.DrawDeskScreen;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.Key;

public abstract class EditDialog extends Panel {
	EditDialog previous;
	DrawDeskScreen sc;
	public EditDialog(DrawDeskScreen panel) {
		super(panel);
		sc=panel;
	}
	public void open() {
		previous=sc.getDialog();
		sc.openDialog(this);
	}
	public void close() {
		onClose();
		
		if(previous!=null) {
			sc.closeDialog(false);
			sc.openDialog(previous);
		}else
			sc.closeDialog(true);
	}
	public abstract void onClose();
	@Override
	public boolean keyPressed(Key key) {
		if(key.esc()) {
			
			//this.closeGui(true);
			return true;
		}
		return super.keyPressed(key);
	}
}
