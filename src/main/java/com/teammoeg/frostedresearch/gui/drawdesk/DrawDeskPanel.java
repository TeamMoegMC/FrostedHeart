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

package com.teammoeg.frostedresearch.gui.drawdesk;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.cui.Button;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.blocks.DrawingDeskTileEntity;
import com.teammoeg.frostedresearch.gui.TechButton;
import com.teammoeg.frostedresearch.gui.tech.ResearchProgressPanel;
import com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacket;
import com.teammoeg.frostedresearch.network.FHResearchControlPacket;
import com.teammoeg.frostedresearch.network.FHResearchControlPacket.Operator;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.client.gui.GuiGraphics;

public class DrawDeskPanel extends UILayer {
    DrawDeskScreen dd;
    MainGamePanel mgp;

    boolean showHelp;

    boolean enabled;

    boolean visible;
    @Override
	public boolean isVisible() {
		return visible;
	}
    public void setVisible(boolean visible) {
    	this.visible=visible;
    }

	public DrawDeskPanel(DrawDeskScreen p) {
        super(p);
        dd = p;
        mgp = new MainGamePanel(this, dd);
        mgp.setPosAndSize(165, 25, 218, 164);
    }

    @Override
    public void addUIElements() {

        add(mgp);
        HelpPanel hp = new HelpPanel(this);
        hp.setPosAndSize(140, 19, 243, 170);
        add(hp);

        ResearchProgressPanel p = new ResearchProgressPanel(this);
        p.setPosAndSize(14, 19, 111, 68);
        add(p);

        TechButton techTree = new TechButton(this, DrawDeskIcons.TECH) {

            @Override
            public void onClicked(MouseButton arg0) {
                dd.showTechTree();
            }


        };
        techTree.setPosAndSize(16, 68, 36, 19);

        add(techTree);
        TechButton techStop = new TechButton(this, DrawDeskIcons.STOP) {

            @Override
            public void onClicked(MouseButton arg0) {
                Research current = ClientResearchDataAPI.getData().get().getCurrentResearch().get();
                if (current != null)
                	FRNetwork.INSTANCE.sendToServer(new FHResearchControlPacket(Operator.PAUSE, current));
            }
        };
        techStop.setPosAndSize(55, 68, 19, 19);
        add(techStop);
        Button itemSubmit = new Button(this) {

            @Override
            public void getTooltip(TooltipBuilder list) {
                super.getTooltip(list);
                if (!ResearchHooks.canExamine(dd.getTile().getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT)))
                    list.accept(Lang.translateGui("draw_desk.unable_examine"));
                else
                    list.accept(Lang.translateGui("draw_desk.examine"));
            }

            @Override
            public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
                if (isMouseOver() || !ResearchHooks.canExamine(dd.getTile().getInventory().getStackInSlot(DrawingDeskTileEntity.EXAMINE_SLOT)))
                    DrawDeskIcons.EXAMINE.draw(matrixStack, x, y, w, h);
            }

            @Override
            public void onClicked(MouseButton arg0) {
            	FRNetwork.INSTANCE.sendToServer(new FHDrawingDeskOperationPacket(dd.getTile().getBlockPos(), 3));
            }

        };
        itemSubmit.setPosAndSize(113, 109, 18, 18);
        add(itemSubmit);
        int sw = 387;
        int sh = 203;
        this.setSize(sw, sh);
    }

    @Override
    public void alignWidgets() {
    }

    public void closeHelp() {
        showHelp = false;
        mgp.setEnabled(true);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
        DrawDeskIcons.Background.draw(matrixStack, x, y, w, h);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
	public boolean onKeyPressed(int keyCode, int scanCode, int modifier) {
    	if (showHelp && CInputHelper.isEsc(keyCode)) {
            closeHelp();
            return true;
        }
		return super.onKeyPressed(keyCode, scanCode, modifier);
	}



    public void openHelp() {
        showHelp = true;
        mgp.setEnabled(false);

    }


}
