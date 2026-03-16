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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import lombok.Getter;
import lombok.Setter;

/**
 * 三态复选框控件。具有正常、悬停和锁定三种视觉状态的抽象复选框。
 * 锁定状态下不可交互，显示锁定图标。
 * <p>
 * Tristate checkbox widget. An abstract checkbox with three visual states: normal,
 * hover, and locked. Not interactive in locked state and displays the locked icon.
 */
public abstract class TristateCheckBox extends UIElement {
    /** 是否启用（非锁定状态） / Whether enabled (not locked) */
    boolean enabled;
    /** 正常状态图标 / Normal state icon */
    CIcon normal;
    /** 悬停状态图标 / Hover state icon */
    CIcon over;
    /** 锁定状态图标 / Locked state icon */
    CIcon locked;
    /** 自定义提示信息回调 / Custom tooltip callback */
    @Getter
    @Setter
    Consumer<TooltipBuilder> tooltips;

    /**
     * 创建三态复选框。
     * <p>
     * Creates a tristate checkbox.
     *
     * @param panel 父级UI元素 / Parent UI element
     * @param normal 正常状态图标 / Normal state icon
     * @param over 悬停状态图标 / Hover state icon
     * @param locked 锁定状态图标 / Locked state icon
     */
    public TristateCheckBox(UIElement panel, CIcon normal, CIcon over, CIcon locked) {
        super(panel);
        this.normal = normal;
        this.over = over;
        this.locked = locked;
    }

    /** {@inheritDoc} */
    @Override
	public void getTooltip(TooltipBuilder tooltip) {
		super.getTooltip(tooltip);
		if (tooltips != null)
            tooltips.accept(tooltip);
	}

    /** {@inheritDoc} */
    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        if (getEnabled()) {
            if (super.isMouseOver())
                over.draw(graphics, x, y, w, h);
            else
                normal.draw(graphics, x, y, w, h);
        } else
            locked.draw(graphics, x, y, w, h);
	}

    /** {@inheritDoc} */
    @Override
	public Cursor getCursor() {
        if (enabled)
            return Cursor.HAND;
		return super.getCursor();
	}

    /**
     * 获取是否启用（非锁定状态）。
     * <p>
     * Gets whether enabled (not locked).
     *
     * @return 是否启用 / Whether enabled
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * 设置启用状态。
     * <p>
     * Sets the enabled state.
     *
     * @param enabled 是否启用 / Whether enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取锁定状态图标。
     * <p>
     * Gets the locked state icon.
     *
     * @return 锁定图标 / Locked icon
     */
    public CIcon getLocked() {
        return locked;
    }

    /**
     * 设置锁定状态图标。
     * <p>
     * Sets the locked state icon.
     *
     * @param locked 锁定图标 / Locked icon
     */
    public void setLocked(CIcon locked) {
        this.locked = locked;
    }

    /**
     * 获取正常状态图标。
     * <p>
     * Gets the normal state icon.
     *
     * @return 正常图标 / Normal icon
     */
    public CIcon getNormal() {
        return normal;
    }

    /**
     * 设置正常状态图标。
     * <p>
     * Sets the normal state icon.
     *
     * @param normal 正常图标 / Normal icon
     */
    public void setNormal(CIcon normal) {
        this.normal = normal;
    }

    /**
     * 获取悬停状态图标。
     * <p>
     * Gets the hover state icon.
     *
     * @return 悬停图标 / Hover icon
     */
    public CIcon getOver() {
        return over;
    }

    /**
     * 设置悬停状态图标。
     * <p>
     * Sets the hover state icon.
     *
     * @param over 悬停图标 / Hover icon
     */
    public void setOver(CIcon over) {
        this.over = over;
    }

    /**
     * 重置（清除）自定义提示信息回调。
     * <p>
     * Resets (clears) the custom tooltip callback.
     */
    public void resetTooltips() {
        this.tooltips = null;
    }

	/** {@inheritDoc} */
	@Override
	public boolean onMousePressed(MouseButton button) {
		if(!isMouseOver())return false;

		return onClicked(button);
	}

	/**
	 * 复选框被点击时的回调方法。子类可重写以实现自定义点击行为。
	 * <p>
	 * Callback method when the checkbox is clicked. Subclasses can override for custom click behavior.
	 *
	 * @param button 被点击的鼠标按键 / The mouse button that was clicked
	 * @return 是否已处理点击事件 / Whether the click event was handled
	 */
	public boolean onClicked(MouseButton button) {
		return false;
	}
}
