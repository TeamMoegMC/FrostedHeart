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

package com.teammoeg.chorda.client.cui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.lang.Components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TooltipBuilder implements Consumer<Component> {
	private List<Component> tooltip = new ArrayList<>();
	private int zOffset = 600;

	public TooltipBuilder(int initZ) {
		zOffset=initZ;
	}

	@Override
	public void accept(Component t) {
		tooltip.add(t);
	}
	public TooltipBuilder add(Component t) {
		tooltip.add(t);
		return this;
	}
	public TooltipBuilder addString(String str) {
		tooltip.add(Components.str(str));
		return this;
	}
	public TooltipBuilder addTranslation(String str) {
		tooltip.add(Components.translatable(str));
		return this;
	}
	public TooltipBuilder translateZ(int value) {
		zOffset+=value;
		return this;
	}
	public void draw(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!tooltip.isEmpty()) {
			graphics.pose().translate(0, 0, zOffset);

			graphics.setColor(1f, 1f, 1f, 0.8f);
			graphics.renderTooltip(ClientUtils.getMc().font, tooltip, Optional.empty(), mouseX, Math.max(mouseY, 18));
			graphics.setColor(1f, 1f, 1f, 1f);
		}
	}
}
