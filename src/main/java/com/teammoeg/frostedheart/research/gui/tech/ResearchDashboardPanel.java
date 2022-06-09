package com.teammoeg.frostedheart.research.gui.tech;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.FHResearch;
import com.teammoeg.frostedheart.research.ResearchEditorDialog;
import com.teammoeg.frostedheart.research.gui.RTextField;
import com.teammoeg.frostedheart.research.gui.TechIcons;
import com.teammoeg.frostedheart.research.gui.TechTextButton;
import com.teammoeg.frostedheart.research.gui.editor.EditUtils;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;

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
       /* Button closePanel = new SimpleTextButton(this, new StringTextComponent("Close"), Icon.EMPTY) {
            @Override
            public void onClicked(MouseButton mouseButton) {
                detailPanel.close();
                //closeGui();
            }
        };
        closePanel.setPosAndSize(width-PADDING, 0, PADDING, PADDING);
        add(closePanel);*/
    	RTextField tf=new RTextField(this);
    	tf.setPos(0,0);
    	add(tf);
    	tf.setMaxWidth(140).setMinWidth(140).setMaxLine(2).setColor(TechIcons.text).addFlags(4);
    	tf.setText(detailPanel.research.getName());
    	if(FHResearch.editor) {
			Button create = new TechTextButton(this, GuiUtils.str("edit"),
					Icon.EMPTY) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					EditUtils.editResearch(this, detailPanel.research);
				}
			};
			create.setPos(40,20);
			add(create);
		}
    }

    @Override
    public void alignWidgets() {

    }

    @Override
    public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h) {
    	
        super.draw(matrixStack, theme, x, y, w, h);
        // name
        //theme.drawString(matrixStack, detailPanel.research.getName(), x+7, y+8);
        // icon
        TechIcons.SHADOW.draw(matrixStack, x+1, y+36, 36, 9);
        detailPanel.icon.draw(matrixStack, x+3, y+10, 32, 32);
        GuiHelper.setupDrawing();
        TechIcons.HLINE_L.draw(matrixStack, x, y+49,140, 3);
        
        // TODO: research progress
        // ResearchData data = ResearchDataAPI.getData((ServerPlayerEntity) detailPanel.researchScreen.player).getData(detailPanel.research);
        // theme.drawString(matrixStack, data.getProgress()*100 + "%", x+theme.getStringWidth(detailPanel.research.getName())+5, y);
    }
}
