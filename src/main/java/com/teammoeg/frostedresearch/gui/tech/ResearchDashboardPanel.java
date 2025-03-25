/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedresearch.gui.tech;

import com.teammoeg.chorda.client.cui.RTextField;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.data.ResearchData;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.gui.ResearchEditUtils;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.gui.TechTextButton;

import blusunrize.immersiveengineering.client.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;

import java.text.DecimalFormat;

public class ResearchDashboardPanel extends Panel {

    final static String read = "kmgtpezyh";
    final static DecimalFormat df1 = new DecimalFormat("#.#");

    final static DecimalFormat df2 = new DecimalFormat("#.##");

    final static DecimalFormat df3 = new DecimalFormat("#.##");

    ResearchDetailPanel detailPanel;
    RTextField techpoint;
    RTextField availableInsightLevel;
    boolean isClueNotCompleted=false;
    public ResearchDashboardPanel(ResearchDetailPanel panel) {
        super(panel);
        this.setOnlyInteractWithWidgetsInside(true);
        this.setOnlyRenderWidgetsInside(true);
        detailPanel = panel;
        techpoint = new RTextField(this).setMaxWidth(100).setMaxLine(1).setColor(TechIcons.text);
        techpoint.setPos(40, 20+ClientUtils.font().lineHeight);
        availableInsightLevel = new RTextField(this).setMaxWidth(100).setMaxLine(1).setColor(TechIcons.text);
        availableInsightLevel.setPos(40, 20+ClientUtils.font().lineHeight*2);
        availableInsightLevel.setMaxLine(1);
    }

    public static synchronized String toReadable(long num) {
        int unit = -1;
        double lnum = num;
        while (lnum > 1999) {
            unit++;
            lnum /= 1000;
        }
        if (unit < 0)
            return String.valueOf(num);
        if (lnum >= 1000) {
            return "" + ((long) lnum) + read.charAt(unit);
        } else if (lnum >= 100) {
            return df1.format(lnum) + read.charAt(unit);
        } else if (lnum >= 10) {
            return df2.format(lnum) + read.charAt(unit);
        } else {
            return df3.format(lnum) + read.charAt(unit);
        }
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
        RTextField tf = new RTextField(this);
        tf.setPos(0, 0);
        add(tf);
        tf.setMaxWidth(140).setMinWidth(140).setMaxLine(2).setColor(TechIcons.text).addFlags(4);
        tf.setText(detailPanel.research.getName());
        
        
        
        RTextField tp = new RTextField(this).setMaxWidth(140).setMaxLine(1).setColor(TechIcons.text);
        tp.setPos(40, 20);
        add(tp);
        tp.setText(Lang.translateGui("research.points"));
        tp.setX(140 - tp.width);
        
        if (FHResearch.editor) {
            Button create = new TechTextButton(this, Components.str("edit"),
                    Icon.empty()) {
                @Override
                public void onClicked(MouseButton mouseButton) {
                	if(detailPanel.research!=null)
                		ResearchEditUtils.editResearch(this, detailPanel.research);
                }
            };
            create.setPos(40, 30);
            add(create);
        }
        techpoint.setText(toReadable(detailPanel.research.getRequiredPoints()) + "IOPS");
        add(techpoint);
        techpoint.setColor(TechIcons.text);
        ResearchData rd = detailPanel.research.getData();
        if (rd.canResearch()) {
            if (!rd.canComplete(detailPanel.research)) {
            	tp.setColor(TechIcons.text_red);
                techpoint.setColor(TechIcons.text_red);
            }
            techpoint.setText(toReadable(rd.getTotalCommitted(detailPanel.research)) + "/" + toReadable(detailPanel.research.getRequiredPoints()) + "IOPS");
        }
        techpoint.setX(140 - techpoint.width);
        if (!rd.canResearch()) {
	        int insightNeeded=detailPanel.research.getInsight();
	        int insightAvailable=ClientResearchDataAPI.getData().get().getAvailableInsightLevel();
	        availableInsightLevel.setText(Lang.translateGui("research.insight_required",insightNeeded,insightAvailable));
	        if (insightNeeded>insightAvailable) {
	            availableInsightLevel.setColor(TechIcons.text_red);
	        }
	        add(availableInsightLevel);
	        availableInsightLevel.setX(140 - availableInsightLevel.width);
	    }
        if (rd.canResearch() && !rd.canComplete(detailPanel.research)) {
            RTextField rq = new RTextField(this).setMaxWidth(140).setMaxLine(1).setColor(TechIcons.text_red);
            rq.setPos(40, 20+ClientUtils.font().lineHeight*3);
            add(rq);
            rq.setText(Lang.translateGui("research.required_clue"));
            rq.setX(140 - rq.width);
        }
    }

    @Override
    public void alignWidgets() {

    }
    @Override
    public void draw(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        

        

        super.draw(matrixStack, theme, x, y, w, h);

        // name
        //theme.drawString(matrixStack, detailPanel.research.getName(), x+7, y+8);
        // icon
        TechIcons.SHADOW.draw(matrixStack, x + 1, y + 36, 36, 9);
        detailPanel.icon.draw(matrixStack, x + 3, y + 10, 32, 32);
        GuiHelper.setupDrawing();
        TechIcons.HLINE_L.draw(matrixStack, x, y + 55, 140, 3);

        // TODO: research progress
        // ResearchData data = ResearchDataAPI.getData((ServerPlayerEntity) detailPanel.researchScreen.player).getData(detailPanel.research);
        // theme.drawString(matrixStack, data.getProgress()*100 + "%", x+theme.getStringWidth(detailPanel.research.getName())+5, y);
    }
}
