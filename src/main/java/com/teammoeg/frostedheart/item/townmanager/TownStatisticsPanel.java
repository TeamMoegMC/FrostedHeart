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

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * 数据统计页签的内容面板。将城镇每日快照绘制为三条折线图：
 * 人口（自动量程）、平均生命与平均精神（固定 0-100 量程）。
 * 每条折线标注最新值与相对前一日的变化；历史不足两天时显示收集提示。
 * <p>
 * Content panel of the statistics tab. Renders the town's daily snapshots as
 * three line charts: population (auto-scaled), average health and average
 * mental (fixed 0-100 scale). Each chart shows the latest value and the
 * day-over-day change; a hint is shown while fewer than two days of history
 * are available.
 */
public class TownStatisticsPanel extends UIElement {

    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int HEADER_HEIGHT = 14;
    private static final int CHART_LABEL_HEIGHT = 11;
    private static final int CHART_PLOT_HEIGHT = 42;
    private static final int CHART_SPACING = 8;
    private static final int CHART_TOTAL_HEIGHT = CHART_LABEL_HEIGHT + CHART_PLOT_HEIGHT;
    private static final int PLOT_MARGIN = 4;

    private static final int COLOR_POPULATION = 0xFF55FFFF;
    private static final int COLOR_HEALTH = 0xFFFF5555;
    private static final int COLOR_MENTAL = 0xFF55FF55;

    private final Supplier<TeamTownData> townDataSource;

    public TownStatisticsPanel(UIElement parent, int x, int y, Supplier<TeamTownData> townDataSource) {
        super(parent);
        this.townDataSource = townDataSource;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        Font font = Minecraft.getInstance().font;
        drawPanel(graphics, x, y, width, height);
        TeamTownData data = townDataSource.get();
        List<TownHistoryEntry> history = data == null ? List.of() : data.getHistory();

        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.statistics"),
                x + 5, y + 4, 0xFFFFAA00, true);
        if (!history.isEmpty()) {
            Component days = Component.translatable("gui.frostedheart.town_manager.last_n_days", history.size());
            graphics.drawString(font, days, x + width - 5 - font.width(days), y + 4, 0xFFAAAAAA, true);
        }

        if (history.size() < 2) {
            int centerY = y + HEADER_HEIGHT + (height - HEADER_HEIGHT) / 2;
            drawCentered(graphics, font,
                    Component.translatable("gui.frostedheart.town_manager.collecting"),
                    x + width / 2, centerY - 10, 0xFFAAAAAA);
            drawCentered(graphics, font,
                    Component.translatable("gui.frostedheart.town_manager.collecting_hint"),
                    x + width / 2, centerY + 2, 0xFF777777);
            return;
        }

