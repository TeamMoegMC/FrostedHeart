package com.teammoeg.frostedheart.research.screen;

import static com.teammoeg.frostedheart.research.screen.ResearchCategoryPanel.CAT_PANEL_HEIGHT;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.ResearchCategories;
import com.teammoeg.frostedheart.research.ResearchCategory;
import com.teammoeg.frostedheart.research.ResearchLevel;

import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.entity.player.PlayerEntity;

public class ResearchDetailScreen extends BaseScreen {

    public static final int PADDING = 2;
    private PlayerEntity player;
    public Research selectedResearch;

    public ResearchDetailScreen(PlayerEntity player, Research r) {
        this.player = player;
        this.selectedResearch=r;
    }

    @Override
    public void addWidgets() {
    }

    @Override
    public boolean onInit() {
    	int x=(this.getScreen().getScaledWidth()-200)/2;
    	int y=(this.getScreen().getScaledHeight()-200)/2;
    	super.setPosAndSize(x, y, 200, 200);
        return true;
    }
    @Override
    public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
    	theme.drawString(matrixStack,selectedResearch.getName(), x+10, y+3);
        super.drawBackground(matrixStack, theme, x, y, w, h);
        
    }
}
