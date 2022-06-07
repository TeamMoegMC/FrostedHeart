package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.gui.TechIcons;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.ITextComponent;

public class CluePanel extends Panel {
	Clue c;
	public static final String sq = "\u2610";
	public static final String sq_v = "\u2611";
	public static final String sq_x = "\u2612";
	ITextComponent hover;
	TextField clueName;
	TextField desc;
	public CluePanel(Panel panel, Clue c) {
		super(panel);
		this.c = c;

	}
	public void initWidgets() {
		int offset = 1;
		clueName= new TextField(this);
		clueName.setMaxWidth(width - 6).setText(c.getName()).setColor(TechIcons.text).setPos(10, offset);
		
		offset += clueName.height + 2;
		ITextComponent itx = c.getDescription();
		if (itx != null) {
			desc = new TextField(this);
			desc.setMaxWidth(width).setText(itx).setColor(TechIcons.text).setPos(0, offset);
			offset += desc.height + 2;
		}
		offset+=1;
		hover = c.getHint();
		this.setHeight(offset);
	}
	@Override
	public void addWidgets() {
		add(clueName);
		if(desc!=null)
		add(desc);
	}
	@Override
	public boolean mousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (getWidgetType() != WidgetType.DISABLED) {
				//TODO edit clue
			}

			return true;
		}

		return false;
	}
	@Override
	public void alignWidgets() {
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		// super.drawBackground(matrixStack, theme, x, y, w, h);
		if(c.isCompleted())
			TechIcons.CHECKBOX_CHECKED.draw(matrixStack, x, y, 9, 9);
		else
			TechIcons.CHECKBOX.draw(matrixStack, x, y, 9, 9);
	}

	@Override
	public void addMouseOverText(TooltipList arg0) {
		super.addMouseOverText(arg0);
		if(hover!=null)
			arg0.add(hover);
	}

}
