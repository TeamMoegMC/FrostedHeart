package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetLayout;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

public class SingleEditDialog extends EditDialog{
	public static Editor<String> TEXT_EDITOR=(p,l,v,c)->{
		open(p,l,v,c);
	};
	public static Editor<Long> LONG_EDITOR=(p,l,v,c)->{
		open(p,l,String.valueOf(v),o->{
			c.accept(Long.parseLong(o));
		});
	};
	public static Editor<Integer> INT_EDITOR=(p,l,v,c)->{
		open(p,l,String.valueOf(v),o->{
			c.accept(Integer.parseInt(o));
		});
	};
	public static Editor<Double> REAL_EDITOR=(p,l,v,c)->{
		open(p,l,String.valueOf(v),o->{
			c.accept(Double.parseDouble(o));
		});
	};
	LabeledTextBox box;
	Button ok;
	public SingleEditDialog(Widget panel,String label,String val,Consumer<String> onFinished) {
		super(panel);
		box=new LabeledTextBox(this,label,val);
		ok=new SimpleTextButton(this,GuiUtils.str("OK"),Icon.EMPTY) {

			@Override
			public void onClicked(MouseButton arg0) {
				try {
					onFinished.accept(box.getText());
				}catch(Exception ex) {
					ex.printStackTrace();
				}
				close();
			}
			
		};
		ok.setHeight(20);
		ok.setWidth(200);
		setSize(400,200);
	}
	public static void open(Widget p,String l,String v,Consumer<String> f) {
		new SingleEditDialog(p,l,v,f).open();
	}
	@Override
	public void onClose() {
	}

	@Override
	public void addWidgets() {
		add(box);
		add(ok);
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
