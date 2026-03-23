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
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.text.Components;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 滚动条控件。支持水平和垂直方向的滚动操作，可通过鼠标拖拽、点击轨道和滚轮滚动控制。
 * <p>
 * Scroll bar widget. Supports both horizontal and vertical scrolling via mouse dragging,
 * track clicking, and mouse wheel scrolling.
 */
public class ScrollBar extends UIElement {
	/** 是否为垂直滚动条 / Whether this is a vertical scroll bar */
	@Getter
	final boolean isVertical;
	/** 滚动条滑块大小（像素） / Scroll bar thumb size in pixels */
	@Getter
	private final int scrollBarSize;
	/** 当前滚动值 / Current scroll value */
	private double value = 0;
	/** 滚轮滚动步长 / Mouse wheel scroll step */
	private double step = 20;
	/** 页面步长（点击轨道时的跳跃量） / Page step (jump amount when clicking the track) */
	@Getter
	private double page=200;
	/** 拖拽参考位置 / Drag reference position */
	private double delta = Integer.MAX_VALUE;
	/** 最小滚动值 / Minimum scroll value */
	@Getter
	private double min = 0;
	/** 最大滚动值 / Maximum scroll value */
	@Getter
	private double max = 100;
	/** 是否忽略鼠标悬停检测 / Whether to ignore mouse-over detection */
	@Setter
	private boolean ignoreMouseOver = false;
	/** 是否忽略滚动方向限制 / Whether to ignore scroll direction restriction */
	@Setter
	private boolean ignoreDirection = true;

	/**
	 * 创建滚动条控件。
	 * <p>
	 * Creates a scroll bar widget.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param isVertical 是否为垂直方向 / Whether vertical orientation
	 * @param size 滑块大小（像素） / Thumb size in pixels
	 */
	public ScrollBar(UIElement parent, boolean isVertical, int size) {
		super(parent);
		this.isVertical = isVertical;
		scrollBarSize = Math.max(size, 0);
	}

	/**
	 * 设置最小滚动值。
	 * <p>
	 * Sets the minimum scroll value.
	 *
	 * @param min 最小值 / Minimum value
	 */
	public void setMinValue(double min) {
		this.min = min;
		setValue(getValue());
	}

	/**
	 * 设置最大滚动值。
	 * <p>
	 * Sets the maximum scroll value.
	 *
	 * @param max 最大值 / Maximum value
	 */
	public void setMaxValue(double max) {
		this.max = max;
		setValue(getValue());
	}

	/**
	 * 设置滚轮滚动步长。
	 * <p>
	 * Sets the mouse wheel scroll step.
	 *
	 * @param s 步长值 / Step value
	 */
	public void setScrollStep(double s) {
		step = Math.max(0D, s);
	}

	/**
	 * 设置页面步长（点击轨道时的跳跃量）。
	 * <p>
	 * Sets the page step (jump amount when clicking the track).
	 *
	 * @param s 页步长 / Page step value
	 */
	public void setPageStep(double s) {
		page = Math.max(0D, s);
	}

	/** {@inheritDoc} */
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

	/** {@inheritDoc} */
	@Override
	public boolean onMouseScrolled(double scroll) {
		if (scroll != 0 && isScrollDirection() && isScrollFocused()) {
			setValue(getValue() - getScrollStep() * scroll);
			return true;
		}

		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void getTooltip(TooltipBuilder list) {
		if (showValueTooltip()) {
			Component t = getTitle();
			list.accept(Components.str(Components.isEmpty(t) ? (Double.toString(getValue())) : (t + ": " + getValue())));
		}
	}

	/**
	 * 是否在悬停时显示当前值的提示框。默认返回false。
	 * <p>
	 * Whether to show the current value tooltip on hover. Returns false by default.
	 *
	 * @return 是否显示值提示框 / Whether to show value tooltip
	 */
	public boolean showValueTooltip() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
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

		drawBackground(graphics, x-1, y-1, width+2, height+2, hint);

		if (scrollBarSize > 0) {
			if (isVertical) {
				drawScrollBar(graphics, x+1, (int) (y + lerpValue(height - scrollBarSize-2)+1), width-2, scrollBarSize, hint);
			} else {
				drawScrollBar(graphics, (int) (x + lerpValue(width - scrollBarSize-2)+1), y+1, scrollBarSize, height-2, hint);
			}
		}
	}

