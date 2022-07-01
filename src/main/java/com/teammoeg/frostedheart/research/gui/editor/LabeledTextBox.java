package com.teammoeg.frostedheart.research.gui.editor;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextBox;

public class LabeledTextBox extends LabeledPane<TextBox> {
	String orig;
	public LabeledTextBox(Panel panel,String lab,String txt) {
		super(panel,lab);
		obj=new TextBox(this);
		
		obj.allowInput();
		if(txt==null)txt="";
		obj.setText(txt);
		obj.setSize(200, 16);
		orig=txt;
		
	}
	public String getText() {
		return obj.getText();
	}
	public void setText(String s) {
		obj.setText(s);
	}
}
