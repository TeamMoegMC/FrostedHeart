/*
 * Copyright (c) 2024 TeamMoeg
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

import com.teammoeg.chorda.client.cui.LimitedTextField;
import com.teammoeg.chorda.client.cui.MouseButton;
import com.teammoeg.chorda.client.cui.TooltipBuilder;
import com.teammoeg.chorda.client.cui.UIElement;
import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.client.ui.Colors;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class Entry extends UILayer {
    public static final int DEF_HEIGHT = 16;
    @Getter
    protected final UILayer panel;
    protected LimitedTextField title;
    protected FlatIcon.FlatIconWidget icon;

    public Entry(Category parent, UILayer affectedPanel, Component title) {
        super(parent);
        this.panel = affectedPanel;
        this.title = new LimitedTextField(this, title, getWidth()).shouldShowTooltip(false);
        this.icon = new FlatIcon.FlatIconWidget(this);
        this.icon.setSize(10, 10);
        this.icon.setColor(Colors.L_TEXT_GRAY);
        addUIElements();
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x+w, y+h, Colors.L_BG_GRAY);
        if (isMouseOver() && isEnabled()) {
            graphics.fill(x-4, y, x-2, y+h, Colors.L_TEXT_GRAY);
        }
        if (getParent().selected == this) {
            graphics.fill(x-4, y, x-2, y+h, Colors.L_BG_GREEN);
        }
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth()-8);
        setHeight(DEF_HEIGHT);
        int offsetX = 4;
        setOffsetX(offsetX);
        int y = Math.round((getHeight() - title.getHeight())*0.5F);

        icon.setPos(-offsetX + 1, 3);
        int titleOffsetX = icon.hasIcon() ? 9 : 0;
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
        add(this.icon);
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

    public Entry setIcon(FlatIcon icon) {
        this.icon.setIcon(icon);
        refresh();
        return this;
    }

    public Entry setTitle(Component title) {
        this.title.setTitle(title);
        return this;
    }

    @Override
    public Component getTitle() {
        return this.title.getTitle();
    }

    @Override
    public boolean isVisible() {
        return getParent().isOpened() && super.isVisible();
    }

    @Override
    public void alignWidgets() {}
}
