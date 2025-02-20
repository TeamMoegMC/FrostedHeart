package com.teammoeg.frostedheart.content.research.gui.editor;

import com.teammoeg.chorda.client.cui.UIWidget;

import dev.ftb.mods.ftblibrary.ui.Panel;

public interface EditorManager {
	public void openDialog(EditDialog previous,boolean refresh);
	public void closeDialog(boolean refresh);
	public void closeGui();
	public EditDialog getDialog();
}
