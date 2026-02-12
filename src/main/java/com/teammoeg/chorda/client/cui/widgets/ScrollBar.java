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

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.theme.VanillaTheme;
import com.teammoeg.chorda.text.Components;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class ScrollBar extends UIElement {
	@Getter
	final boolean isVertical;
	@Getter
	private final int scrollBarSize;
	private double value = 0;
	private double step = 20;
	@Getter
	private double page=200;
	private double delta = Integer.MAX_VALUE;
	@Getter
	private double min = 0;
	@Getter
	private double max = 100;
	@Setter
	private boolean ignoreMouseOver = false;
	@Setter
	private boolean ignoreDirection = true;

	public ScrollBar(UIElement parent, boolean isVertical, int size) {
		super(parent);
		this.isVertical = isVertical;
		scrollBarSize = Math.max(size, 0);
	}

	public void setMinValue(double min) {
		this.min = min;
		setValue(getValue());
	}

	public void setMaxValue(double max) {
		this.max = max;
		setValue(getValue());
	}

	public void setScrollStep(double s) {
		step = Math.max(0D, s);
	}
	public void setPageStep(double s) {
		page = Math.max(0D, s);
	}
	@Override
	public boolean onMousePressed(MouseButton button) {
		if (isMouseOver()) {
			int scrollBarSize=getScrollBarSize();
			if(isVertical) {
				int barPos=(int) (lerpValue(height - scrollBarSize-2)+1);
				System.out.println(getMouseY()+"/"+barPos+"("+height);
				if(getMouseY()<barPos) {
					this.setValue(this.getValue()-getPage());
				}else if(getMouseY()>barPos+scrollBarSize) {
					this.setValue(this.getValue()+getPage());
				}else
					delta =getMouseY();
			}else {
				int barPos=(int) (lerpValue(width - scrollBarSize-2)+1);
				if(getMouseX()<barPos) {
					this.setValue(this.getValue()-getPage());
				}else if(getMouseX()>barPos+scrollBarSize) {
					this.setValue(this.getValue()+getPage());
				}else
					delta=getMouseX();
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean onMouseScrolled(double scroll) {
		if (scroll != 0 && isScrollDirection() && isScrollFocused()) {
			setValue(getValue() - getScrollStep() * scroll);
			return true;
		}

		return false;
	}

	@Override
	public void getTooltip(TooltipBuilder list) {
		if (showValueTooltip()) {
			Component t = getTitle();
			list.accept(Components.str(Components.isEmpty(t) ? (Double.toString(getValue())) : (t + ": " + getValue())));
		}
	}

	public boolean showValueTooltip() {
		return false;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, int width, int height) {
		int scrollBarSize = getScrollBarSize();

		if (scrollBarSize > 0) {
			double v = getValue();

			if (delta != Integer.MIN_VALUE) {
				if (CInputHelper.isMouseLeftDown()) {
					//System.out.println(delta);
					if (isVertical) {
						v += (getMouseY() - (delta)) * (getMax()-getMin()) / (double) (height - scrollBarSize);
						delta=getMouseY();
					} else {
						v += (getMouseX() - (delta)) * (getMax()-getMin()) / (double) (width - scrollBarSize);
						delta=getMouseX();
					}
				} else {
					delta = Integer.MIN_VALUE;
				}
			}

			setValue(v);
		}

		drawBackground(graphics, x-1, y-1, width+2, height+2);

		if (scrollBarSize > 0) {
			if (isVertical) {
				drawScrollBar(graphics, x+1, (int) (y + lerpValue(height - scrollBarSize-2)+1), width-2, scrollBarSize);
			} else {
				drawScrollBar(graphics, (int) (x + lerpValue(width - scrollBarSize-2)+1), y+1, scrollBarSize, height-2);
			}
		}
	}

	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
		getTheme().drawSliderBackground(graphics, x, y, w, h, isMouseOver());
	}

	public void drawScrollBar(GuiGraphics graphics, int x, int y, int w, int h) {
		getTheme().drawSliderBar(graphics, x, y, w, h, isMouseOver());
	}

	public void onValueChanged() {
	}

	public boolean isScrollDirection() {
		return ignoreDirection || CInputHelper.isShiftKeyDown() != isVertical;
	}

	public boolean isScrollFocused() {
		return ignoreMouseOver || isMouseOver();
	}

	public void setValue(double v) {
		v = Mth.clamp(v, getMin(), getMax());

		if (value != v) {
			value = v;
			onValueChanged();
		}
	}

	public double getValue() {
		return value;
	}

	public double lerpValue(double max) {
		return Mth.clamp(getScrollRatio(), 0, 1) * max;
	}

	public float getScrollRatio() {
		return (float) (this.getValue() / (this.getMax() - this.getMin()));
	}

	public double getScrollStep() {
		return step;
	}
}