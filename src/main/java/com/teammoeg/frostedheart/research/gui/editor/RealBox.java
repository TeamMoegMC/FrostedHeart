package com.teammoeg.frostedheart.research.gui.editor;

import dev.ftb.mods.ftblibrary.ui.Panel;

public class RealBox extends LabeledTextBox {
	
	public RealBox(Panel panel, String lab,double val) {
		super(panel, lab,String.valueOf(val));
	}
	public void setNum(double number) {
		super.setText(String.valueOf(number));
	};
	public double getNum() {
		try {
			return Double.parseDouble(getText());
		}catch(NumberFormatException ex) {
			
		}
		return Double.parseDouble(orig);
	};
}
