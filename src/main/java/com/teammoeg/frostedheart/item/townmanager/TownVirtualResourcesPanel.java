/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.item.townmanager;

import com.teammoeg.chorda.client.RenderingHint;
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSnapshot;
import com.teammoeg.frostedheart.content.town.transport.TownTransportSummary;
import com.teammoeg.frostedheart.content.town.transport.TownTransportState;
import com.teammoeg.frostedheart.content.town.transport.TransportAdmissionStatus;
import com.teammoeg.frostedheart.content.town.transport.TransportEndpointKind;
import com.teammoeg.frostedheart.content.town.transport.TransportReservation;
import com.teammoeg.frostedheart.content.town.transport.TransportReservationModel;
import net.minecraft.core.GlobalPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Selector and specialized detail pages for every {@link VirtualResourceType}. */
public final class TownVirtualResourcesPanel extends UILayer {
    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int LIST_WIDTH = 100;
    private static final int LIST_TOP = 22;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = (HEIGHT - LIST_TOP - 4) / ROW_HEIGHT;
    private static final int DETAIL_LEFT = LIST_WIDTH + 4;
    private static final int DETAIL_TOP = 24;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final Supplier<TeamTown> townSource;
    private final TownInfoPanel detailPanel;
    private final TransportDetailsState transportDetails = new TransportDetailsState();
    private VirtualResourceType selectedType;
    private int listScroll;

    public TownVirtualResourcesPanel(
            UIElement parent, int x, int y, Supplier<TeamTown> townSource
    ) {
        this(parent, x, y, townSource, firstType());
    }

    public TownVirtualResourcesPanel(
            UIElement parent, int x, int y, Supplier<TeamTown> townSource,
            @Nullable VirtualResourceType initialType
    ) {
        super(parent);
        this.townSource = townSource;
        this.selectedType = initialType;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
        setScissorEnabled(false);
        detailPanel = new TownInfoPanel(
                this,
                DETAIL_LEFT,
                DETAIL_TOP,
                WIDTH - DETAIL_LEFT,
                HEIGHT - DETAIL_TOP,
                this::detailRows,
                false);
    }

    @Override
    public void addUIElements() {
        add(detailPanel);
    }

    @Override
    public void alignWidgets() {
    }

    @Override
    public void drawBackground(
            GuiGraphics graphics, int x, int y, int width, int height,
            RenderingHint hint
    ) {
        Font font = Minecraft.getInstance().font;
        List<VirtualResourceType> types = resourceTypes();
        normalizeSelection(types);
        drawPanel(graphics, x, y, width, height);
        graphics.fill(x + LIST_WIDTH, y + 1, x + LIST_WIDTH + 1, y + height - 1,
                0xFF777777);
        graphics.drawString(
                font,
                TownTextLayout.ellipsize(
                        font,
                        Component.translatable(
                                "gui.frostedheart.town_manager.virtual_resources").getString(),
                        LIST_WIDTH - 8),
                x + 4,
                y + 7,
                0xFFFFAA00,
                true);
        renderList(graphics, font, x, y, types);
        renderDetailHeader(graphics, font, x, y);
    }

