package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class ConfirmDialog extends BaseEditDialog{
	public static Editor<Boolean> EDITOR=(p,l,v,c)->{
		new ConfirmDialog(p,l,v,c).open();;
	};
	TextField tf;
	Button cancel;
	Button ok;
	Consumer<Boolean> fin;
	boolean selected=false;
	public ConfirmDialog(Widget panel,String label,boolean exp,Consumer<Boolean> onFinished) {
		super(panel);
		tf=new TextField(this).setColor(Color4I.RED).setMaxWidth(200).setText(label);
		fin=onFinished;
		selected=!exp;
		cancel=new SimpleTextButton(this,GuiUtils.str("Cancel"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				
				close();
			}
			
		};
		ok=new SimpleTextButton(this,GuiUtils.str("OK"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				selected=exp;
				close();
			}
			
		};
		ok.setHeight(20);
		ok.setWidth(200);
		cancel.setHeight(20);
		cancel.setWidth(200);
	}
	@Override
	public void onClosed() {
		if(!selected)
			try {
				fin.accept(selected);
			}catch(Exception ex) {
				ex.printStackTrace();
			}
	}

	@Override
	public void addWidgets() {
		add(tf);
		add(ok);
		add(cancel);
	}
	@Override
	public void onClose() {
	}

}
