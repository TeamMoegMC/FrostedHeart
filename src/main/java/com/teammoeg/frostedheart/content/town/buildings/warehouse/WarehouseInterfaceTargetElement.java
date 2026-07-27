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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

class WarehouseInterfaceTargetElement extends UIElement {
    private final WarehouseInterfaceMenu menu;
    private final int slot;
    private final ScrollTracker scrollTracker = new ScrollTracker();

    WarehouseInterfaceTargetElement(UIElement parent, WarehouseInterfaceMenu menu, int slot) {
        super(parent);
        this.menu = menu;
        this.slot = slot;
        setPosAndSize(8 + slot * 18, 18, 16, 16);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);

        WarehouseInterfaceTarget target = menu.getTarget(slot);
        if (target != null) {
            ItemStack display = target.key().toStack(1);
            graphics.renderItem(display, x, y);
            if (target.amount() != 1) {
                Font font = getFont();
                String amount = Integer.toString(target.amount());
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 350);
                graphics.drawString(font, amount, x + 17 - font.width(amount), y + 9, 0xffffff, true);
                graphics.pose().popPose();
            }
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
            menu.clearTarget(slot);
            return true;
        }
        if (button == MouseButton.LEFT && !menu.getCarried().isEmpty()) {
            menu.setTargetFromCarried(slot);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) {
            return super.onMouseScrolled(scroll);
        }
        WarehouseInterfaceTarget target = menu.getTarget(slot);
        if (target == null) {
            return false;
        }

        scrollTracker.addScroll(scroll);
        int wheel = scrollTracker.getScroll();
        if (wheel != 0) {
            int increment = getAdjustIncrement();
            int newAmount = Mth.clamp(target.amount() + wheel * increment, 1,
                    target.key().toStack(1).getMaxStackSize());
            menu.setTargetAmount(slot, newAmount);
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
        WarehouseInterfaceTarget target = menu.getTarget(slot);
        if (target != null) {
            Screen.getTooltipFromItem(Minecraft.getInstance(), target.key().toStack(1)).forEach(tooltip::accept);
            tooltip.accept(Component.translatable("gui.frostedheart.warehouse_interface.target_amount", target.amount())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("gui.frostedheart.warehouse_interface.adjust_hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.accept(Component.translatable("gui.frostedheart.warehouse_interface.empty_target")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.getTooltip(tooltip);
    }
}
