package com.teammoeg.frostedheart.content.research.gui.editor;

import dev.ftb.mods.ftblibrary.ui.Panel;

public interface EditorManager {
	public void openDialog(Panel panel,boolean refresh);
	public void closeDialog(boolean refresh);
	public void closeGui();
	public EditDialog getDialog();
}
