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

package com.teammoeg.frostedresearch.gui.tech;

import java.util.List;

import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;
import com.teammoeg.chorda.client.ui.CGuiHelper;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.gui.DrawDeskTheme;
import com.teammoeg.frostedresearch.gui.TechIcons;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.effects.Effect;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class EffectWidget extends UIElement {
	List<Component> tooltips;
	Component title;
	CIcon icon;
	Effect e;
	Research r;

	public EffectWidget(UIElement panel, Effect e, Research r) {
		super(panel);
		tooltips = e.getTooltip(r);
		title = e.getName(r);
		icon = e.getIcon();
		this.e = e;
		this.r = r;
		this.setSize(16, 16);
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		list.accept(title);
		tooltips.forEach(list::accept);
	}

	@Override
	public void render(GuiGraphics matrixStack, int x, int y, int w, int h) {
		CGuiHelper.resetGuiDrawing();
		DrawDeskTheme.drawSlot(matrixStack, x, y, w, h);
		icon.draw(matrixStack, x, y, w, h);
		if (ClientResearchDataAPI.getData().get().isEffectGranted(r, e)) {
			matrixStack.pose().pushPose();
			matrixStack.pose().translate(0, 0, 300);
			CGuiHelper.resetGuiDrawing();
			TechIcons.FIN.draw(matrixStack, x, y, w, h);
			matrixStack.pose().popPose();
		}
	}

	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			if (this.isEnabled()) {
				// TODO edit effect
				e.onClick(r.getData());
			}

			return true;
		}

		return false;
	}
}
