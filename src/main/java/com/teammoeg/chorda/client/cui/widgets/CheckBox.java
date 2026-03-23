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

import com.teammoeg.chorda.client.CInputHelper.Cursor;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.icon.CIcons.CIcon;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 复选框控件。支持选中和未选中两种状态切换，使用不同图标表示各状态。
 * <p>
 * Checkbox widget. Supports toggling between checked and unchecked states,
 * using different icons to represent each state.
 */
public class CheckBox extends UIElement {
	/** 当前是否选中 / Whether currently checked */
	protected boolean checked;
	/** 未选中状态图标和选中状态图标 / Icon for unchecked state and checked state */
    protected CIcon uncheckedIcon, checkedIcon;


    /**
     * 创建复选框控件。
     * <p>
     * Creates a checkbox widget.
     *
     * @param panel 父级UI元素 / Parent UI element
     * @param uncheckedIcon 未选中状态图标 / Icon for unchecked state
     * @param checkedIcon 选中状态图标 / Icon for checked state
     * @param checked 初始选中状态 / Initial checked state
     */
    public CheckBox(UIElement panel, CIcon uncheckedIcon, CIcon checkedIcon, boolean checked) {
        super(panel);
        this.checked = checked;
        this.uncheckedIcon = uncheckedIcon;
        this.checkedIcon = checkedIcon;
    }

    /** {@inheritDoc} */
    @Override
	public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        if (checked)
        	checkedIcon.draw(graphics, x, y, w, h);
        else
        	uncheckedIcon.draw(graphics, x, y, w, h);
	}

    /**
     * 获取当前选中状态。
     * <p>
     * Gets the current checked state.
     *
     * @return 是否选中 / Whether checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * 设置选中状态。如果状态发生变化，会触发 {@link #onSwitched()} 回调。
     * <p>
     * Sets the checked state. Triggers {@link #onSwitched()} callback if the state changes.
     *
     * @param checked 新的选中状态 / New checked state
     */
    public void setChecked(boolean checked) {
    	boolean isSwitched=false;
    	if(checked!=this.checked)
    		isSwitched=true;
    	this.checked = checked;
    	if(isSwitched)
    		onSwitched();
    }

    /** {@inheritDoc} */
    @Override
	public boolean onMousePressed(MouseButton button) {
    	if(!isMouseOver())return false;
    	if(!isEnabled())return false;
    	checked = !checked;
        onSwitched();
		return true;
	}

    /**
     * 选中状态切换时的回调方法。子类可重写此方法以实现自定义逻辑。
     * <p>
     * Callback method when the checked state is switched. Subclasses can override for custom logic.
     */
    public void onSwitched() {

    }

	/** {@inheritDoc} */
	@Override
	public Cursor getCursor() {
		return Cursor.HAND;
	}


}
