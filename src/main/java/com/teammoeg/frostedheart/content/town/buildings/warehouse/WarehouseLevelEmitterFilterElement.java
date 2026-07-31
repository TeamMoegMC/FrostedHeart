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
class WarehouseLevelEmitterFilterElement extends UIElement {
    private final WarehouseLevelEmitterMenu menu;
    private final ScrollTracker scrollTracker = new ScrollTracker();

    WarehouseLevelEmitterFilterElement(UIElement parent, WarehouseLevelEmitterMenu menu) {
        super(parent);
        this.menu = menu;
        setPosAndSize(8, 20, 16, 16);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);

        SimpleItemKey filter = menu.getFilter();
        if (filter != null) {
            graphics.renderItem(filter.toStack(1), x, y);
        }

        if (isMouseOver()) {
            graphics.fill(x, y, x + 16, y + 16, 0x66ffffff);
        }
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver()) {
            return super.onMousePressed(button);
        }
        if (button == MouseButton.RIGHT) {
            menu.clearFilter();
            return true;
        }
        if (button == MouseButton.LEFT && !menu.getCarried().isEmpty()) {
            menu.setFilterFromCarried();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) {
            return super.onMouseScrolled(scroll);
        }
        scrollTracker.addScroll(scroll);
        int wheel = scrollTracker.getScroll();
        if (wheel != 0) {
            long newThreshold = (long) menu.getThreshold() + (long) wheel * getAdjustIncrement();
            menu.setThreshold((int) Mth.clamp(newThreshold, 1, Integer.MAX_VALUE));
        }
        return true;
    }

    private int getAdjustIncrement() {
        boolean shift = CInputHelper.isShiftKeyDown();
        boolean control = CInputHelper.isCtrlKeyDown();
        if (shift && control) {
            return 64;
        }
        if (control) {
            return 16;
        }
        return shift ? 8 : 1;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        SimpleItemKey filter = menu.getFilter();
        if (filter != null) {
            ItemStack display = filter.toStack(1);
            Screen.getTooltipFromItem(Minecraft.getInstance(), display).forEach(tooltip::accept);
            tooltip.accept(Component.translatable("gui.frostedheart.warehouse_level_emitter.threshold", menu.getThreshold())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("gui.frostedheart.warehouse_level_emitter.adjust_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.accept(Component.translatable("gui.frostedheart.warehouse_level_emitter.empty_filter")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.getTooltip(tooltip);
    }
}