	/**
	 * 绘制滚动条轨道背景。
	 * <p>
	 * Draws the scroll bar track background.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param x X坐标 / X coordinate
	 * @param y Y坐标 / Y coordinate
	 * @param w 宽度 / Width
	 * @param h 高度 / Height
	 */
	public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		hint.theme(this).drawSliderBackground(graphics, x, y, w, h, isMouseOver());
	}

	/**
	 * 绘制滚动条滑块。
	 * <p>
	 * Draws the scroll bar thumb.
	 *
	 * @param graphics 图形上下文 / Graphics context
	 * @param x X坐标 / X coordinate
	 * @param y Y坐标 / Y coordinate
	 * @param w 宽度 / Width
	 * @param h 高度 / Height
	 */
	public void drawScrollBar(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
		hint.theme(this).drawSliderBar(graphics, x, y, w, h, isMouseOver());
	}

	/**
	 * 滚动值改变时的回调方法。子类可重写以响应值变化。
	 * <p>
	 * Callback method when the scroll value changes. Subclasses can override to respond to value changes.
	 */
	public void onValueChanged() {
	}

	/**
	 * 判断当前滚轮事件是否匹配滚动方向。按住Shift键时切换水平/垂直方向。
	 * <p>
	 * Checks whether the current scroll event matches the scroll direction.
	 * Holding Shift switches between horizontal and vertical directions.
	 *
	 * @return 是否匹配滚动方向 / Whether the scroll direction matches
	 */
	public boolean isScrollDirection() {
		return ignoreDirection || CInputHelper.isShiftKeyDown() != isVertical;
	}

	/**
	 * 判断滚动条是否处于滚动焦点状态。
	 * <p>
	 * Checks whether the scroll bar is in scroll focus.
	 *
	 * @return 是否有滚动焦点 / Whether scroll-focused
	 */
	public boolean isScrollFocused() {
		return ignoreMouseOver || isMouseOver();
	}

	/**
	 * 设置滚动值，会限制在最小值和最大值之间。
	 * <p>
	 * Sets the scroll value, clamped between minimum and maximum values.
	 *
	 * @param v 新的滚动值 / New scroll value
	 */
	public void setValue(double v) {
		v = Mth.clamp(v, getMin(), getMax());

		if (value != v) {
			value = v;
			onValueChanged();
		}
	}

	/**
	 * 获取当前滚动值。
	 * <p>
	 * Gets the current scroll value.
	 *
	 * @return 当前滚动值 / Current scroll value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * 根据滚动比例计算插值位置。
	 * <p>
	 * Calculates the interpolated position based on scroll ratio.
	 *
	 * @param max 最大位置值 / Maximum position value
	 * @return 插值后的位置 / Interpolated position
	 */
	public double lerpValue(double max) {
		return Mth.clamp(getScrollRatio(), 0, 1) * max;
	}

	/**
	 * 获取当前滚动比例（0到1之间）。
	 * <p>
	 * Gets the current scroll ratio (between 0 and 1).
	 *
	 * @return 滚动比例 / Scroll ratio
	 */
	public float getScrollRatio() {
		return (float) (this.getValue() / (this.getMax() - this.getMin()));
	}

	/**
	 * 获取滚轮滚动步长。
	 * <p>
	 * Gets the mouse wheel scroll step.
	 *
	 * @return 滚动步长 / Scroll step
	 */
	public double getScrollStep() {
		return step;
	}
}