        int chartX = x + PLOT_MARGIN;
        int chartW = width - PLOT_MARGIN * 2;
        int chartY = y + HEADER_HEIGHT;
        drawChart(graphics, font, chartX, chartY, chartW, history,
                e -> (double) e.population(), COLOR_POPULATION,
                "gui.frostedheart.town_manager.population", 0, -1);
        chartY += CHART_TOTAL_HEIGHT + CHART_SPACING;
        drawChart(graphics, font, chartX, chartY, chartW, history,
                TownHistoryEntry::avgHealth, COLOR_HEALTH,
                "gui.frostedheart.town_manager.avg_health", 0, 100);
        chartY += CHART_TOTAL_HEIGHT + CHART_SPACING;
        drawChart(graphics, font, chartX, chartY, chartW, history,
                TownHistoryEntry::avgMental, COLOR_MENTAL,
                "gui.frostedheart.town_manager.avg_mental", 0, 100);
    }

    /**
     * 绘制单条折线图：标题行（指标名、最新值、日变化）加下方绘图区。
     * <p>
     * Draws one line chart: a label row (metric name, latest value, daily
     * delta) above the plot area.
     *
     * @param fixedMax 固定量程上限，负数表示按数据自动量程 / fixed scale maximum, negative for auto-scaling
     */
    private void drawChart(GuiGraphics graphics, Font font, int x, int y, int w,
                           List<TownHistoryEntry> history,
                           ToDoubleFunction<TownHistoryEntry> metric, int color,
                           String labelKey, double fixedMin, double fixedMax) {
        double latest = metric.applyAsDouble(history.get(history.size() - 1));
        double previous = metric.applyAsDouble(history.get(history.size() - 2));
        double change = latest - previous;

        Component label = Component.translatable(labelKey)
                .append(Component.literal(": " + format(latest)))
                .append(deltaText(change));
        graphics.drawString(font, label, x, y, color, true);

        int plotY = y + CHART_LABEL_HEIGHT;
        int plotH = CHART_PLOT_HEIGHT;
        graphics.fill(x, plotY, x + w, plotY + plotH, 0xFF181818);
        graphics.fill(x, plotY, x + w, plotY + 1, 0xFF373737);
        graphics.fill(x, plotY, x + 1, plotY + plotH, 0xFF373737);
        graphics.fill(x, plotY + plotH - 1, x + w, plotY + plotH, 0xFF8B8B8B);
        graphics.fill(x + w - 1, plotY, x + w, plotY + plotH, 0xFF8B8B8B);

        double min = fixedMax >= 0 ? fixedMin : 0;
        double max = fixedMax >= 0 ? fixedMax : Math.max(1, history.stream()
                .mapToDouble(metric).max().orElse(1));

        // 中位参考线 / middle reference line
        int midY = plotY + plotH / 2;
        for (int i = x + 2; i < x + w - 2; i += 4) {
            graphics.fill(i, midY, i + 2, midY + 1, 0xFF333333);
        }

        // 量程标注 / scale labels
        graphics.drawString(font, format(max), x + 3, plotY + 2, 0xFF777777, false);
        String minText = format(min);
        graphics.drawString(font, minText, x + 3, plotY + plotH - 10, 0xFF777777, false);

        // 折线 / polyline
        int innerX = x + 2;
        int innerW = w - 4;
        int innerY = plotY + 2;
        int innerH = plotH - 4;
        int points = history.size();
        int prevPx = 0;
        int prevPy = 0;
        for (int i = 0; i < points; i++) {
            double value = metric.applyAsDouble(history.get(i));
            int px = points == 1 ? innerX + innerW / 2
                    : innerX + innerW * i / (points - 1);
            double ratio = (value - min) / (max - min);
            ratio = Math.max(0, Math.min(1, ratio));
            int py = innerY + innerH - 1 - (int) Math.round(ratio * (innerH - 1));
            if (i > 0) {
                drawLine(graphics, prevPx, prevPy, px, py, color);
            }
            prevPx = px;
            prevPy = py;
        }
        // 最新数据点 / latest data point marker
        graphics.fill(prevPx - 1, prevPy - 1, prevPx + 2, prevPy + 2, 0xFFFFFFFF);
    }

    /**
     * 以逐点填充的方式绘制连续线段，避免使用 GL 线图元。
     * <p>
     * Draws a continuous segment by filling point by point, avoiding GL line
     * primitives.
     */
    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        int prevX = x1;
        int prevY = y1;
        for (int i = 1; i <= steps; i++) {
            int cx = x1 + (x2 - x1) * i / steps;
            int cy = y1 + (y2 - y1) * i / steps;
            graphics.fill(Math.min(prevX, cx), Math.min(prevY, cy),
                    Math.max(prevX, cx) + 1, Math.max(prevY, cy) + 1, color);
            prevX = cx;
            prevY = cy;
        }
    }

    private static Component deltaText(double change) {
        String text = (change > 0 ? " (+" : " (") + format(change) + ")";
        int color = change > 0.05 ? 0xFF55FF55 : change < -0.05 ? 0xFFFF5555 : 0xFF777777;
        return Component.literal(text).withStyle(style -> style.withColor(color));
    }

    private static void drawCentered(GuiGraphics graphics, Font font, Component text,
                                     int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, true);
    }

    private static String format(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1e9) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }
}
