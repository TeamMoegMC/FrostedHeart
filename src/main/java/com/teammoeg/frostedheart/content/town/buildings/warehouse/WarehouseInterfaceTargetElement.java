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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

class WarehouseInterfaceTargetElement extends AbstractFilterGhostSlot {
    private final WarehouseInterfaceMenu menu;
    private final int slot;

    WarehouseInterfaceTargetElement(UIElement parent, WarehouseInterfaceMenu menu, int slot) {
        super(parent);
        this.menu = menu;
        this.slot = slot;
        setPosAndSize(8 + slot * 18, WarehouseInterfaceMenu.TARGET_FILTER_Y, 16, 16);
    }

    @Override
    protected ItemStack getDisplayStack() {
        WarehouseInterfaceTarget t = menu.getTarget(slot);
        return t != null ? t.key().toStack(1) : ItemStack.EMPTY;
    }

    @Override
    protected int getCurrentValue() {
        WarehouseInterfaceTarget t = menu.getTarget(slot);
        return t != null ? t.amount() : 0;
    }

    @Override
    protected void setValue(int newValue) {
        menu.setTargetAmount(slot, newValue);
    }

    @Override
    protected void setFilterFromCarried() {
        menu.setTargetFromCarried(slot);
    }

    @Override
    protected void clearFilter() {
        menu.clearTarget(slot);
    }

    @Override
    protected ItemStack getMenuCarried() {
        return menu.getCarried();
    }

    @Override
    protected int getMaxValue() {
        WarehouseInterfaceTarget t = menu.getTarget(slot);
        return t != null ? t.key().toStack(1).getMaxStackSize() : 64;
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
    protected boolean shouldDrawCount() {
        return super.shouldDrawCount();
    }

    @Override
    protected void onLeftClickWithoutCarried() {
        clearFilter();
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
    }

}
