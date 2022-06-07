package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.gui.TechIcons;

import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

public class RequirementSlot extends Widget {
	ItemStack[] i;
	public RequirementSlot(Panel panel,ItemStack... i) {
		super(panel);
		this.i=i;
		this.setSize(16,16);
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		ItemStack cur=i[(int) ((System.currentTimeMillis()/1000)%i.length)];
		//list.add(cur.getDisplayName());
		cur.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL).forEach(list::add);
	}
	@Override
	public boolean mousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (getWidgetType() != WidgetType.DISABLED) {
				//TODO edit ingredient
			}

			return true;
		}

		return false;
	}
	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		ItemStack cur=i[(int) ((System.currentTimeMillis()/1000)%i.length)];
		GuiHelper.setupDrawing();
		TechIcons.SLOT.draw(matrixStack, x-4, y-4, 24,24);
		matrixStack.push();
		matrixStack.translate(0, 0, 100);
		GuiHelper.drawItem(matrixStack,cur, x, y, w/16F, h/16F,true,null);
		matrixStack.pop();
	}





}
