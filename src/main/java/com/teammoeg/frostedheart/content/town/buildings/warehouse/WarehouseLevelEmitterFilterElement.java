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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.ScrollTracker;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.widgets.AbstractFilterGhostSlot;
import com.teammoeg.chorda.client.icon.FlatIcon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 仓库发信器的过滤物品幽灵槽：左键放入手中物品设置监控物品，右键清除，滚轮调整阈值。
 * <p>
 * Ghost slot for the warehouse level emitter's watched item: left-click with a carried
 * item to set it, right-click to clear, scroll to adjust the threshold.
 */
class WarehouseLevelEmitterFilterElement extends AbstractFilterGhostSlot {
    private final WarehouseLevelEmitterMenu menu;

    WarehouseLevelEmitterFilterElement(UIElement parent, WarehouseLevelEmitterMenu menu) {
        super(parent);
        this.menu = menu;
        setPosAndSize(8, 20, 16, 16);
    }

    @Override
    protected ItemStack getDisplayStack() {
        SimpleItemKey f = menu.getFilter();
        return f != null ? f.toStack(1) : ItemStack.EMPTY;
    }

    @Override
    protected int getCurrentValue() {
        return menu.getThreshold();
    }

    @Override
    protected void setValue(int newValue) {
        menu.setThreshold(newValue);
    }

    @Override
    protected void setFilterFromCarried() {
        menu.setFilterFromCarried();
    }

    @Override
    protected void clearFilter() {
        menu.clearFilter();
    }

    @Override
    protected ItemStack getMenuCarried() {
        return menu.getCarried();
    }

    @Override
    protected int getMaxValue() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getAdjustIncrement() {
        boolean shift = CInputHelper.isShiftKeyDown();
        boolean ctrl = CInputHelper.isCtrlKeyDown();
        if (shift && ctrl) return 64;
        if (ctrl) return 16;
        return shift ? 8 : 1;
    }

    @Override
    protected void onLeftClickWithoutCarried() {
        clearFilter();
    }

    @Override
    protected boolean shouldDrawCount() {
        return false;
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
    }
}
