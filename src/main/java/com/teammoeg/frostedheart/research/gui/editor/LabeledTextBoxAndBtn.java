package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class LabeledTextBoxAndBtn extends LabeledTextBox {
	Button btn;
	public LabeledTextBoxAndBtn(Panel panel, String lab, String txt,String btn,Consumer<Consumer<String>> onbtn) {
		super(panel, lab, txt);
		this.btn=new SimpleTextButton(this,GuiUtils.str(btn),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				onbtn.accept(s->obj.setText(s));
			}};
	}
	@Override
	public void addWidgets() {
		
		add(label);
		add(obj);
		add(btn);
		setSize(super.align(WidgetLayout.HORIZONTAL),20);
	}
}
