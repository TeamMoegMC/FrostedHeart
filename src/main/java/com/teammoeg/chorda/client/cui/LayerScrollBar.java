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

package com.teammoeg.chorda.client.cui;


public class LayerScrollBar extends ScrollBar {
	private final UILayer layer;

	public LayerScrollBar(UIElement parent, boolean isVertical, UILayer affected) {
		super(parent, isVertical, 0);
		layer = affected;
		affected.setSmoothScrollEnabled(true);
	}

	public LayerScrollBar(UIElement parent, UILayer affected) {
		this(parent, true, affected);
	}

	public UILayer getAffectedLayer() {
		return layer;
	}

	@Override
	public double getMin() {
		return 0;
	}

	@Override
	public void setMaxValue(double max) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setMinValue(double min) {
		if(min!=0)
		throw new UnsupportedOperationException();
	}
	@Override
	public double getPage() {
		return isVertical ? layer.getHeight() :  layer.getWidth();
	}

	@Override
	public void setPageStep(double s) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getMax() {
		return isVertical ? layer.getContentHeight() - layer.getHeight() : layer.getContentWidth() - layer.getWidth();
	}

	@Override
	public void setScrollStep(double s) {
		layer.setScrollStep(s);
	}

	@Override
	public double getScrollStep() {
		return layer.getScrollStep();
	}

	@Override
	public int getScrollBarSize() {
		var max = getMax();
		if (max <= 0) {
			return 0;
		}

		int size = isVertical ?
				(int) (layer.getHeight() / (max + layer.getHeight()) * getHeight()) :
				(int) (layer.getWidth() / (max + layer.getWidth()) * getWidth());

        return Math.max(size, 10);
	}

	@Override
	public void onValueChanged() {
		var value = getMax() <= 0 ? 0 : getValue();

		if (isVertical) {
			getAffectedLayer().setOffsetY(-(int) value);
		} else {
			getAffectedLayer().setOffsetX(-(int) value);
		}
	}

	@Override
	public boolean isScrollFocused() {
		return super.isScrollFocused() || layer.isMouseOver();
	}

	@Override
	public boolean isVisible() {
		return getScrollBarSize() > 0;
	}

	@Override
	public boolean isEnabled() {
		return getScrollBarSize() > 0;
	}
}