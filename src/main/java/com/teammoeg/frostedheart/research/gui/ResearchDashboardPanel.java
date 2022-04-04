package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.research.ResearchData;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import static com.teammoeg.frostedheart.research.gui.ResearchDetailPanel.PADDING;

public class ResearchDashboardPanel extends Panel {

    ResearchDetailPanel detailPanel;

    public ResearchDashboardPanel(ResearchDetailPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        detailPanel = panel;
    }

    @Override
    public void addWidgets() {
        // close panel button
        Button closePanel = new SimpleTextButton(this, new StringTextComponent("Close"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {
                detailPanel.research = null;
                closeGui();
            }
        };
        closePanel.setPosAndSize(width-PADDING, 0, PADDING, PADDING);
        add(closePanel);
    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
        super.draw(matrixStack, theme, x, y, w, h);
        // name
        theme.drawString(matrixStack, detailPanel.research.getName(), x, y);
        // icon
        detailPanel.icon.draw(matrixStack, x, y+PADDING, 32, 32);
        // TODO: research progress
        // ResearchData data = ResearchDataAPI.getData((ServerPlayerEntity) detailPanel.researchScreen.player).getData(detailPanel.research);
        // theme.drawString(matrixStack, data.getProgress()*100 + "%", x+theme.getStringWidth(detailPanel.research.getName())+5, y);
    }
}
