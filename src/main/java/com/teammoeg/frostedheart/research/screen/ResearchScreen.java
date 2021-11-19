package com.teammoeg.frostedheart.research.screen;

import java.util.ArrayList;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.frostedheart.research.DefaultResearches;
import com.teammoeg.frostedheart.research.ResearchCategory;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.entity.player.PlayerEntity;

public class ResearchScreen extends BaseScreen {

    private PlayerEntity player;
    public ResearchCategoryPanel researchCategoryPanel;
    public ArrayList<ResearchCategory> categories = new ArrayList<>();
    public ResearchCategory selectedCategory;


    public ResearchScreen(PlayerEntity player) {
        this.player = player;
        researchCategoryPanel = new ResearchCategoryPanel(this);
        System.out.println(DefaultResearches.HEATING.getId());
        categories.add(DefaultResearches.HEATING);
        if (!categories.isEmpty()) {
            selectCategory(categories.get(0));
        }
    }

    @Override
    public void addWidgets() {
        add(researchCategoryPanel);
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
    }

    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(matrixStack, theme, x, y, w, h);

        int pw = 20;

        Color4I backgroundColor = Color4I.DARK_GRAY.withAlpha(128);
        RenderSystem.enableBlend();
        backgroundColor.draw(matrixStack, x, y, w, h);
        RenderSystem.disableBlend();
    }

    public void selectCategory(@Nullable ResearchCategory category) {
        if (selectedCategory != category) {
            selectedCategory = category;
            researchCategoryPanel.refreshWidgets();
        }
    }

//    @Override
//    public void drawForeground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
//        Color4I borderColor = Color4I.WHITE;
//        GuiHelper.drawHollowRect(matrixStack, x, y, w, h, borderColor, false);
//        super.drawForeground(matrixStack, theme, x, y, w, h);
//    }
}
