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

package com.teammoeg.chorda.client.cui.contentpanel;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.chorda.client.cui.widgets.LayerScrollBar;
import com.teammoeg.chorda.client.cui.widgets.ScrollBar;
import com.teammoeg.frostedheart.content.archive.Alignment;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * 内容面板，用于展示可滚动的富文本内容（文字、图片、物品等）。
 * 自动管理滚动条，支持通过Builder模式流式构建内容行。
 * 面板尺寸默认按屏幕高度80%的4:3比例计算。
 * <p>
 * Content panel for displaying scrollable rich content (text, images, items, etc.).
 * Automatically manages a scrollbar and supports fluent content line building via
 * the Builder pattern. Panel size defaults to a 4:3 ratio at 80% of screen height.
 */
public class ContentPanel extends UILayer {
    public ScrollBar scrollBar;
    @Getter
    protected List<Line<?>> lines = new ArrayList<>();
    @Getter
    @Setter
    private boolean visible=true;
    public ContentPanel(UIElement parent) {
        super(parent);
        this.scrollBar = new LayerScrollBar(parent, true, this);
        resize();
    }

    public ContentPanel(UIElement parent, Theme theme) {
        this(parent);
        setTheme(theme);
    }

    public void setParent(UILayer parent) {
        super.setParent(parent);
        parent.add(this);
        parent.add(scrollBar);
    }

    public Builder builder() {
    	return new Builder(this);
    }

    public static Builder builder(UIElement parent) {
    	return new Builder(new ContentPanel(parent));
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        hint.theme(this).drawUIBackground(graphics, x-8, y-8, w+16, h+16);
    }

    public void fillContent(Collection<? extends UIElement> widgets) {
        int old = getContentHeight();
        clearElement();
        for (UIElement widget : widgets) {
            if (widget instanceof Line<?> line) {
                this.lines.add(line);
            }
            add(widget);
        }
        refresh();
        if (old != getContentHeight()) {
            scrollBar.setValue(0);
        }
    }
    public void copy(Collection<? extends UIElement> widgets) {
        int old = getContentHeight();
        clearElement();
        for (UIElement widget : widgets) {
            if (widget instanceof Line<?> line) {
                this.lines.add(line);
            }
            add(widget);
        }
        refresh();
        if (old != getContentHeight()) {
            scrollBar.setValue(0);
        }
    }
    public void addLine(Line<?> line) {
        this.lines.add(line);
        add(line);
        refresh();
    }

    public void addLines(Collection<? extends Line<?>> lines) {
        this.lines.addAll(lines);
        lines.forEach(this::add);
        refresh();
    }

    @Override
    public void refresh() {
        resize();
        for (UIElement element : elements) {
            element.refresh();
        }
        alignWidgets();
    }

    public void resize() {
        int h = (int)(ClientUtils.screenHeight() * 0.8F);
        int w = (int)(h * 1.3333F); // 4:3
        setSize(w, h);
        scrollBar.setPosAndSize(getX() + getWidth()+7, -7, 6, getHeight()+14);
    }

    @Override
    public void getTooltip(TooltipBuilder list) {
        super.getTooltip(list);
//        list.translateZ(300);
    }

    @Override
    public void alignWidgets() {
        align(4, false);
    }

    @Override
    public void addUIElements() {}

    public static class Builder{
        private final ContentPanel parent;
        public Builder(ContentPanel parent) {
            super();
            this.parent = parent;
        }
        public <T extends Line<?>>Builder add(T line,Consumer<T> config){
            config.accept(line);
            parent.lines.add(line);
            parent.add(line);
            return this;
        }
        public  Builder text(String text) {
            return text(text,a->{});
        }

        public  Builder text(Component text) {
            return text(text,a->{});
        }
        public  Builder text(String text,Consumer<TextLine> config) {
            return text(Component.literal(text),config);
        }

        public  Builder text(Component text,Consumer<TextLine> config) {
            return add(new TextLine(parent, text, Alignment.LEFT),config);
        }

        public  Builder img(ResourceLocation imageLocation) {
            return img(imageLocation, a->{});
        }
        public  Builder img(String imageLocation) {
            return img(imageLocation, a->{});
        }

        public  Builder img(String imageLocation,Consumer<ImageLine> config) {
            return img(ResourceLocation.tryParse(imageLocation), config);
        }

        public  Builder img(ResourceLocation imageLocation,Consumer<ImageLine> config) {
            return add(new ImageLine(parent, imageLocation, Alignment.CENTER),config);
        }
        public  Builder items(ItemStack... items) {
            return items(a->{},items);
        }

        public  Builder items(Collection<ItemStack> items) {
            return items(items, a->{});
        }
        public  Builder items(Consumer<ItemRow> config,ItemStack... items) {
            return items(List.of(items),config);
        }

        public  Builder items(Collection<ItemStack> items,Consumer<ItemRow> config) {
            return add(new ItemRow(parent, items, Alignment.CENTER),config);
        }
        public  Builder space() {
            return space(a->{});
        }

        public  Builder space(int height) {
            return space(height,a->{});
        }
        public  Builder space(Consumer<EmptyLine> config) {
            return space(8,config);
        }

        public  Builder space(int height,Consumer<EmptyLine> config) {
            return add(new EmptyLine(parent, height),config);
        }
        public  Builder br() {
            return br(a->{});
        }

        public  Builder br(int color) {
            return br(color,a->{});
        }
        public  Builder br(Consumer<BreakLine> config) {
            return add(new BreakLine(parent),config);
        }

        public  Builder br(int color,Consumer<BreakLine> config) {
            return br(c->{c.color(color);config.accept(c);});
        }
        public ContentPanel build() {
            parent.refresh();
            return parent;
        }
    }
}
