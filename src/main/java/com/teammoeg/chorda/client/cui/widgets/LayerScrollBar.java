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

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;

/**
 * 图层滚动条。绑定到{@link UILayer}的滚动条，自动根据图层内容大小计算滚动范围和滑块尺寸。
 * <p>
 * Layer scroll bar. A scroll bar bound to a {@link UILayer} that automatically calculates
 * scroll range and thumb size based on the layer's content dimensions.
 */
public class LayerScrollBar extends ScrollBar {
	/** 被滚动的图层 / The layer being scrolled */
	private final UILayer layer;

	/**
	 * 创建绑定到指定图层的滚动条。
	 * <p>
	 * Creates a scroll bar bound to the specified layer.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param isVertical 是否为垂直滚动条 / Whether this is a vertical scroll bar
	 * @param affected 被滚动的目标图层 / The target layer to scroll
	 */
	public LayerScrollBar(UIElement parent, boolean isVertical, UILayer affected) {
		super(parent, isVertical, 0);
		layer = affected;
		affected.setSmoothScrollEnabled(true);
	}

	/**
	 * 创建绑定到指定图层的垂直滚动条。
	 * <p>
	 * Creates a vertical scroll bar bound to the specified layer.
	 *
	 * @param parent 父级UI元素 / Parent UI element
	 * @param affected 被滚动的目标图层 / The target layer to scroll
	 */
	public LayerScrollBar(UIElement parent, UILayer affected) {
		this(parent, true, affected);
	}

	/**
	 * 获取被滚动的目标图层。
	 * <p>
	 * Gets the target layer being scrolled.
	 *
	 * @return 目标图层 / The affected layer
	 */
	public UILayer getAffectedLayer() {
		return layer;
	}

	/** {@inheritDoc} */
	@Override
	public double getMin() {
		return 0;
	}

	/**
	 * 不支持手动设置最大值，最大值由图层内容大小自动计算。
	 * <p>
	 * Manual max value setting is not supported; max is auto-calculated from layer content size.
	 *
	 * @throws UnsupportedOperationException 始终抛出 / Always thrown
	 */
	@Override
	public void setMaxValue(double max) {
		throw new UnsupportedOperationException();
	}

	/**
	 * 不支持设置非零最小值。
	 * <p>
	 * Setting a non-zero minimum value is not supported.
	 *
	 * @throws UnsupportedOperationException 当min不为0时抛出 / Thrown when min is not 0
	 */
	@Override
	public void setMinValue(double min) {
		if(min!=0)
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public double getPage() {
		return isVertical ? layer.getHeight() :  layer.getWidth();
	}

	/**
	 * 不支持手动设置页步长，页步长由图层尺寸自动决定。
	 * <p>
	 * Manual page step setting is not supported; page step is auto-determined by layer size.
	 *
	 * @throws UnsupportedOperationException 始终抛出 / Always thrown
	 */
	@Override
	public void setPageStep(double s) {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public double getMax() {
		return isVertical ? layer.getContentHeight() - layer.getHeight() : layer.getContentWidth() - layer.getWidth();
	}

	/** {@inheritDoc} */
	@Override
	public void setScrollStep(double s) {
		layer.setScrollStep(s);
	}

	/** {@inheritDoc} */
	@Override
	public double getScrollStep() {
		return layer.getScrollStep();
	}

	/**
	 * 根据图层内容和视口尺寸计算滚动条滑块大小。
	 * <p>
	 * Calculates the scroll bar thumb size based on layer content and viewport dimensions.
	 *
	 * @return 滑块大小（像素），最小为10；内容无溢出时返回0 / Thumb size in pixels (minimum 10); returns 0 when content does not overflow
	 */
	@Override
	public int getScrollBarSize() {
		var max = getMax();
		if (max <= 0) {
			setValue(0);
			return 0;
		}

		int size = isVertical ?
				(int) (layer.getHeight() / (max + layer.getHeight()) * getHeight()) :
				(int) (layer.getWidth() / (max + layer.getWidth()) * getWidth());

        return Math.max(size, 10);
	}

	/** {@inheritDoc} */
	@Override
	public void onValueChanged() {
		var value = getMax() <= 0 ? 0 : getValue();

		if (isVertical) {
			getAffectedLayer().setOffsetY(-(int) value);
		} else {
			getAffectedLayer().setOffsetX(-(int) value);
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean isScrollFocused() {
		return (super.isScrollFocused() || layer.isMouseOver())&& getAffectedLayer().isEnabled()&&isEnabled()&&isVisible();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isVisible() {
		return getScrollBarSize() > 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEnabled() {
		return getScrollBarSize() > 0&&isVisible;
	}

	/** 滚动条可见性标记 / Scroll bar visibility flag */
    boolean isVisible = true;

    /**
     * 隐藏滚动条。
     * <p>
     * Hides the scroll bar.
     */
    public void hide() {
        this.isVisible = false;
    }

    /**
     * 显示滚动条。
     * <p>
     * Shows the scroll bar.
     */
    public void unhide() {
        this.isVisible = true;
    }
    
}