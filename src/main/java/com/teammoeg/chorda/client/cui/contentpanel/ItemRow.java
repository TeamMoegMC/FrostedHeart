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

import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.ItemSlot;
import com.teammoeg.chorda.client.ui.Colors;
import com.teammoeg.frostedheart.content.archive.Alignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ItemRow extends Line<ItemRow> {
    protected final List<ItemStack> items = new ArrayList<>();
    protected int rowSize = 1;
    protected int backgroundColor = Colors.L_BG_GRAY;

    public ItemRow(UIElement parent, Collection<ItemStack> items, Alignment alignment) {
        super(parent, alignment);
        this.items.addAll(items);
        addUIElements();
    }

    public void addItem(ItemStack item) {
        items.add(item);
        elements.add(new ItemSlot(this, item));
    }

    @Override
    public void addUIElements() {
        this.elements.addAll(items.stream().map(item -> new ItemSlot(this, item)).toList());
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x+w, y+h, backgroundColor);
        super.render(graphics, x, y, w, h);
    }

    @Override
    public void refresh() {
        super.refresh();
        rowSize = Math.max(getWidth() / (ItemSlot.ITEM_WIDTH+4), 1);
        setHeight(Math.max((int)Math.ceil((double)items.size() / rowSize), 1) * (ItemSlot.ITEM_HEIGHT+4));
        alignWidgets();
    }

    @Override
    public void alignWidgets() {
        List<List<UIElement>> rows = new ArrayList<>();
        List<UIElement> row = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0 && i % rowSize == 0) {
                rows.add(row);
                row = new ArrayList<>();
            }
            row.add(elements.get(i));
        }
        rows.add(row);

        for (int y = 0; y < rows.size(); y++) {
            List<UIElement> widgets = rows.get(y);
            for (int x = 0; x < widgets.size(); x++) {
                UIElement widget = widgets.get(x);
                switch (alignment) {
                    case LEFT -> widget.setPos(x*(ItemSlot.ITEM_WIDTH +4)+2, y*(ItemSlot.ITEM_HEIGHT+4)+2);
                    case CENTER -> widget.setPos((getWidth()/2 - widgets.size()*10) + x*(ItemSlot.ITEM_WIDTH+4) + 2, y*(ItemSlot.ITEM_HEIGHT+4)+2);
                    case RIGHT -> widget.setPos(getWidth()-16-2 - x*(ItemSlot.ITEM_WIDTH +4), y*(ItemSlot.ITEM_HEIGHT+4)+2);
                }
            }
        }
    }

    public ItemRow bgColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    @Override
    public Component getTitle() {
        var title = Component.empty();
        int max = Math.min(items.size(), 4);
        for (int i = 0; i < max; i++) {
            ItemStack item = items.get(i);
            title.append(item.getDisplayName());
            if (i < max-1) {
                title.append(", ");
            }
        }
        if (items.size() > 4) {
            title.append("...");
        }
        return title;
    }
}
