package com.teammoeg.frostedheart.research.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.ResearchCategory;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.util.text.StringTextComponent;

public class ResearchCategoryPanel extends Panel {
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
            setSize(36, 36);
        }

        @Override
        public void onClicked(MouseButton mouseButton) {
            categoryPanel.researchScreen.selectCategory(category);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(category.getName());
            list.add(category.getDesc());
        }
    }

    @Override
    public void addWidgets() {
        if (!researchScreen.categories.isEmpty()) {
            for (ResearchCategory category : researchScreen.categories) {
                add(new CategoryButton(this, category));
            }
        }
    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        theme.drawString(matrixStack, new StringTextComponent("The Winter Rescue"), parent.width / 2, parent.height / 10, Color4I.WHITE, 0);
        for (Widget widget : widgets) {
            widget.draw(matrixStack, theme, x + 50, y + 50, w, h);
        }
    }
}

