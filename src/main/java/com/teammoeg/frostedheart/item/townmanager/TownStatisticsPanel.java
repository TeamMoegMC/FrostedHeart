/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.TownHistoryEntry;
import com.teammoeg.frostedheart.content.town.observation.TownOperationalHistory;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/** Two player-facing history views. Missing optional samples produce line gaps. */
public class TownStatisticsPanel extends UIElement {
    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int HEADER_HEIGHT = 28;
    private static final int CHART_LABEL_HEIGHT = 11;
    private static final int CHART_PLOT_HEIGHT = 40;
    private static final int CHART_SPACING = 6;
    private static final int PLOT_MARGIN = 4;

    private final Supplier<TeamTownData> townDataSource;
    private View view = View.RESIDENTS;

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

        graphics.drawString(font, Component.translatable("gui.frostedheart.town_manager.statistics"),
                x + 5, y + 4, 0xFFFFAA00, true);
        if (!history.isEmpty()) {
            Component days = Component.translatable("gui.frostedheart.town_manager.last_n_days", history.size());
            graphics.drawString(font, days, x + width - 5 - font.width(days), y + 4, 0xFFAAAAAA, true);
        }
        int half = (width - 8) / 2;
        drawViewButton(graphics, font, x + 4, y + 14, half, View.RESIDENTS);
        drawViewButton(graphics, font, x + 4 + half, y + 14, half, View.SURVIVAL);

        if (history.size() < 2) {
            drawCentered(graphics, font, Component.translatable("gui.frostedheart.town_manager.collecting"),
                    x + width / 2, y + 96, 0xFFAAAAAA);
            drawCentered(graphics, font, Component.translatable("gui.frostedheart.town_manager.collecting_hint"),
                    x + width / 2, y + 108, 0xFF777777);
            return;
        }

