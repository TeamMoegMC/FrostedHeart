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

package com.teammoeg.chorda.client.cui.base;

import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.text.Components;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 工具提示构建器，用于收集和渲染CUI元素的工具提示文本。
 * 支持添加文本组件、字符串、翻译键，并通过主题系统进行最终渲染。
 * 可调整Z轴偏移以控制提示的渲染层级。
 * <p>
 * Tooltip builder for collecting and rendering tooltip text for CUI elements.
 * Supports adding text components, plain strings, and translation keys, with final
 * rendering delegated to the theme system. Z-axis offset can be adjusted to control
 * the tooltip rendering layer.
 */
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
	public void draw(GuiGraphics graphics, int mouseX, int mouseY, Theme theme) {
		if (!tooltip.isEmpty()) {
			theme.drawTooltip(graphics, tooltip, mouseX, mouseY, zOffset);
		}
	}
}
