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

import com.teammoeg.chorda.client.MouseHelper;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.widgets.LimitedTextField;
import com.teammoeg.chorda.client.icon.FlatIcon;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.List;

/**
 * 可折叠的分类容器，支持嵌套子分类和条目选择。
 * <p>
 * A collapsible category container that supports nested sub-categories and entry selection.
 */
public class Category extends UILayer {
    /** 子元素的水平偏移量 / Horizontal offset for child elements */
    public static final int CHILDREN_OFFSET = 8;

    @Getter
    private final int depth;
    @Getter
    private final Category root;
    private Entry selected;
    @Getter
    protected boolean opened = false;

    protected final LimitedTextField title;
    protected FlatIcon.FlatIconWidget icon;

    /**
     * 创建一个新的分类。
     * <p>
     * Creates a new category.
     *
     * @param panel 父层容器 / the parent layer container
     * @param title 分类标题 / the category title
     */
    public Category(UILayer panel, Component title) {
        super(panel);
        setSize(panel.getWidth(), Entry.DEF_HEIGHT);

        this.title = new LimitedTextField(this, title, getWidth()).shouldShowTooltip(false);
        this.icon = FlatIcon.RIGHT.toWidget(this, theme().UIAltTextColor());
        this.icon.setSize(10, 10);
        addUIElements();

        if (panel instanceof Category p) {
            root = p.root;
            depth = p.depth + 1;
        } else {
            depth = 0;
            root = this;
        }

        if (!panel.getElements().contains(this)) {
            panel.add(this);
        }
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        theme().drawButton(graphics, x, y, w, Entry.DEF_HEIGHT, false, isEnabled());
        if (isMouseOver()) {
            graphics.fill(x-4, y, x-2, y+Entry.DEF_HEIGHT, theme().UIAltTextColor());
        }
    }

    @Override
    public void refresh() {
        setWidth(parent.getWidth() - (parent instanceof Category ? CHILDREN_OFFSET : 0));
        setOffsetY(Entry.DEF_HEIGHT);
        setOffsetX(CHILDREN_OFFSET);

        if (opened) {
            for (UIElement ele : elements) {
                ele.refresh();
            }
            alignWidgets();
            setHeight(getContentHeight() + Entry.DEF_HEIGHT);
        } else {
            setHeight(Entry.DEF_HEIGHT);
        }

        icon.setPos(-CHILDREN_OFFSET, -Entry.DEF_HEIGHT + 3);
        int titleOffsetX = 4;
        title.setPos(titleOffsetX, -Entry.DEF_HEIGHT + 4);
        title.setWidth(getWidth() - titleOffsetX - CHILDREN_OFFSET);
    }

    /**
     * 批量添加子元素到此分类。
     * <p>
     * Adds all child elements to this category in batch.
     *
     * @param widgets 要添加的子元素集合 / the collection of child elements to add
     */
    public void addAll(Collection<? extends UIElement> widgets) {
        widgets.forEach(this::add);
    }

    @Override
    public void addUIElements() {
        add(this.title);
        add(this.icon);
    }

    @Override
    public void alignWidgets() {
        if (opened) {
            align(2, 2, false, icon, title);
        } else {
            recalcContentSize();
        }
    }

    /**
     * 通过路径查找子元素。路径以"/"分隔各层级名称。
     * <p>
     * Finds a child element by path. The path uses "/" to separate level names.
     *
     * @param path 以"/"分隔的路径字符串 / a "/" separated path string
     * @return 路径末端的元素，未找到则返回null / the element at the end of the path, or null if not found
     */
    public UIElement find(String path) {
        if (path == null || path.isBlank()) return null;

        String[] segments = path.split("/");
        Category currentCategory = root;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) continue;

            List<UIElement> elements = currentCategory.getElements();
            UIElement found = null;

            for (UIElement element : elements) {
                if (segment.equals(CategoryHelper.getRawTitle(element))) {
                    found = element;
                    break;
                }
            }

            if (found == null) return null;

            if (i == segments.length - 1) {
                return found;
            } else if (found instanceof Category) {
                currentCategory = (Category) found;
            } else {
                return null;
            }
        }

        return null;
    }

    /**
     * 打开路径中的所有分类，并选中末端条目。
     * <p>
     * Opens all categories along the path and selects the terminal entry.
     *
     * @param path 以"/"分隔的路径字符串 / a "/" separated path string
     * @return 路径末端的元素，未找到则返回null / the element at the end of the path, or null if not found
     */
    public UIElement open(String path) {
        UIElement widget = find(path);
        if (widget == null) {
            return null;
        } else if (widget instanceof Category category) {
            category.setOpened(true);
        }

        UIElement parent = widget.getParent();
        while (parent instanceof Category category) {
            category.setOpened(true);
            parent = parent.getParent();
        }

        if (widget instanceof Entry entry) {
            select(entry);
        }
        return widget;
    }

    /**
     * 设置分类的展开/折叠状态。
     * <p>
     * Sets the opened/collapsed state of this category.
     *
     * @param opened true为展开，false为折叠 / true to expand, false to collapse
     */
    public void setOpened(boolean opened) {
        if (this.opened != opened) {
            this.opened = opened;
            icon.setIcon(opened ? FlatIcon.DOWN : FlatIcon.RIGHT);
            root.parent.refresh();
        }
    }

    /**
     * 选中一个条目。
     * <p>
     * Selects an entry.
     *
     * @param widget 要选中的条目 / the entry to select
     */
    public void select(Entry widget) {
        root.selected = widget;
    }

    /**
     * 获取选中的条目
     */
    public Entry getSelected() {
        return root.selected;
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) return false;

        if (MouseHelper.isMouseIn(getMouseX(), getMouseY(), 0, 0, getWidth(), Entry.DEF_HEIGHT)) {
            switch (button) {
                case LEFT -> {
                    setOpened(!opened);
                    return true;
                }
                case RIGHT -> {
                    setOpened(false);
                    return true;
                }
            }
        }

        for (int i = elements.size() - 1; i >= 0; i--) {
            UIElement element = elements.get(i);
            if (element.isEnabled() && element.isVisible() && element.onMousePressed(button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        if (isEnabled() && isVisible() && MouseHelper.isMouseIn(getMouseX(), getMouseY(), 0, 0, getWidth(), Entry.DEF_HEIGHT)) {
            list.accept(getTitle());
        }
        super.getTooltip(list);
    }

    @Override
    public boolean isVisible() {
        return getParent() instanceof Category p ? p.isOpened() : super.isVisible();
    }

    /**
     * 设置分类标题。
     * <p>
     * Sets the category title.
     *
     * @param title 新标题 / the new title
     */
    public void setTitle(Component title) {
        this.title.setTitle(title);
    }

    @Override
    public Component getTitle() {
        return title.getTitle();
    }
}
