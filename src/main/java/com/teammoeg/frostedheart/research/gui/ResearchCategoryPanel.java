package com.teammoeg.frostedheart.research.gui;

import static com.teammoeg.frostedheart.research.gui.ResearchScreen.PADDING;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class ResearchCategoryPanel extends Panel {
	public static final int CAT_PANEL_HEIGHT = 40;

	public ResearchScreen researchScreen;

	public ResearchCategoryPanel(ResearchScreen panel) {
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
			if (FHResearch.getFirstResearchInCategory(category) != null)
				categoryPanel.researchScreen.selectResearch(FHResearch.getFirstResearchInCategory(category));
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			list.add(category.getDesc());
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			setSize(w, h);
			
			//theme.drawHorizontalTab(matrixStack, x, y, w, h,categoryPanel.researchScreen.selectedCategory==category);
			if(categoryPanel.researchScreen.selectedCategory==category)
				Extrawidgets.TAB_V_SELECTED.draw(matrixStack, x, y-3, w, h+2);
			else
				Extrawidgets.TAB_V_UNSELECTED.draw(matrixStack, x, y-3, w, h);
			//super.drawBackground(matrixStack, theme, x, y, w, h);
			this.drawIcon(matrixStack, theme, x + (w - 16) / 2, y + 4, 16, 16);
			theme.drawString(matrixStack, category.getName(), x + (w - theme.getStringWidth(category.getName())) / 2, y + 24);
		}
	}

	@Override
	public void addWidgets() {
		int k=0;
		for (ResearchCategory r:ResearchCategories.ALL.values()) {
			CategoryButton button = new CategoryButton(this,r);
			button.setPosAndSize(posX + k * (width / 5), posY, width / 5, height - PADDING * 2);
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
		drawBackground(matrixStack, theme, x, y, w, h);
	}

}
