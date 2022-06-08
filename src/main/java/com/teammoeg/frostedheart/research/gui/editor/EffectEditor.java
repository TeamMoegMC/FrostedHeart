package com.teammoeg.frostedheart.research.gui.editor;

import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.effects.Effect;

import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;

public class EffectEditor extends EditDialog {
	String lbl;
	Effect e;
	Consumer<Effect> cb;

	public EffectEditor(Widget panel, String lbl, Effect e, Consumer<Effect> cb) {
		super(panel);
		this.lbl = lbl;
		this.e = e;
		this.cb = cb;
	}

	@Override
	public void onClose() {
	}

	@Override
	public void addWidgets() {
	}

	@Override
	public void alignWidgets() {
		int offset=5;
		for(Widget w:this.widgets) {
			w.setPos(5, offset);
			offset+=w.height+1;
		}
		setHeight(offset+5);
	}


	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		theme.drawGui(matrixStack, x, y, w, h,WidgetType.NORMAL);
	}
}
