package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
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
	/*public boolean checkMouseOver(int mouseX, int mouseY) {
		if (parent == null) {
			return true;
		} else if (!parent.isMouseOver()) {
			return false;
		}

		int ax = getX();
		int ay = getY();
		return mouseX >= ax && mouseY >= ay+4 && mouseX < ax + width-8 && mouseY < ay + height-8;
	}*/
	
	@Override
	public void addMouseOverText(TooltipList list) {
		ItemStack cur=i[(int) ((System.currentTimeMillis()/1000)%i.length)];
		//list.add(cur.getDisplayName());
		cur.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL).forEach(list::add);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		ItemStack cur=i[(int) ((System.currentTimeMillis()/1000)%i.length)];
		GuiHelper.setupDrawing();
		DrawDeskIcons.SLOT.draw(matrixStack, x-4, y-4, 24,24);
		matrixStack.push();
		matrixStack.translate(0, 0, 100);
		GuiHelper.drawItem(matrixStack,cur, x, y, w/16F, h/16F,true,null);
		matrixStack.pop();
	}





}
