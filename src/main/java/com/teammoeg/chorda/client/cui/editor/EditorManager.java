package com.teammoeg.chorda.client.cui.editor;

public interface EditorManager {
	public void openDialog(EditDialog previous,boolean refresh);
	public void closeDialog(boolean refresh);
	public void closeGui();
	public EditDialog getDialog();
}
