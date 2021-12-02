package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;

public class ResearchCategoryPanel extends Panel {
	public static final int CATEGORY_WIDTH = 100, CATEGORY_HEIGHT = 22;
	public static final int CAT_ICON_WIDTH = 18, CAT_ICON_HEIGHT = 18;
	public static final int CAT_PANEL_WIDTH = 374, CAT_PANEL_HEIGHT = 40;

	public ResearchScreen researchScreen;

	public ResearchCategoryPanel(Panel panel) {
		super(panel);
		researchScreen = (ResearchScreen) panel.getGui();
	}

	public static class CategoryButton extends Button {

		ResearchCategory category;
		ResearchCategoryPanel categoryPanel;

		public CategoryButton(Panel panel, ResearchCategory category) {
			super(panel, category.getName(), Icon.getIcon(category.getIcon()));
			this.category = category;
			this.categoryPanel = (ResearchCategoryPanel) panel;
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			categoryPanel.researchScreen.selectCategory(category);
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			list.add(category.getDesc());
		}

		@Override
		public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
			w=theme.getStringWidth(category.getName())+CAT_ICON_WIDTH+6;
			setWidth(w);
			super.drawBackground(matrixStack, theme, x, y, w, h);
            //theme.drawHorizontalTab(matrixStack, x, y, w, h, categoryPanel.researchScreen.selectedCategory == category);
			
			this.drawIcon(matrixStack, theme, x + 2, y + 2, CAT_ICON_WIDTH, CAT_ICON_HEIGHT);
			theme.drawString(matrixStack, category.getName(), x + CAT_ICON_WIDTH+4, y + (CATEGORY_HEIGHT-theme.getFontHeight())/ 2);
		}
	}

	@Override
	public void addWidgets() {
		for (int k = 0; k < ResearchCategories.ALL.size(); k++) {
			CategoryButton button = new CategoryButton(this, ResearchCategories.ALL.get(k));
			button.setPosAndSize(posX + k * (CATEGORY_WIDTH + 4),posY, CATEGORY_WIDTH, CATEGORY_HEIGHT);
			add(button);
		}
	}

	@Override
	public void alignWidgets() {

	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
		boolean renderInside = getOnlyRenderWidgetsInside();

		drawBackground(matrixStack, theme, x, y, w, h);

		if (renderInside) {
			GuiHelper.pushScissor(getScreen(), x, y, w, h);
		}

		setOffset(true);
		drawOffsetBackground(matrixStack, theme, x, y, w, h);
		int currentX=widgets.get(0).posX;
		for (int i = 0; i < widgets.size(); i++) {
			Widget widget = widgets.get(i);
			widget.setX(currentX);
			if (widget.shouldDraw() && (!renderInside || widget.collidesWith(x, y, w, h))) {
				drawWidget(matrixStack, theme, widget, i, x, y, w, h);
				currentX+=widget.width;
			}
		}

		setOffset(false);

		if (renderInside) {
			GuiHelper.popScissor(getScreen());
		}
	}

}
