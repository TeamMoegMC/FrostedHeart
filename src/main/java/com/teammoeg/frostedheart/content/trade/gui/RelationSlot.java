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

package com.teammoeg.frostedheart.content.trade.gui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;

import net.minecraft.client.gui.GuiGraphics;

public class RelationSlot extends UIElement {
    Supplier<Integer> toshow;
    Consumer<TooltipBuilder> tooltip;

    public RelationSlot(UIElement panel) {
        super(panel);
        this.setSize(16, 16);
    }

    public RelationSlot(UIElement panel, Supplier<Integer> is) {
        super(panel);
        this.toshow = is;
        this.setSize(16, 16);
    }


    @Override
	public void getTooltip(TooltipBuilder tooltip) {
		super.getTooltip(tooltip);
		this.tooltip.accept(tooltip);
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {

        if (toshow == null)
            return;
        int val = toshow.get();
        //System.out.println(val);
        if (val == 0) return;
        val = Math.min(Math.max(-1000, val), 1000);
        int show = (int) Math.ceil(Math.abs(val / 10f));
        String str = (val < 0 ? "-" : "") + show;

        int dx;
        switch (str.length()) {
            case 0:
                dx = 15;
                break;
            case 1:
                dx = 15;
                break;
            case 2:
                dx = 10;
                break;
            case 3:
                dx = 5;
                break;
            case 4:
                dx = 0;
                break;
            default:
                dx = 0;
                break;
        }
        matrixStack.drawString(getFont(), str, x + dx - 2, y + 10, val<0?0x55ff55:0xff5555);
        

    }

    public void setTooltip(Consumer<TooltipBuilder> tooltip) {
        this.tooltip = tooltip;
    }

}
