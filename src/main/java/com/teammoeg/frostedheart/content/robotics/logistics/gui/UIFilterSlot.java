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

package com.teammoeg.frostedheart.content.robotics.logistics.gui;

import com.teammoeg.chorda.client.cui.widgets.AbstractFilterGhostSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.frostedheart.content.robotics.logistics.Filter;

import net.minecraft.world.item.ItemStack;

public class UIFilterSlot extends AbstractFilterGhostSlot {
    private final LogisticInterfaceChestOutMenu menu;
    private final int index;
    private final FilterLayer layer;
    private ItemStack displayStack = ItemStack.EMPTY;

    public UIFilterSlot(FilterLayer parent, LogisticInterfaceChestOutMenu menu, int index) {
        super(parent);
        this.menu = menu;
        this.index = index;
        this.layer = parent;

        CDataSlot<Filter> slot = menu.list.get(index);
        slot.bind(c -> {
            if (c == null)
                displayStack = ItemStack.EMPTY;
            else
                displayStack = c.getDisplayItem();
        });
        if (slot.getValue() != null)
            displayStack = slot.getValue().getDisplayItem();
        else
            displayStack = ItemStack.EMPTY;
    }

    @Override
    protected ItemStack getDisplayStack() {
        return displayStack;
    }

    @Override
    protected int getCurrentValue() {
        Filter filter = getFilter();
        return filter != null ? filter.getSize() : 0;
    }

    @Override
    protected void setValue(int newValue) {
        menu.setFilterSize(index, newValue);
    }

    @Override
    protected void setFilterFromCarried() {
        menu.setFilterItem(index);
        menu.setFilterSize(index, 1);   // 与原始行为一致
    }

    @Override
    protected void clearFilter() {
        menu.unsetFilterItem(index);
    }

    @Override
    protected ItemStack getMenuCarried() {
        return menu.getCarried();
    }

    @Override
    protected void onLeftClickWithoutCarried() {
        // 打开过滤器子层
        if (getFilter() != null) {
            layer.layer.openFilterLayer(index);
        }
    }

    public Filter getFilter() {
        return menu.list.get(index).getValue();
    }

}
