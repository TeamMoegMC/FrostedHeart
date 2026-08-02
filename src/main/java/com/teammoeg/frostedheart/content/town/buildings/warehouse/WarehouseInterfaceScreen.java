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

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class WarehouseInterfaceScreen extends MenuPrimaryLayer<WarehouseInterfaceMenu> {
    private final WarehouseInterfaceTargetElement[] targets =
            new WarehouseInterfaceTargetElement[WarehouseInterfaceBlockEntity.SLOT_COUNT];
    private final RedstoneModeButton redstoneModeButton;

    public WarehouseInterfaceScreen(WarehouseInterfaceMenu menu) {
        super(menu);
        setSize(176, 166);
        for (int slot = 0; slot < targets.length; slot++) {
            targets[slot] = new WarehouseInterfaceTargetElement(this, menu, slot);
        }
        redstoneModeButton = new RedstoneModeButton(this, menu);
    }

    @Override
    public void addChildUIElements() {
        for (WarehouseInterfaceTargetElement target : targets) {
            add(target);
        }
        add(redstoneModeButton);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x, y, x + w, y + h, 0xffc6c6c6);
        graphics.fill(x + 4, y + 39, x + 172, y + 67, 0xff9b9b9b);
        graphics.fill(x + 4, y + 78, x + 172, y + 163, 0xff9b9b9b);

        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT; slot++) {
            int slotX = x + 8 + slot * 18;
            int slotY = y + 46;
            drawSlotBackground(graphics, slotX, slotY);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBackground(graphics, x + 8 + column * 18, y + 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotBackground(graphics, x + 8 + column * 18, y + 142);
        }

        graphics.drawString(getFont(), Component.translatable("container.frostedheart.warehouse_interface"),
                x + 8, y + 5, 0x404040, false);
        Component status = switch (menu.getConnectionStatus()) {
            case WarehouseInterfaceBlockEntity.STATUS_WORKING ->
                    Component.translatable("gui.frostedheart.warehouse_interface.status.working");
            case WarehouseInterfaceBlockEntity.STATUS_UNAVAILABLE ->
                    Component.translatable("gui.frostedheart.warehouse_interface.status.unavailable");
            default -> Component.translatable("gui.frostedheart.warehouse_interface.status.unbound");
        };
        int statusColor = switch (menu.getConnectionStatus()) {
            case WarehouseInterfaceBlockEntity.STATUS_WORKING -> 0x287a28;
            case WarehouseInterfaceBlockEntity.STATUS_UNAVAILABLE -> 0xa06a00;
            default -> 0x9a2828;
        };
        graphics.drawString(getFont(), status, x + 8, y + 69, statusColor, false);
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
    }

    /**
     * 红石控制模式切换按钮：点击在忽略 / 有信号才输出 / 无信号才输出之间循环。
     * 仅影响从城镇仓库补货输出，存回仓库不受门控。
     * <p>
     * Redstone control mode toggle: cycles ignore / output while powered / output while
     * unpowered. Only restocking output is gated; storing back is not.
     */
    private static class RedstoneModeButton extends UIElement {
        private final WarehouseInterfaceMenu menu;

        RedstoneModeButton(UIElement parent, WarehouseInterfaceMenu menu) {
            super(parent);
            this.menu = menu;
            setPosAndSize(102, 3, 68, 13);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xff373737);
            graphics.fill(x, y, x + w, y + h, isMouseOver() ? 0xffa0a0a0 : 0xff8b8b8b);

            Component label = Component.translatable(switch (menu.getRedstoneMode()) {
                case HIGH_SIGNAL -> "gui.frostedheart.warehouse_interface.redstone_mode.high";
                case LOW_SIGNAL -> "gui.frostedheart.warehouse_interface.redstone_mode.low";
                default -> "gui.frostedheart.warehouse_interface.redstone_mode.ignore";
            });
            Font font = getFont();
            graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, 0xffffff, true);
        }

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver()) {
                return super.onMousePressed(button);
            }
            if (button == MouseButton.LEFT) {
                menu.cycleRedstoneMode();
                return true;
            }
            return false;
        }

        @Override
        public void getTooltip(TooltipBuilder tooltip) {
            tooltip.accept(Component.translatable(switch (menu.getRedstoneMode()) {
                        case HIGH_SIGNAL -> "gui.frostedheart.warehouse_interface.redstone_mode.high.tooltip";
                        case LOW_SIGNAL -> "gui.frostedheart.warehouse_interface.redstone_mode.low.tooltip";
                        default -> "gui.frostedheart.warehouse_interface.redstone_mode.ignore.tooltip";
                    }).withStyle(ChatFormatting.GRAY));
            super.getTooltip(tooltip);
        }
    }
}
