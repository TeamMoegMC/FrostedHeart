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

/**
 * 仓库发信器界面：配置监控物品与阈值、切换发信模式，并显示连接状态、当前存量与输出状态。
 * <p>
 * Warehouse level emitter screen: configures the watched item and threshold, toggles the
 * emitter mode, and shows the connection status, current stock, and output state.
 */
public class WarehouseLevelEmitterScreen extends MenuPrimaryLayer<WarehouseLevelEmitterMenu> {
    private final WarehouseLevelEmitterFilterElement filterElement;
    private final ModeButton modeButton;

    public WarehouseLevelEmitterScreen(WarehouseLevelEmitterMenu menu) {
        super(menu);
        setSize(176, 166);
        filterElement = new WarehouseLevelEmitterFilterElement(this, menu);
        modeButton = new ModeButton(this, menu);
    }

    @Override
    public void addChildUIElements() {
        add(filterElement);
        add(modeButton);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x, y, x + w, y + h, 0xffc6c6c6);
        graphics.fill(x + 4, y + 16, x + 172, y + 67, 0xff9b9b9b);
        graphics.fill(x + 4, y + 78, x + 172, y + 163, 0xff9b9b9b);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBackground(graphics, x + 8 + column * 18, y + 84 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotBackground(graphics, x + 8 + column * 18, y + 142);
        }

        Font font = getFont();
        graphics.drawString(font, Component.translatable("container.frostedheart.warehouse_level_emitter"),
                x + 8, y + 5, 0x404040, false);

        graphics.drawString(font, Component.translatable("gui.frostedheart.warehouse_level_emitter.threshold",
                menu.getThreshold()), x + 30, y + 24, 0x404040, false);

        Component status = switch (menu.getConnectionStatus()) {
            case WarehouseLevelEmitterBlockEntity.STATUS_WORKING ->
                    Component.translatable("gui.frostedheart.warehouse_level_emitter.status.working");
            case WarehouseLevelEmitterBlockEntity.STATUS_UNAVAILABLE ->
                    Component.translatable("gui.frostedheart.warehouse_level_emitter.status.unavailable");
            default -> Component.translatable("gui.frostedheart.warehouse_level_emitter.status.unbound");
        };
        int statusColor = switch (menu.getConnectionStatus()) {
            case WarehouseLevelEmitterBlockEntity.STATUS_WORKING -> 0x287a28;
            case WarehouseLevelEmitterBlockEntity.STATUS_UNAVAILABLE -> 0xa06a00;
            default -> 0x9a2828;
        };
        graphics.drawString(font, status, x + 8, y + 44, statusColor, false);

        graphics.drawString(font, Component.translatable("gui.frostedheart.warehouse_level_emitter.current_stock",
                menu.getCurrentStock()), x + 8, y + 56, 0x404040, false);

        Component output = menu.isEmitterOn()
                ? Component.translatable("gui.frostedheart.warehouse_level_emitter.output.on")
                : Component.translatable("gui.frostedheart.warehouse_level_emitter.output.off");
        graphics.drawString(font, output, x + 100, y + 56, menu.isEmitterOn() ? 0xb02828 : 0x606060, false);
    }

    private static void drawSlotBackground(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xff373737);
        graphics.fill(x, y, x + 16, y + 16, 0xff8b8b8b);
    }

    /**
     * 发信模式切换按钮：点击在"存量≥阈值时发信"与"存量<阈值时发信"之间切换。
     * <p>
     * Emitter mode toggle button: switches between emitting at or above the threshold
     * and emitting below the threshold.
     */
    private static class ModeButton extends UIElement {
        private final WarehouseLevelEmitterMenu menu;

        ModeButton(UIElement parent, WarehouseLevelEmitterMenu menu) {
            super(parent);
            this.menu = menu;
            setPosAndSize(100, 20, 68, 16);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xff373737);
            graphics.fill(x, y, x + w, y + h, isMouseOver() ? 0xffa0a0a0 : 0xff8b8b8b);

            Component label = menu.getMode() == WarehouseRedstoneMode.LOW_SIGNAL
                    ? Component.translatable("gui.frostedheart.warehouse_level_emitter.mode.low")
                    : Component.translatable("gui.frostedheart.warehouse_level_emitter.mode.high");
            Font font = getFont();
            graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, 0xffffff, true);
        }

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver()) {
                return super.onMousePressed(button);
            }
            if (button == MouseButton.LEFT) {
                menu.cycleMode();
                return true;
            }
            return false;
        }

        @Override
        public void getTooltip(TooltipBuilder tooltip) {
            tooltip.accept(Component.translatable(menu.getMode() == WarehouseRedstoneMode.LOW_SIGNAL
                            ? "gui.frostedheart.warehouse_level_emitter.mode.low.tooltip"
                            : "gui.frostedheart.warehouse_level_emitter.mode.high.tooltip")
                    .withStyle(ChatFormatting.GRAY));
            super.getTooltip(tooltip);
        }
    }
}
