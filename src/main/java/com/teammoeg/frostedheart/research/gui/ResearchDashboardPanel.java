package com.teammoeg.frostedheart.research.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.util.text.StringTextComponent;

import static com.teammoeg.frostedheart.research.gui.ResearchDetailPanel.PADDING;

public class ResearchDashboardPanel extends Panel {

    ResearchDetailPanel detailPanel;
    Button closePanel;
    Button commitItems;
    Button startResearch;

    public ResearchDashboardPanel(ResearchDetailPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        detailPanel = panel;
    }

    @Override
    public void addWidgets() {
        // start research button
        startResearch = new SimpleTextButton(this, new StringTextComponent("Start Research"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {
            }
        };

        startResearch.setPos(width-PADDING*5, 0);
        add(startResearch);

        // commit items button
        commitItems = new SimpleTextButton(this, new StringTextComponent("Commit Materials"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {
            }
        };

        commitItems.setPos(width-PADDING*5, PADDING*2);
        add(commitItems);

        // close panel button
        closePanel = new SimpleTextButton(this, new StringTextComponent("Close"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {
                detailPanel.research = null;
                closeGui();
            }
        };
        closePanel.setPosAndSize(width, 0, PADDING, PADDING);
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
        // research pts
    }
}
