package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.util.text.ITextComponent;

public abstract class TechButton extends Button {

	public TechButton(Panel panel) {
		super(panel);
	}

	public TechButton(Panel panel, ITextComponent t, Icon i) {
		super(panel, t, i);
	}

	@Override
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		GuiHelper.setupDrawing();
		DrawDeskIcons.drawTexturedRect(matrixStack, x, y, w, h, isMouseOver());
	}



}
