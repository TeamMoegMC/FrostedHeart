package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class ConfirmDialog extends EditDialog{
	public static Editor<Boolean> EDITOR=(p,l,v,c)->{
		new ConfirmDialog(p,l,v,c).open();;
	};
	TextField tf;
	Button cancel;
	Button ok;
	public ConfirmDialog(Widget panel,String label,boolean exp,Consumer<Boolean> onFinished) {
		super(panel);
		tf=new TextField(this).setColor(Color4I.RED).setMaxWidth(200).setText(label);
		cancel=new SimpleTextButton(this,GuiUtils.str("Cancel"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				close();
			}
			
		};
		ok=new SimpleTextButton(this,GuiUtils.str("OK"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				try {
					onFinished.accept(exp);
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				close();
			}
			
		};
		ok.setHeight(20);
		ok.setWidth(200);
		cancel.setHeight(20);
		cancel.setWidth(200);
		setSize(400,200);
	}
	@Override
	public void onClose() {
	}

	@Override
	public void addWidgets() {
		
		add(ok);
		add(cancel);
	}

	@Override
	public void alignWidgets() {
		setHeight(super.align(WidgetLayout.VERTICAL));
	}
	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}

}
