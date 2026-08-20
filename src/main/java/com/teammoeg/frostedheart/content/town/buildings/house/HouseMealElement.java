/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.house;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** Read-only item grid for the latest actual meal consumed by one house. */
final class HouseMealElement extends UIElement {
    private static final int PANEL_WIDTH = 160;
    private static final int PANEL_HEIGHT = 130;
    private static final int GRID_LEFT = 6;
    private static final int GRID_TOP = 43;
    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 8;
    private static final int VISIBLE_ROWS = 4;
    private static final int SCROLLBAR_LEFT = 152;
    private static final int SCROLLBAR_WIDTH = 5;

    private final HouseMenu menu;
    private int scrollRow;

    HouseMealElement(UIElement parent, int x, int y, HouseMenu menu) {
        super(parent);
        this.menu = menu;
        setPos(x, y);
        setSize(PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            RenderingHint hint
    ) {
        Font font = Minecraft.getInstance().font;
        drawPanel(graphics, x, y, width, height);
        graphics.drawString(
                font,
                Component.translatable("gui.frostedheart.house.daily_meal")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                x + 5, y + 5, 0xFFFFFFFF, true);

        HouseBuilding house = menu.getHouse().orElse(null);
        if (house == null) {
            drawStatus(graphics, font, x, y, "gui.frostedheart.house.unavailable");
            return;
        }
        HouseBuilding.DailyReport report = house.getDailyReport();
        HouseBuilding.DailyMeal meal = report.meal();
        if (!meal.hasData()) {
            drawStatus(graphics, font, x, y, "gui.frostedheart.house.no_meal_report");
            return;
        }

        graphics.drawString(
                font,
                Component.translatable(
                        "gui.frostedheart.house.meal_settlement_day", meal.settlementDay()),
                x + 5, y + 18, 0xFFAAAAAA, false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.frostedheart.house.meal_food_units",
                        decimal(report.foodConsumed()), decimal(report.foodRequired())),
                x + 5, y + 30, 0xFFFFFFFF, false);

        List<HouseBuilding.MealEntry> entries = meal.entries();
        if (entries.isEmpty()) {
            drawStatus(graphics, font, x, y, "gui.frostedheart.house.no_meal_served");
            scrollRow = 0;
            return;
        }

        int totalRows = totalRows(entries);
        scrollRow = Mth.clamp(scrollRow, 0, Math.max(0, totalRows - VISIBLE_ROWS));
        renderGrid(graphics, font, x, y, entries);
        renderScrollbar(graphics, x, y, totalRows);
    }

    private void renderGrid(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            List<HouseBuilding.MealEntry> entries
    ) {
        int startIndex = scrollRow * COLUMNS;
        for (int slot = 0; slot < COLUMNS * VISIBLE_ROWS; slot++) {
            int column = slot % COLUMNS;
            int row = slot / COLUMNS;
            int slotX = x + GRID_LEFT + column * SLOT_SIZE;
            int slotY = y + GRID_TOP + row * SLOT_SIZE;
            drawSlot(graphics, slotX, slotY);

            int entryIndex = startIndex + slot;
            if (entryIndex >= entries.size()) continue;
            HouseBuilding.MealEntry entry = entries.get(entryIndex);
            ItemStack stack = entry.item().toItemStack();
            graphics.renderItem(stack, slotX + 1, slotY + 1);
            graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1, null);

            String amount = roundedAmount(entry.amount());
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.drawString(
                    font, amount, slotX + 17 - font.width(amount), slotY + 9,
                    0xFFFFFFFF, true);
            graphics.pose().popPose();

            if (isHoveredSlot(column, row)) {
                graphics.fill(
                        slotX + 1, slotY + 1, slotX + 17, slotY + 17,
                        0x80FFFFFF);
            }
        }
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        HouseBuilding.DailyMeal meal = currentMeal();
        if (!isMouseOver() || !meal.hasData() || meal.entries().isEmpty()) return;
        int column = ((int) getMouseX() - GRID_LEFT) / SLOT_SIZE;
        int row = ((int) getMouseY() - GRID_TOP) / SLOT_SIZE;
        if (getMouseX() < GRID_LEFT || getMouseY() < GRID_TOP
                || column < 0 || column >= COLUMNS || row < 0 || row >= VISIBLE_ROWS) {
            return;
        }
        int index = (scrollRow + row) * COLUMNS + column;
        if (index < 0 || index >= meal.entries().size()) return;
        HouseBuilding.MealEntry entry = meal.entries().get(index);
        entry.item().toItemStack().getTooltipLines(
                Minecraft.getInstance().player, TooltipFlag.NORMAL).forEach(tooltip);
        tooltip.accept(Component.translatable(
                "gui.frostedheart.house.meal_item_amount", decimal(entry.amount()))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        int maxScroll = Math.max(0, totalRows(currentMeal().entries()) - VISIBLE_ROWS);
        if (maxScroll == 0) return false;
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scroll), 0, maxScroll);
        return true;
    }

    private HouseBuilding.DailyMeal currentMeal() {
        return menu.getHouse().map(HouseBuilding::getDailyReport)
                .map(HouseBuilding.DailyReport::meal)
                .orElse(HouseBuilding.DailyMeal.EMPTY);
    }

    private boolean isHoveredSlot(int column, int row) {
        double localX = getMouseX() - GRID_LEFT;
        double localY = getMouseY() - GRID_TOP;
        return isMouseOver()
                && localX >= column * SLOT_SIZE
                && localX < (column + 1) * SLOT_SIZE
                && localY >= row * SLOT_SIZE
                && localY < (row + 1) * SLOT_SIZE;
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int totalRows) {
        if (totalRows <= VISIBLE_ROWS) return;
        int trackY = y + GRID_TOP;
        int trackHeight = VISIBLE_ROWS * SLOT_SIZE;
        int thumbHeight = Math.max(10, trackHeight * VISIBLE_ROWS / totalRows);
        int maxScroll = totalRows - VISIBLE_ROWS;
        int thumbY = trackY + (trackHeight - thumbHeight) * scrollRow / maxScroll;
        graphics.fill(
                x + SCROLLBAR_LEFT, trackY,
                x + SCROLLBAR_LEFT + SCROLLBAR_WIDTH, trackY + trackHeight,
                0xFF202020);
        graphics.fill(
                x + SCROLLBAR_LEFT + 1, thumbY,
                x + SCROLLBAR_LEFT + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                0xFF909090);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, 0xFF373737);
    }

    private static void drawStatus(
            GuiGraphics graphics, Font font, int x, int y, String key
    ) {
        graphics.drawCenteredString(
                font, Component.translatable(key),
                x + PANEL_WIDTH / 2, y + 65, 0xFFAAAAAA);
    }

    private static void drawPanel(
            GuiGraphics graphics, int x, int y, int width, int height
    ) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private static int totalRows(List<?> entries) {
        return (entries.size() + COLUMNS - 1) / COLUMNS;
    }

    private static String roundedAmount(double amount) {
        return Long.toString(Math.round(amount));
    }

    private static String decimal(double value) {
        String result = String.format(Locale.ROOT, "%.4f", value);
        while (result.endsWith("0")) result = result.substring(0, result.length() - 1);
        if (result.endsWith(".")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
