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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class WarehouseInterfaceScreen extends MenuPrimaryLayer<WarehouseInterfaceMenu> {
    private final WarehouseInterfaceTargetElement[] targets =
            new WarehouseInterfaceTargetElement[WarehouseInterfaceBlockEntity.SLOT_COUNT];

    public WarehouseInterfaceScreen(WarehouseInterfaceMenu menu) {
        super(menu);
        setSize(176, 166);
        for (int slot = 0; slot < targets.length; slot++) {
            targets[slot] = new WarehouseInterfaceTargetElement(this, menu, slot);
        }
    }

    @Override
    public void addChildUIElements() {
        for (WarehouseInterfaceTargetElement target : targets) {
            add(target);
        }
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
}
