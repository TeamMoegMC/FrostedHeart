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
import com.teammoeg.chorda.client.cui.base.MenuPrimaryLayer;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.Verifiers;
import com.teammoeg.chorda.client.cui.widgets.TextBox;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class WarehouseInterfaceScreen extends MenuPrimaryLayer<WarehouseInterfaceMenu> {
    private final WarehouseInterfaceTargetElement[] targets =
            new WarehouseInterfaceTargetElement[WarehouseInterfaceBlockEntity.SLOT_COUNT];
    private final RedstoneModeButton redstoneModeButton;
    private final RateInput rateInput;
    private final RateButton applyRateButton;

    public WarehouseInterfaceScreen(WarehouseInterfaceMenu menu) {
        super(menu);
        setSize(176, WarehouseInterfaceMenu.SCREEN_HEIGHT);
        for (int slot = 0; slot < targets.length; slot++) {
            targets[slot] = new WarehouseInterfaceTargetElement(this, menu, slot);
        }
        redstoneModeButton = new RedstoneModeButton(this, menu);
        rateInput = new RateInput(this);
        rateInput.setRateRange(WarehouseInterfaceTransportView.EMPTY.maximumRateItemsPerSecond(), 0);
        rateInput.setPosAndSize(40, 20, 38, 14);
        applyRateButton = new RateButton(this, 82, 20, 30, 14);
    }

    @Override
    public void addChildUIElements() {
        for (WarehouseInterfaceTargetElement target : targets) {
            add(target);
        }
        add(redstoneModeButton);
        add(rateInput);
        add(applyRateButton);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
        graphics.fill(x, y, x + w, y + h, 0xffc6c6c6);
        graphics.fill(x + 4, y + WarehouseInterfaceMenu.TARGET_FILTER_Y - 3,
                x + 172, y + WarehouseInterfaceMenu.INTERFACE_SLOT_Y + 20, 0xff9b9b9b);
        graphics.fill(x + 4, y + WarehouseInterfaceMenu.PLAYER_INVENTORY_Y - 6,
                x + 172, y + WarehouseInterfaceMenu.SCREEN_HEIGHT - 3, 0xff9b9b9b);

        for (int slot = 0; slot < WarehouseInterfaceBlockEntity.SLOT_COUNT; slot++) {
            int slotX = x + 8 + slot * 18;
            int slotY = y + WarehouseInterfaceMenu.INTERFACE_SLOT_Y;
            drawSlotBackground(graphics, slotX, slotY);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotBackground(graphics, x + 8 + column * 18,
                        y + WarehouseInterfaceMenu.PLAYER_INVENTORY_Y + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlotBackground(graphics, x + 8 + column * 18,
                    y + WarehouseInterfaceMenu.PLAYER_HOTBAR_Y);
        }

        graphics.drawString(getFont(), Component.translatable("container.frostedheart.warehouse_interface"),
                x + 8, y + 5, 0x404040, false);
        graphics.drawString(getFont(), Component.translatable("gui.frostedheart.warehouse_interface.rate"),
                x + 8, y + 23, 0x404040, false);

        WarehouseInterfaceTransportView view = menu.getTransportView();
        rateInput.setRateRange(view.maximumRateItemsPerSecond(), view.rateItemsPerSecond());
        String acceptedRate = rateInputText(view);
        if (!rateInput.isFocused() && !acceptedRate.equals(rateInput.getText())) {
            rateInput.setText(acceptedRate, false);
        }
        Component status = statusComponent(view);
        int statusColor = switch (view.status()) {
            case ACTIVE -> 0x287a28;
            case DISABLED, WAREHOUSE_UNAVAILABLE -> 0xa06a00;
            default -> 0x9a2828;
        };
        graphics.drawString(getFont(), status, x + 8, y + 83, statusColor, false);
        graphics.drawString(getFont(), Component.translatable(
                        "gui.frostedheart.warehouse_interface.transport.current_rate",
                        format(view.effectiveRateItemsPerSecond())),
                x + 8, y + 93, view.isRateLimited() ? 0x9a2828 : 0x404040, false);
        graphics.drawString(getFont(), Component.translatable(
                        "gui.frostedheart.warehouse_interface.transport.reserved",
                        format(view.reservedCapacity())),
                x + 8, y + 103, 0x404040, false);
        graphics.drawString(getFont(), Component.translatable(
                        "gui.frostedheart.warehouse_interface.transport.town_capacity",
                        format(view.townRemainingCapacity()),
                        format(view.townTotalCapacity())),
                x + 8, y + 113, 0x404040, false);
    }

    private Component statusComponent(WarehouseInterfaceTransportView view) {
        if (view.decision() == com.teammoeg.frostedheart.content.town.transport.TransportReservationDecision.INVALID_REQUEST) {
            return Component.translatable("gui.frostedheart.warehouse_interface.transport.invalid_request");
        }
        if (view.decision() == com.teammoeg.frostedheart.content.town.transport.TransportReservationDecision.INSUFFICIENT_CAPACITY) {
            return Component.translatable("gui.frostedheart.warehouse_interface.transport.rate_increase_rejected");
        }
        return switch (view.status()) {
            case ACTIVE -> Component.translatable("gui.frostedheart.warehouse_interface.transport.active");
            case THROTTLED -> Component.translatable(
                    "gui.frostedheart.warehouse_interface.transport.throttled");
            case DISABLED -> Component.translatable("gui.frostedheart.warehouse_interface.transport.disabled");
            case WAREHOUSE_UNAVAILABLE -> Component.translatable(
                    "gui.frostedheart.warehouse_interface.status.unavailable");
            case UNBOUND -> Component.translatable("gui.frostedheart.warehouse_interface.status.unbound");
        };
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    static String rateInputText(WarehouseInterfaceTransportView view) {
        return Integer.toString(view.rateItemsPerSecond());
    }

    static int rateScrollIncrement(boolean shiftDown, boolean ctrlDown) {
        if (shiftDown && ctrlDown) {
            return 64;
        }
        if (ctrlDown) {
            return 16;
        }
        return shiftDown ? 8 : 1;
    }

    static int adjustRateForScroll(
            int currentRate,
            int scrollSteps,
            boolean shiftDown,
            boolean ctrlDown,
            int maximumRate
    ) {
        long adjusted = (long) currentRate
                + (long) scrollSteps * rateScrollIncrement(shiftDown, ctrlDown);
        return (int) Math.max(0L, Math.min(maximumRate, adjusted));
    }

    static boolean exceedsMaximumRate(String text, int maximumRate) {
        try {
            return Integer.parseInt(text) > maximumRate;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static int rateForScroll(String text, int acceptedRate, int maximumRate) {
        try {
            int parsed = Integer.parseInt(text);
            if (parsed >= 0 && parsed <= maximumRate) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        return Math.max(0, Math.min(maximumRate, acceptedRate));
    }

    private void submitRateInput() {
        if (!rateInput.isTextValid()) {
            return;
        }
        try {
            rateInput.setFocused(false);
            menu.setTransportRate(Integer.parseInt(rateInput.getText()));
        } catch (NumberFormatException ignored) {
        }
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
            setPosAndSize(114, 20, 56, 14);
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

    private final class RateInput extends TextBox {
        private final ScrollTracker scrollTracker = new ScrollTracker();
        private int maximumRate;

        RateInput(WarehouseInterfaceScreen parent) {
            super(parent);
        }

        void setRateRange(int maximumRate, int acceptedRate) {
            int requiredLength = Math.max(
                    Integer.toString(maximumRate).length(),
                    Integer.toString(acceptedRate).length());
            setMaxLength(requiredLength);
            if (this.maximumRate == maximumRate) {
                return;
            }
            this.maximumRate = maximumRate;
            setFilter(Verifiers.intRange(0, maximumRate));
            setText(getText(), false);
        }

        @Override
        public void insertText(String text) {
            String previous = getText();
            super.insertText(text);
            if (exceedsMaximumRate(getText(), maximumRate)) {
                setText(previous, false);
            }
        }

        @Override
        public boolean onMouseScrolled(double scroll) {
            if (!isMouseOver()) {
                return super.onMouseScrolled(scroll);
            }
            scrollTracker.addScroll(scroll);
            int scrollSteps = scrollTracker.getScroll();
            if (scrollSteps != 0) {
                int currentRate = rateForScroll(
                        getText(), menu.getTransportView().rateItemsPerSecond(), maximumRate);
                int adjustedRate = adjustRateForScroll(
                        currentRate, scrollSteps,
                        CInputHelper.isShiftKeyDown(), CInputHelper.isCtrlKeyDown(), maximumRate);
                setText(Integer.toString(adjustedRate), false);
                setFocused(false);
                menu.setTransportRate(adjustedRate);
            }
            return true;
        }

        @Override
        public void onEnterPressed() {
            submitRateInput();
            setFocused(false);
        }
    }

    private final class RateButton extends UIElement {
        RateButton(UIElement parent, int x, int y, int width, int height) {
            super(parent);
            setPosAndSize(x, y, width, height);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, int w, int h, RenderingHint hint) {
            graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xff373737);
            graphics.fill(x, y, x + w, y + h, isMouseOver() ? 0xffa0a0a0 : 0xff8b8b8b);
            Component label = Component.translatable("gui.frostedheart.warehouse_interface.apply");
            Font font = getFont();
            graphics.drawString(font, label, x + (w - font.width(label)) / 2,
                    y + (h - 8) / 2 + 1, 0xffffff, true);
        }

        @Override
        public boolean onMousePressed(MouseButton button) {
            if (!isMouseOver() || button != MouseButton.LEFT) {
                return false;
            }
            submitRateInput();
            return true;
        }
    }
}
