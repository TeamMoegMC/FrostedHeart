package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;
import com.teammoeg.frostedheart.research.gui.TechIcons;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.TextFormatting;

public class ResearchCategoryPanel extends Panel {
	public static final int CAT_PANEL_HEIGHT = 40;

	public ResearchPanel researchScreen;

	public ResearchCategoryPanel(ResearchPanel panel) {
		super(panel);
		researchScreen = panel;
	}

	public static class CategoryButton extends Button {

		ResearchCategory category;
		ResearchCategoryPanel categoryPanel;

		public CategoryButton(ResearchCategoryPanel panel, ResearchCategory category) {
			super(panel, category.getName(), Icon.getIcon(category.getIcon()));
			this.category = category;
			this.categoryPanel = panel;
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			categoryPanel.researchScreen.selectCategory(category);
			
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			list.add(category.getName());
			list.add(category.getDesc().mergeStyle(TextFormatting.GRAY));
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {

			//theme.drawHorizontalTab(matrixStack, x, y, w, h,categoryPanel.researchScreen.selectedCategory==category);
			
			if(categoryPanel.researchScreen.selectedCategory==category) {
				TechIcons.TAB_HL.draw(matrixStack, x, y, w, 7);
				this.drawIcon(matrixStack, theme,x + 7,y + 2, 16, 16);
			}else
				this.drawIcon(matrixStack, theme,x + 7,y + 5, 16, 16);
			//super.drawBackground(matrixStack, theme, x, y, w, h);
			
			//theme.drawString(matrixStack, category.getName(), x + (w - theme.getStringWidth(category.getName())) / 2, y + 24);
		}
	}

	@Override
	public void addWidgets() {
		int k=0;
		for (ResearchCategory r:ResearchCategories.ALL.values()) {
			CategoryButton button = new CategoryButton(this,r);
			button.setPosAndSize(k * 40,0,30,21);
			add(button);
			k++;
		}
	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.draw(matrixStack, theme, x, y, w, h);
		//drawBackground(matrixStack, theme, x, y, w, h);
	}

	@Override
	public boolean isEnabled() {
		return researchScreen.canEnable(this);
	}

}
