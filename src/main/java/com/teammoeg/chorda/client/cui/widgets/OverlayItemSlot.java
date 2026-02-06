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

package com.teammoeg.chorda.client.cui.widgets;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class OverlayItemSlot extends ItemSlot {
	@Getter
	@Setter
	protected CIcon overlay;
	@Getter
	@Setter
	protected int overlayWidth;
	@Getter
	@Setter
	protected int overlayHeight;
	@Getter
	@Setter
	Consumer<TooltipBuilder> tooltips;
	public OverlayItemSlot(UIElement parent) {
		super(parent);
	}
	
	public OverlayItemSlot(UIElement parent, ItemStack item) {
		super(parent, item);
	}

	public OverlayItemSlot(UIElement parent, Ingredient item) {
		super(parent, item);
	}

	public OverlayItemSlot(UIElement parent, ItemStack[] item) {
		super(parent, item);
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {

		super.render(graphics, x, y, w, h);
      
        if (overlay != null) {
        	graphics.pose().pushPose();
        	graphics.pose().translate(0, 0, 200);
            overlay.draw(graphics, x, y, overlayWidth, overlayHeight);
            graphics.pose().popPose();
        }
        
	}
    public void setOverlay(CIcon overlay, int height, int width) {
        this.overlay = overlay;
        this.overlayHeight = height;
        this.overlayWidth = width;
    }
    public void resetOverlay() {
        this.overlay = null;
    }

	@Override
	public void getTooltip(TooltipBuilder tooltip) {
		super.getTooltip(tooltip);
		if(tooltips!=null)
			tooltips.accept(tooltip);
	}

	@Override
	public void clear() {
		resetOverlay();
		super.clear();
	}
    
}
