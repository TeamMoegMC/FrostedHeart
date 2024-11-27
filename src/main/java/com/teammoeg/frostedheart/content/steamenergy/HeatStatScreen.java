/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;

public class HeatStatScreen extends BaseScreen {
    HeatStatContainer cx;

    public HeatStatScreen(HeatStatContainer cx) {
        this.cx = cx;
    }
    public static class EndPointFakeSlot extends Panel {
		public EndPointFakeSlot(Panel panel) {
			super(panel);
			this.setSize(33, 39);
		}
		@Override
		public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
			theme.drawContainerSlot(matrixStack, x, y, w, h);
		}
		@Override
		public void addWidgets() {
		}
		@Override
		public void alignWidgets() {
		}
    	
    }   
    public static class EndPointSlot extends Panel {
    	EndPointData epd;
    	Icon ic;
    	String val;
    	boolean isIntake;
		public EndPointSlot(Panel panel, EndPointData epd,boolean isIntake) {
			super(panel);
			this.epd = epd;
			ic=ItemIcon.getItemIcon(epd.blk.asItem());
			if(isIntake) {
				val=String.format("%.1f",epd.avgIntake);
			}else {
				val=String.format("%.1f",epd.avgOutput);
			}
			this.isIntake=isIntake;
			this.setSize(33, 39);
		}
		@Override
		public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
			theme.drawContainerSlot(matrixStack, x, y, w, h);
			ic.draw(matrixStack, x+4, y+2, 24, 24);
			if(isIntake)
				theme.drawString(matrixStack, val, x+32-theme.getStringWidth(val), y+30);
			else
				theme.drawString(matrixStack, val, x+32-theme.getStringWidth(val), y+30,epd.canCostMore?Color4I.RED:Color4I.GREEN,0);
		}
		@Override
		public void addWidgets() {
		}
		@Override
		public void alignWidgets() {
		}
    	
    }
    public static class EndPointList extends Panel {
        public HeatStatScreen tradeScreen;
        public PanelScrollBar scroll;
        boolean isIntake;
        public EndPointList(HeatStatScreen panel,boolean isIntake) {
            super(panel);
            tradeScreen = panel;
            this.isIntake=isIntake;
            this.scroll=new PanelScrollBar(panel, this); 
            this.setWidth(100);
        }

        @Override
        public void addWidgets() {
            int offset = 0;
            int i=0;
            for (EndPointData r : tradeScreen.cx.data) {
            	if((isIntake&&r.avgIntake==-1)||(!isIntake&&r.avgOutput==-1))continue;
            	EndPointSlot button = new EndPointSlot(this, r,isIntake);
                add(button);
                button.setPos(i*33, offset);
                i++;
                if(i>2) {
                	i=0;
                	offset += button.height+1;
                }
            }
            if(i!=0) {
            	while(i<=2) {
            		EndPointFakeSlot slot=new EndPointFakeSlot(this);
            		slot.setPos(i*33, offset);
            		add(slot);
            		i++;
            	}
            }
           // scroll.setMaxValue(offset+39 + 1);
        }

        @Override
        public void alignWidgets() {
        }

		@Override
		public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
			super.drawBackground(matrixStack, theme, x, y, w, h);
			theme.drawPanelBackground(matrixStack, x, y, w, h);
		}

    }
    @Override
    public void addWidgets() {
    	EndPointList iepl=new EndPointList(this,true);
    	iepl.setPosAndSize(6, 18, 99, 200);
    	iepl.scroll.setPosAndSize(108, 18, 10, 200);
    	this.add(iepl);
    	this.add(iepl.scroll);
    	EndPointList oepl=new EndPointList(this,false);
    	oepl.setPosAndSize(128, 18, 99, 200);
    	oepl.scroll.setPosAndSize(230, 18, 10, 200);
    	this.add(oepl);
    	this.add(oepl.scroll);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(matrixStack, theme, x, y, w, h);
    }

    @Override
    public void drawForeground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
        super.drawForeground(matrixStack, theme, x, y, w, h);
        theme.drawString(matrixStack,"Generating",x+6,y+ 6);
        theme.drawString(matrixStack,"Consuming",x+118,y+ 6);
    }

    @Override
    public boolean onInit() {
        int sw = 244;
        int sh = 246;
        this.setSize(sw, sh);
        return super.onInit();
    }

}
