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

import org.joml.Matrix4f;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.TesselateHelper;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.text.Components;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 滚动条控件。支持水平和垂直方向的滚动操作，可通过鼠标拖拽、点击轨道和滚轮滚动控制。
 * <p>
 * Scroll bar widget. Supports both horizontal and vertical scrolling via mouse dragging,
 * track clicking, and mouse wheel scrolling.
 */
public class LevelSpinLayer extends UIElement {
	/** 滚动条滑块大小（像素） / Scroll bar thumb size in pixels */
	@Getter
	private final int scrollBarSize;
	/** 当前滚动值 / Current scroll value */
	private int value = 0;
	/** 最小滚动值 / Minimum scroll value */
	@Getter
	private int min = 0;
	/** 最大滚动值 / Maximum scroll value */
	@Getter
	private int max = 0;

	/**
	 * 创建滚动条控件。
	 * <p>
	 * Creates a scroll bar widget.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param size 滑块大小（像素） / Thumb size in pixels
	 */
	public LevelSpinLayer(UIElement parent, int size) {
		super(parent);
		scrollBarSize = Math.max(size, 0);
		this.setHeight(scrollBarSize+2);
		this.setMaxValue(10);
	}

	/**
	 * 设置最小滚动值。
	 * <p>
	 * Sets the minimum scroll value.
	 *
	 * @param min 最小值 / Minimum value
	 */
	public void setMinValue(int min) {
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
	public void setMaxValue(int max) {
		this.max = max;
		setValue(getValue());
		this.setWidth(scrollBarSize*(max-min+1)+24);
	}

	/** {@inheritDoc} */
	@Override
	public boolean onMousePressed(MouseButton button) {
		System.out.println(this.getMouseX()+","+this.getMouseY()+","+isMouseOver()+"("+this.getWidth()+","+this.getHeight());
		if (isMouseOver()) {
			int scrollBarSize=getScrollBarSize();
			if(this.getMouseX()<10) {
				this.setValue(this.getValue()-1);
				return true;
			}
			if(this.getMouseX()>scrollBarSize*(max-min)+10) {
				this.setValue(this.getValue()+1);
				return true;
			}
			int setValue=Mth.floor((this.getMouseX()-10)/(scrollBarSize+1))+1;
			this.setValue(setValue);
			
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
		int dx=x;
		boolean isMinMouseOver=isMouseOver&&this.getMouseX()<10;
		hint.theme(this).drawButton(graphics, dx, y, 10, scrollBarSize, isMinMouseOver, true);
		graphics.drawString(getFont(), "-", dx+2, y+2, hint.theme(this).buttonTextColor());
		dx+=11;
		try(var helper=TesselateHelper.getShapeTesslator()){
			Matrix4f m4f=graphics.pose().last().pose();
			int maxVal=(max-min);
			int curVal=getValue()-min;
			for(int i=0;i<maxVal;i++) {

				helper.drawRect(m4f, dx, y, dx+scrollBarSize, y+scrollBarSize, hint.theme(this).UITextColor(), true);
				if(i<curVal) {
					helper.fillRect(m4f, dx+2, y+2, dx+scrollBarSize-2, y+scrollBarSize-2, hint.theme(this).UITextColor());
				}
				dx+=scrollBarSize+1;
			}
		}
		boolean isMaxMouseOver=isMouseOver&&this.getMouseX()>dx+1;
		hint.theme(this).drawButton(graphics, dx, y, 10, scrollBarSize, isMaxMouseOver, true);
		graphics.drawString(getFont(), "+", dx+2, y+2, hint.theme(this).buttonTextColor());
		

	}


	/**
	 * 滚动值改变时的回调方法。子类可重写以响应值变化。
	 * <p>
	 * Callback method when the scroll value changes. Subclasses can override to respond to value changes.
	 */
	public void onValueChanged() {
	}

	/**
	 * 设置滚动值，会限制在最小值和最大值之间。
	 * <p>
	 * Sets the scroll value, clamped between minimum and maximum values.
	 *
	 * @param v 新的滚动值 / New scroll value
	 */
	public void setValue(int v) {
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
	public int getValue() {
		return value;
	}
}