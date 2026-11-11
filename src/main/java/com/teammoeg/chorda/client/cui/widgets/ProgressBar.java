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

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 进度条控件：每帧从供应器读进度（0–1）和颜色，由当前主题的 {@code drawProgressBar} 绘制。
 * <p>
 * A progress bar that reads its progress (0 to 1) and color from suppliers every frame and is drawn by the current
 * theme's {@code drawProgressBar}, so a themed screen gets its own track and fill.
 */
public class ProgressBar extends UIElement {
	private final DoubleSupplier progress;
	private final IntSupplier color;
	private Supplier<List<Component>> tooltip = List::of;

	public ProgressBar(UIElement parent, DoubleSupplier progress, IntSupplier color) {
		super(parent);
		this.progress = progress;
		this.color = color;
		setSize(40, 4);
	}

	public ProgressBar(UIElement parent, double progress, int color) {
		this(parent, () -> progress, () -> color);
	}

	/** Lines shown when the bar is hovered; empty for none. */
	public ProgressBar tooltip(Supplier<List<Component>> lines) {
		this.tooltip = lines == null ? List::of : lines;
		return this;
	}

	public float getProgress() {
		double value = progress.getAsDouble();
		if (!Double.isFinite(value)) return 0;
		return (float) Math.max(0, Math.min(1, value));
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		hint.theme(this).drawProgressBar(graphics, x, y, w, h, getProgress(), color.getAsInt());
	}

	@Override
	public void getTooltip(TooltipBuilder builder) {
		tooltip.get().forEach(builder::accept);
	}
}
