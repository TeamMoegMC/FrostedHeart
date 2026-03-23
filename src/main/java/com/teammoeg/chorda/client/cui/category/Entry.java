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

package com.teammoeg.chorda.client.cui.category;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.LimitedTextField;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.math.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * 分类条目的抽象基类。可在分类中被选中并显示图标和标题。
 * <p>
 * Abstract base class for category entries. Can be selected within a category and displays an icon and title.
 */
public abstract class Entry extends UILayer {
    /** 默认条目高度 / Default entry height */
    public static final int DEF_HEIGHT = 16;
    protected LimitedTextField title;
    protected FlatIcon icon;

    /**
     * 创建一个新的分类条目。
     * <p>
     * Creates a new category entry.
     *
     * @param parent 父分类 / the parent category
     * @param title  条目标题 / the entry title
     */
    public Entry(Category parent, Component title) {
        super(parent);
        this.title = new LimitedTextField(this, title, getWidth()).shouldShowTooltip(false);
        addUIElements();
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
    	hint.theme(this).drawButton(graphics, x, y, w, h, false, isEnabled());
        if (isMouseOver() && isEnabled()) {
            graphics.fill(x-4, y, x-2, y+h, hint.theme(this).UIAltTextColor());
        }
        if (getParent().getSelected() == this) {
            graphics.fill(x-4, y, x-2, y+h, Colors.themeColor());
        }
        if(icon!=null)
        	icon.render(graphics.pose(),x+1, y+3, hint.theme(this).UIAltTextColor());
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth()-8);
        setHeight(DEF_HEIGHT);
        int offsetX = 4;
        setOffsetX(offsetX);
        int y = Math.round((getHeight() - title.getHeight())*0.5F);
        int titleOffsetX = icon!=null ? 9 : 0;
        title.setPos(titleOffsetX, y);
        title.setWidth(getWidth() - offsetX - titleOffsetX);

        recalcContentSize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        boolean pressed = super.onMousePressed(button);
        if (pressed) {
            getParent().select(this);
        }
        return pressed;
    }

    @Override
    public void addUIElements() {
        add(this.title);
    }

    @Override
    public Category getParent() {
        return (Category) super.getParent();
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        if (!hasTooltip() || !isMouseOver() || !isVisible() || !isEnabled()) {
            return;
        }
        list.accept(getTitle());
        super.getTooltip(list);
    }

    /**
     * 设置条目图标。
     * <p>
     * Sets the entry icon.
     *
     * @param icon 要设置的图标 / the icon to set
     * @return 当前条目实例（链式调用） / this entry instance (for chaining)
     */
    public Entry setIcon(FlatIcon icon) {
        this.icon=icon;
        refresh();
        return this;
    }

    /**
     * 设置条目标题。
     * <p>
     * Sets the entry title.
     *
     * @param title 新标题 / the new title
     * @return 当前条目实例（链式调用） / this entry instance (for chaining)
     */
    public Entry setTitle(Component title) {
        this.title.setTitle(title);
        return this;
    }

    @Override
    public Component getTitle() {
        return this.title.getTitle();
    }

    /**
     * 用于在 Category 中区分同名条目
     */
    public abstract String getIdentifier();

    @Override
    public boolean isVisible() {
        return getParent().isOpened() && super.isVisible();
    }

    @Override
    public void alignWidgets() {}
}
