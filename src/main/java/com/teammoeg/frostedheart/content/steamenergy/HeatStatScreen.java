/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.chorda.client.cui.base.PrimaryLayer;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.LayerScrollBar;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;

import net.minecraft.client.gui.GuiGraphics;

public class HeatStatScreen extends PrimaryLayer {
    HeatStatContainer cx;

    public HeatStatScreen(HeatStatContainer cx) {
    	super();
        this.cx = cx;
    }

    @Override
    public void addUIElements() {
        EndPointList iepl = new EndPointList(this, true);
        iepl.setPosAndSize(6, 18, 99, 200);
        iepl.scroll.setPosAndSize(108, 18, 10, 200);
        this.add(iepl);
        this.add(iepl.scroll);
        EndPointList oepl = new EndPointList(this, false);
        oepl.setPosAndSize(128, 18, 99, 200);
        oepl.scroll.setPosAndSize(230, 18, 10, 200);
        this.add(oepl);
        this.add(oepl.scroll);
    }

    @Override
    public void drawBackground(GuiGraphics matrixStack,  int x, int y, int w, int h) {
    	getTheme().drawUIBackground(matrixStack, x, y, w, h);
       
    }

    @Override
    public void drawForeground(GuiGraphics matrixStack,  int x, int y, int w, int h) {
        super.drawForeground(matrixStack, x, y, w, h);
        matrixStack.drawString(getFont(), "Consuming", x + 6, y + 6,0xFF000000);
        matrixStack.drawString(getFont(), "Generating", x + 118, y + 6,0XFF000000);
    }

    @Override
    public boolean onInit() {
        int sw = 244;
        int sh = 246;
        this.setSize(sw, sh);
        return super.onInit();
    }

    public static class EndPointFakeSlot extends UIElement {
        public EndPointFakeSlot(UIElement panel) {
            super(panel);
            this.setSize(33, 39);
        }

        @Override
        public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
            //theme.drawContainerSlot(matrixStack, x, y, w, h);
        }

    }

    public static class EndPointSlot extends UIElement {
        HeatEndpoint epd;
        CIcon ic;
        String val;
        boolean isIntake;

        public EndPointSlot(UIElement panel, HeatEndpoint epd, boolean isIntake) {
            super(panel);
            this.epd = epd;
            ic = CIcons.getIcon(epd.blk.asItem());
            if (isIntake) {
                val = String.format("%.1f", epd.avgIntake);
            } else {
                val = String.format("%.1f", epd.avgOutput);
            }
            this.isIntake = isIntake;
            this.setSize(33, 39);
        }

        @Override
        public void render(GuiGraphics matrixStack,  int x, int y, int w, int h) {
            //theme.drawContainerSlot(matrixStack, x, y, w, h);
            ic.draw(matrixStack, x + 4, y + 2, 24, 24);
            if (isIntake)
            	matrixStack.drawString(getFont(), val, x + 32 - getFont().width(val), y + 30, epd.canCostMore ? 0xFFFF5555 : 0x55FF55);
            else
            	matrixStack.drawString(getFont(), val, x + 32 - getFont().width(val), y + 30,getTheme().getButtonTextColor());
        }

    }

    public static class EndPointList extends UILayer {
        public HeatStatScreen screen;
        public LayerScrollBar scroll;
        boolean isIntake;

        public EndPointList(HeatStatScreen panel, boolean isIntake) {
            super(panel);
            screen = panel;
            this.isIntake = isIntake;
            this.scroll = new LayerScrollBar(panel,true, this);
            this.setWidth(100);
        }

        @Override
        public void addUIElements() {
            int offset = 0;
            int i = 0;
            for (HeatEndpoint r : screen.cx.data) {
                if ((isIntake && r.avgIntake == 0) || (!isIntake && r.avgOutput == 0)) continue;
                EndPointSlot button = new EndPointSlot(this, r, isIntake);
                add(button);
                button.setPos(i * 33, offset);
                i++;
                if (i > 2) {
                    i = 0;
                    offset += button.getHeight() + 1;
                }
            }
            if (i != 0) {
                while (i <= 2) {
                    EndPointFakeSlot slot = new EndPointFakeSlot(this);
                    slot.setPos(i * 33, offset);
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
        public void drawBackground(GuiGraphics matrixStack, int x, int y, int w, int h) {
            getTheme().drawPanel(matrixStack, x, y, w, h);
        }

    }

}