    private void renderList(
            GuiGraphics graphics, Font font, int x, int y,
            List<VirtualResourceType> types
    ) {
        int maxScroll = Math.max(0, types.size() - VISIBLE_ROWS);
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = listScroll + row;
            if (index >= types.size()) break;
            VirtualResourceType type = types.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            boolean hovered = getMouseX() >= 2 && getMouseX() < LIST_WIDTH - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT;
            if (type == selectedType) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1,
                        0xA0446688);
            } else if (hovered) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1,
                        0x60444444);
            }
            CIcons.getIcon(icon(type)).draw(graphics, x + 4, rowY + 2, 16, 16);
            graphics.drawString(
                    font,
                    TownTextLayout.ellipsize(font, resourceName(type).getString(), LIST_WIDTH - 27),
                    x + 22,
                    rowY + 6,
                    0xFFFFFFFF,
                    true);
        }
        if (maxScroll > 0) {
            int trackY = y + LIST_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int thumbHeight = Math.max(
                    MIN_THUMB_HEIGHT, trackHeight * VISIBLE_ROWS / types.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * listScroll / maxScroll;
            graphics.fill(x + LIST_WIDTH - 3, trackY, x + LIST_WIDTH - 1,
                    trackY + trackHeight, 0xFF222222);
            graphics.fill(x + LIST_WIDTH - 3, thumbY, x + LIST_WIDTH - 1,
                    thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderDetailHeader(GuiGraphics graphics, Font font, int x, int y) {
        VirtualResourceType type = selectedType;
        if (type == null) return;
        CIcons.getIcon(icon(type)).draw(graphics, x + DETAIL_LEFT + 2, y + 3, 16, 16);
        graphics.drawString(
                font,
                TownTextLayout.ellipsize(
                        font, resourceName(type).getString(), WIDTH - DETAIL_LEFT - 24),
                x + DETAIL_LEFT + 21,
                y + 7,
                0xFFFFAA00,
                true);
    }

    private List<TownInfoPanel.Row> detailRows() {
        TeamTown town = townSource.get();
        if (town == null || selectedType == null) {
            return List.of(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town_manager.no_town"),
                    0xFFFF5555));
        }
        return switch (selectedType) {
            case MAX_CAPACITY -> warehouseRows(town.getResourceHolder());
            case TRANSPORT_CAPACITY -> transportRows(town);
            default -> genericRows(town.getResourceHolder(), selectedType);
        };
    }

    private static List<TownInfoPanel.Row> genericRows(
            TeamTownResourceHolder resources, VirtualResourceType type
    ) {
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(amount("current_amount", resources.get(type), 0xFFFFFFFF));
        rows.add(property(
                "daily_service",
                Component.translatable(type.isService
                        ? "gui.frostedheart.common.yes"
                        : "gui.frostedheart.common.no")));
        rows.add(property(
                "uses_warehouse_capacity",
                Component.translatable(type.needCapacity
                        ? "gui.frostedheart.common.yes"
                        : "gui.frostedheart.common.no")));
        if (type.getMaxLevel() > 0) {
            for (int level = 0; level <= type.getMaxLevel(); level++) {
                rows.add(TownInfoPanel.Row.text(Component.translatable(
                        "gui.frostedheart.town_manager.virtual_resource.level_amount",
                        level,
                        formatNumber(resources.get(type.generateAttribute(level))))));
            }
        }
        return rows;
    }

    private static List<TownInfoPanel.Row> warehouseRows(
            TeamTownResourceHolder resources
    ) {
        TownVirtualResourceMetrics.CapacityBreakdown capacity =
                TownVirtualResourceMetrics.capacity(
                        resources.get(VirtualResourceType.MAX_CAPACITY),
                        resources.getOccupiedCapacity());
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        if (capacity.overcommitted()) {
            rows.add(status("overcommitted", 0xFFFF5555));
        } else if (capacity.total() <= TeamTownResourceHolder.DELTA) {
            rows.add(status("no_capacity", 0xFFFFAA00));
        } else if (capacity.available() <= capacity.total() * 0.1) {
            rows.add(status("nearly_full", 0xFFFFAA00));
        } else {
            rows.add(status("available", 0xFF55FF55));
        }
        rows.add(amount("warehouse_total", capacity.total(), 0xFFFFFFFF));
        rows.add(amount(
                "warehouse_occupied",
                capacity.used(),
                capacity.overcommitted() ? 0xFFFF5555 : 0xFFFFFFFF));
        rows.add(amount(
                "warehouse_available",
                capacity.available(),
                capacity.available() <= capacity.total() * 0.1
                        ? 0xFFFFAA00 : 0xFF55FF55));
        if (capacity.overcommitted()) {
            rows.add(amount("capacity_shortfall", capacity.shortfall(), 0xFFFF5555));
        }
        rows.add(percent("warehouse_utilization", capacity.utilizationFraction(), 0xFFFFFFFF));
        return rows;
    }

    private List<TownInfoPanel.Row> transportRows(TeamTown town) {
        TeamTownResourceHolder resources = town.getResourceHolder();
        TownTransportSnapshot snapshot = TownTransportSnapshot.from(
                resources.get(VirtualResourceType.TRANSPORT_CAPACITY),
                town.getTransportState());
        return transportRows(snapshot, transportDetails);
    }

    static List<TownInfoPanel.Row> transportRows(
            TownTransportSnapshot snapshot,
            TransportDetailsState details
    ) {
        TownTransportSummary summary = snapshot.summary();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        boolean shortage = TransportReservationModel.meaningfullyGreater(
                summary.reservedCapacity(), summary.totalCapacity());
        if (shortage) {
            rows.add(status("shortage", 0xFFFF5555));
        } else if (summary.totalCapacity() <= TeamTownResourceHolder.DELTA) {
            rows.add(status("no_capacity", 0xFFFFAA00));
        } else {
            rows.add(status("available", 0xFF55FF55));
        }
        rows.add(section("transport_realtime", 0xFFFFAA00));
        rows.add(TownInfoPanel.Row.text(Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.transport_effective_warehouses",
                snapshot.effectiveWarehouseCount())));
        rows.add(amount("transport_total", summary.totalCapacity(), 0xFFFFFFFF));
        rows.add(amount(
                "transport_reserved",
                summary.reservedCapacity(),
                shortage ? 0xFFFF5555 : 0xFFFFFFFF));
        rows.add(amount(
                "transport_available",
                summary.remainingRegistrableCapacity(),
                shortage ? 0xFFFF5555 : 0xFF55FF55));
        if (shortage) {
            rows.add(amount("capacity_shortfall", summary.shortfall(), 0xFFFF5555));
        }
        rows.add(percent(
                "transport_effective_rate",
                summary.effectiveRateScale(),
                shortage ? 0xFFFFAA00 : 0xFF55FF55));
        rows.add(TownInfoPanel.Row.empty());
        rows.add(section("transport_daily_report", 0xFFFFAA00));
        TownTransportState.DailyReport report = snapshot.dailyReport();
        if (!report.hasData()) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.town_manager.daily_data_unavailable"),
                    0xFF777777));
        } else {
            TownVirtualResourceMetrics.CapacityBreakdown daily =
                    TownVirtualResourceMetrics.capacity(report.totalCapacity(), report.reservedCapacity());
            rows.add(amount("transport_daily_total", daily.total(), 0xFFAAAAAA));
            rows.add(amount("transport_daily_reserved", daily.used(), 0xFFAAAAAA));
            rows.add(percent("transport_daily_effective_rate", daily.effectiveRateScale(),
                    daily.overcommitted() ? 0xFFFFAA00 : 0xFFAAAAAA));
        }

        rows.add(TownInfoPanel.Row.empty());
        Component detailsControl = details.expanded()
                ? Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.transport_details_collapse")
                : Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.transport_details_expand",
                snapshot.reservations().size());
        rows.add(TownInfoPanel.Row.clickable(
                detailsControl,
                0xFF55FFFF,
                details::toggle));
        if (!details.expanded()) {
            return rows;
        }

        for (TownTransportState.ReservationEntry entry : snapshot.reservations()) {
            TransportReservation reservation = entry.reservation();
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.virtual_resource.transport_endpoint",
                    endpointKind(reservation.endpointKind()),
                    formatPosition(entry.endpointId().endpointPos())), 0xFFFFFFFF));
            rows.add(TownInfoPanel.Row.text(Component.translatable(
                    "gui.frostedheart.town_manager.virtual_resource.transport_endpoint_rate",
                    reservation.rateItemsPerSecond(),
                    formatNumber(reservation.rateItemsPerSecond()
                            * summary.effectiveRateScale()))));
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.virtual_resource.transport_endpoint_distance",
                    warehouseDistance(reservation),
                    distanceFactor(reservation, snapshot.warehouseDistanceCostPerBlock())),
                    0xFFAAAAAA));
            rows.add(TownInfoPanel.Row.colored(Component.translatable(
                    "gui.frostedheart.town_manager.virtual_resource.transport_endpoint_metrics",
                    formatNumber(reservation.reservedTransportCapacity()),
                    admissionStatus(reservation.admissionStatus(), shortage)),
                    0xFFAAAAAA));
        }
        return rows;
    }

    static final class TransportDetailsState {
        private boolean expanded;

        boolean expanded() {
            return expanded;
        }

        void toggle() {
            expanded = !expanded;
        }
    }

    private static TownInfoPanel.Row section(String key, int color) {
        return TownInfoPanel.Row.colored(Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource." + key), color);
    }

    private static Component endpointKind(TransportEndpointKind kind) {
        return Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.transport_endpoint_kind."
                        + kind.name().toLowerCase(Locale.ROOT));
    }

    private static Component admissionStatus(TransportAdmissionStatus status, boolean shortage) {
        String state = status == TransportAdmissionStatus.ACTIVE && shortage
                ? "throttled"
                : status.name().toLowerCase(Locale.ROOT);
        return Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.transport_admission."
                        + state);
    }

    private static String warehouseDistance(TransportReservation reservation) {
        if (reservation.admissionStatus() == TransportAdmissionStatus.UNAVAILABLE) {
            return "-";
        }
        return formatNumber(reservation.scaleMetric());
    }

    private static String distanceFactor(
            TransportReservation reservation,
            double warehouseDistanceCostPerBlock
    ) {
        if (reservation.admissionStatus() == TransportAdmissionStatus.UNAVAILABLE) {
            return "-";
        }
        double factor = 1.0 + warehouseDistanceCostPerBlock * reservation.scaleMetric();
        return TransportReservationModel.isFiniteNonNegative(factor)
                ? formatNumber(factor) : "-";
    }

    private static String formatPosition(GlobalPos globalPos) {
        return globalPos.dimension().location() + " "
                + globalPos.pos().getX() + ", "
                + globalPos.pos().getY() + ", "
                + globalPos.pos().getZ();
    }

    private static TownInfoPanel.Row status(String valueKey, int color) {
        return TownInfoPanel.Row.colored(
                Component.translatable(
                        "gui.frostedheart.town_manager.virtual_resource.status",
                        Component.translatable(
                                "gui.frostedheart.town_manager.virtual_resource.status."
                                        + valueKey)),
                color);
    }

    private static TownInfoPanel.Row amount(String key, double value, int color) {
        return TownInfoPanel.Row.colored(
                Component.translatable(
                        "gui.frostedheart.town_manager.virtual_resource." + key,
                        formatNumber(value)),
                color);
    }

    private static TownInfoPanel.Row property(String key, Component value) {
        return TownInfoPanel.Row.text(Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource." + key,
                value));
    }

    private static TownInfoPanel.Row percent(String key, double fraction, int color) {
        return TownInfoPanel.Row.colored(
                Component.translatable(
                        "gui.frostedheart.town_manager.virtual_resource." + key,
                        String.format(Locale.ROOT, "%.0f%%", fraction * 100.0)),
                color);
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (super.onMousePressed(button)) return true;
        if (getMouseX() >= LIST_WIDTH || getMouseY() < LIST_TOP) return false;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= VISIBLE_ROWS) return false;
        List<VirtualResourceType> types = resourceTypes();
        int index = listScroll + row;
        if (index >= types.size()) return false;
        selectedType = types.get(index);
        detailPanel.resetScroll();
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        if (getMouseX() >= LIST_WIDTH) return super.onMouseScrolled(scroll);
        int maxScroll = Math.max(0, resourceTypes().size() - VISIBLE_ROWS);
        if (maxScroll == 0) return false;
        listScroll = Mth.clamp(
                listScroll - (int) Math.signum(scroll), 0, maxScroll);
        return true;
    }

    @Override
    public void getTooltip(TooltipBuilder tooltip) {
        super.getTooltip(tooltip);
        if (!isMouseOver() || getMouseX() >= LIST_WIDTH || getMouseY() < LIST_TOP) return;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= VISIBLE_ROWS) return;
        List<VirtualResourceType> types = resourceTypes();
        int index = listScroll + row;
        if (index < types.size()) tooltip.accept(resourceName(types.get(index)));
    }

    private void normalizeSelection(List<VirtualResourceType> types) {
        if (types.isEmpty()) {
            selectedType = null;
            listScroll = 0;
            return;
        }
        if (selectedType == null || !types.contains(selectedType)) {
            selectedType = types.get(0);
            detailPanel.resetScroll();
        }
    }

    private static List<VirtualResourceType> resourceTypes() {
        return Arrays.asList(VirtualResourceType.values());
    }

    @Nullable
    private static VirtualResourceType firstType() {
        VirtualResourceType[] types = VirtualResourceType.values();
        return types.length == 0 ? null : types[0];
    }

    private static Component resourceName(VirtualResourceType type) {
        return Component.translatableWithFallback(
                "gui.frostedheart.town_manager.virtual_resource." + type.getKey(),
                humanName(type));
    }

    private static String humanName(VirtualResourceType type) {
        return Arrays.stream(type.getKey().split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static Item icon(VirtualResourceType type) {
        return switch (type) {
            case MAX_CAPACITY -> Items.CHEST;
            case TRANSPORT_CAPACITY -> Items.MINECART;
            default -> Items.PAPER;
        };
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1.0e9) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
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
}