        List<Chart> charts = view == View.RESIDENTS ? residentCharts() : survivalCharts();
        int chartY = y + HEADER_HEIGHT;
        for (Chart chart : charts) {
            drawChart(graphics, font, x + PLOT_MARGIN, chartY, width - PLOT_MARGIN * 2, history, chart);
            chartY += CHART_LABEL_HEIGHT + CHART_PLOT_HEIGHT + CHART_SPACING;
        }
    }

    private void drawViewButton(GuiGraphics graphics, Font font, int x, int y, int width, View candidate) {
        boolean selected = view == candidate;
        graphics.fill(x, y, x + width, y + 11, selected ? 0xFF35566F : 0xFF222222);
        Component label = Component.translatable(candidate.translationKey);
        graphics.drawString(font, label, x + (width - font.width(label)) / 2, y + 2,
                selected ? 0xFFFFFFFF : 0xFF888888, false);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT || getMouseY() < 14 || getMouseY() >= 25) {
            return false;
        }
        view = getMouseX() < WIDTH / 2 ? View.RESIDENTS : View.SURVIVAL;
        return true;
    }

    private void drawChart(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int width,
            List<TownHistoryEntry> history,
            Chart chart
    ) {
        Component title = Component.translatable(chart.titleKey());
        graphics.drawString(font, title, x, y, 0xFFFFFFFF, true);
        int legendX = x + font.width(title) + 4;
        TownHistoryEntry latest = history.get(history.size() - 1);
        for (Series series : chart.series()) {
            MetricPoint point = series.metric().apply(latest);
            String value = point.available() ? format(point.value()) : "-";
            Component text = Component.literal(value);
            if (legendX + font.width(text) >= x + width) break;
            graphics.drawString(font, text, legendX, y, series.color(), false);
            legendX += font.width(text) + 4;
        }

        int plotY = y + CHART_LABEL_HEIGHT;
        int plotHeight = CHART_PLOT_HEIGHT;
        graphics.fill(x, plotY, x + width, plotY + plotHeight, 0xFF181818);
        graphics.fill(x, plotY, x + width, plotY + 1, 0xFF373737);
        graphics.fill(x, plotY + plotHeight - 1, x + width, plotY + plotHeight, 0xFF8B8B8B);

        Scale scale = scale(history, chart);
        for (Reference reference : chart.references()) {
            if (reference.value() < scale.minimum() || reference.value() > scale.maximum()) continue;
            int referenceY = valueY(reference.value(), scale, plotY, plotHeight);
            for (int dash = x + 2; dash < x + width - 2; dash += 4) {
                graphics.fill(dash, referenceY, dash + 2, referenceY + 1, reference.color());
            }
            String label = format(reference.value());
            graphics.drawString(font, label, x + width - font.width(label) - 3,
                    Math.max(plotY + 1, Math.min(plotY + plotHeight - 9, referenceY - 4)),
                    reference.color(), false);
        }

        int innerX = x + 2;
        int innerWidth = width - 4;
        for (Series series : chart.series()) {
            boolean hasPrevious = false;
            int previousX = 0;
            int previousY = 0;
            for (int index = 0; index < history.size(); index++) {
                MetricPoint point = series.metric().apply(history.get(index));
                int pointX = innerX + innerWidth * index / Math.max(1, history.size() - 1);
                if (!point.available()) {
                    hasPrevious = false;
                    continue;
                }
                int pointY = valueY(point.value(), scale, plotY, plotHeight);
                if (hasPrevious) drawLine(graphics, previousX, previousY, pointX, pointY, series.color());
                previousX = pointX;
                previousY = pointY;
                hasPrevious = true;
            }
            MetricPoint last = series.metric().apply(latest);
            if (last.available() && hasPrevious) {
                graphics.fill(previousX - 1, previousY - 1, previousX + 2, previousY + 2, series.color());
            }
        }
    }

    private static Scale scale(List<TownHistoryEntry> history, Chart chart) {
        if (chart.fixedMaximum() > chart.fixedMinimum()) {
            return new Scale(chart.fixedMinimum(), chart.fixedMaximum());
        }
        double minimum = 0.0;
        double maximum = 1.0;
        for (TownHistoryEntry entry : history) {
            for (Series series : chart.series()) {
                MetricPoint point = series.metric().apply(entry);
                if (point.available()) maximum = Math.max(maximum, point.value());
            }
        }
        for (Reference reference : chart.references()) maximum = Math.max(maximum, reference.value());
        return new Scale(minimum, maximum * 1.05);
    }

    private static int valueY(double value, Scale scale, int plotY, int plotHeight) {
        double ratio = (value - scale.minimum()) / Math.max(1.0e-9, scale.maximum() - scale.minimum());
        ratio = Math.max(0.0, Math.min(1.0, ratio));
        return plotY + plotHeight - 3 - (int) Math.round(ratio * (plotHeight - 5));
    }

    private static List<Chart> residentCharts() {
        return List.of(
                new Chart("gui.frostedheart.town_manager.chart_population_capacity", List.of(
                        new Series(0xFF55FFFF, entry -> MetricPoint.of(entry.population())),
                        new Series(0xFFFFAA00, entry -> MetricPoint.of(entry.unableToWorkCount())),
                        new Series(0xFFFF5555, entry -> MetricPoint.of(entry.exitRiskCount()))),
                        List.of(), 0, -1),
                new Chart("gui.frostedheart.town_manager.chart_health", List.of(
                        new Series(0xFFFF5555, entry -> MetricPoint.of(entry.avgHealth())),
                        new Series(0xFFFFAA00, entry -> MetricPoint.of(entry.p10Health()))),
                        List.of(), 0, 100),
                new Chart("gui.frostedheart.town_manager.chart_mental", List.of(
                        new Series(0xFF55FF55, entry -> MetricPoint.of(entry.avgMental())),
                        new Series(0xFF55FFFF, entry -> MetricPoint.of(entry.p10Mental()))),
                        List.of(), 0, 100));
    }

    private static List<Chart> survivalCharts() {
        double warning = FHConfig.SERVER.TOWN.OBSERVATION.reserveWarningDays.get();
        double critical = FHConfig.SERVER.TOWN.OBSERVATION.reserveCriticalDays.get();
        List<Reference> reserveReferences = List.of(
                new Reference(warning, 0xFFFFAA00), new Reference(critical, 0xFFFF5555));
        return List.of(
                new Chart("gui.frostedheart.town_manager.food_reserve_days", List.of(
                        new Series(0xFF55FF55, entry -> metric(entry.operational().foodReserveDays()))),
                        reserveReferences, 0, -1),
                new Chart("gui.frostedheart.town_manager.fuel_reserve_days", List.of(
                        new Series(0xFFFFAA00, entry -> metric(entry.operational().fuelReserveDays()))),
                        reserveReferences, 0, -1),
                new Chart("gui.frostedheart.town_manager.chart_building_temperature", List.of(
                        new Series(0xFF55FFFF, entry -> metric(entry.operational().minimumBuildingTemperatureCelsius()))),
                        List.of(), -40, 40));
    }

    private static MetricPoint metric(TownOperationalHistory.Metric metric) {
        return new MetricPoint(metric.available(), metric.value());
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int index = 0; index <= steps; index++) {
            int x = x1 + (x2 - x1) * index / steps;
            int y = y1 + (y2 - y1) * index / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static void drawCentered(GuiGraphics graphics, Font font, Component text,
                                     int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, true);
    }

    private static String format(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1e9) return String.valueOf((long) value);
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
    }

    private enum View {
        RESIDENTS("gui.frostedheart.town_manager.statistics_residents"),
        SURVIVAL("gui.frostedheart.town_manager.statistics_survival");

        private final String translationKey;

        View(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private record MetricPoint(boolean available, double value) {
        static MetricPoint of(double value) {
            return new MetricPoint(true, value);
        }
    }

    private record Series(int color, Function<TownHistoryEntry, MetricPoint> metric) {
    }

    private record Reference(double value, int color) {
    }

    private record Chart(
            String titleKey,
            List<Series> series,
            List<Reference> references,
            double fixedMinimum,
            double fixedMaximum
    ) {
    }

    private record Scale(double minimum, double maximum) {
    }
}
