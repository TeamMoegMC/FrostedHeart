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
import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.resource.TeamTownResourceHolder;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardIcons;
import com.teammoeg.frostedheart.content.town.client.board.NoticeBoardTheme;
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

/**
 * 城镇服务：左边一列纸签选服务，右边一张纸写它的详情（仓库容量、运力、研究人手）。放在布告板上，尺寸随板子。
 * <p>
 * The town's services: paper tags on the left pick a service, a paper sheet on the right details it (storage,
 * transport capacity, research hands). Lives on the notice board and takes its size.
 */
public final class TownVirtualResourcesPanel extends UILayer {
    private static final int LIST_WIDTH = 112;
    private static final int LIST_TOP = 22;
    private static final int ROW_HEIGHT = 22;
    private static final int DETAIL_LEFT = LIST_WIDTH + 10;
    private static final int DETAIL_TOP = 22;
    /** The summary note above the ledger: a big figure, a paper bar and a one-line verdict. */
    private static final int SUMMARY_H = 50, SUMMARY_GAP = 8;
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
        this(parent, x, y, TownManagerScreen.CONTENT_WIDTH, TownManagerScreen.CONTENT_HEIGHT, townSource, initialType);
    }

    public TownVirtualResourcesPanel(
            UIElement parent, int x, int y, int width, int height, Supplier<TeamTown> townSource,
            @Nullable VirtualResourceType initialType
    ) {
        super(parent);
        this.townSource = townSource;
        this.selectedType = initialType;
        setPos(x, y);
        setSize(width, height);
        setScissorEnabled(false);
        detailPanel = new TownInfoPanel(
                this,
                DETAIL_LEFT,
                DETAIL_TOP + SUMMARY_H + SUMMARY_GAP,
                width - DETAIL_LEFT,
                height - DETAIL_TOP - SUMMARY_H - SUMMARY_GAP,
                this::detailRows,
                true).paper();
    }

    private int visibleRows() {
        return Math.max(1, (getHeight() - LIST_TOP - 4) / ROW_HEIGHT);
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
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                TownTextLayout.ellipsize(
                        font,
                        Component.translatable(
                                "gui.frostedheart.town_manager.virtual_resources").getString(),
                        LIST_WIDTH - 8),
                x + 4,
                y + 6,
                NoticeBoardTheme.ON_WOOD);
        renderList(graphics, font, x, y, types);
        renderDetailHeader(graphics, font, x, y);
        renderSummary(graphics, x + DETAIL_LEFT, y + DETAIL_TOP, getWidth() - DETAIL_LEFT);
    }

    /**
     * 一张钉在明细上面的便签：这项服务现在怎样，一个大数、一道纸槽和一句话说清楚；下面的明细是账本。
     * A note pinned above the ledger: how this service stands now, as a big figure, a paper bar and one line; the rows
     * below are the ledger.
     */
    private record Summary(Component title, Component figure, float fill, int fillColor, Component verdict, int verdictColor) {
    }

    private void renderSummary(GuiGraphics g, int x, int y, int w) {
        TeamTown town = townSource.get();
        Summary summary = town == null || selectedType == null ? null : summary(town, selectedType);
        if (summary == null) return;
        int h = SUMMARY_H;
        g.fill(x + 1, y + 2, x + w + 1, y + h + 1, 0x30000000);
        NoticeBoardIcons.PAPER.draw(g, x, y, w, h);
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.pin(g, NoticeBoardIcons.PIN_BRASS, x + w / 2, y + 1);
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.text(g, summary.title(), x + 8, y + 6, w - 16, NoticeBoardTheme.MUTED);
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.big(g, summary.figure(), x + 8, y + 16, NoticeBoardTheme.INK);
        int barX = x + 8 + com.teammoeg.frostedheart.content.town.client.board.BoardDraw.bigWidth(summary.figure()) + 10, barW = x + w - 8 - barX;
        if (summary.fill() >= 0 && barW >= 30)
            NoticeBoardTheme.INSTANCE.drawProgressBar(g, barX, y + 23, barW, 6, Math.min(1f, summary.fill()), summary.fillColor());
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.text(g, summary.verdict(), x + 8, y + h - 12, w - 16, summary.verdictColor());
    }

    private static Summary summary(TeamTown town, VirtualResourceType type) {
        return switch (type) {
            case MAX_CAPACITY -> {
                TownVirtualResourceMetrics.CapacityBreakdown capacity = TownVirtualResourceMetrics.capacity(
                        town.getResourceHolder().get(VirtualResourceType.MAX_CAPACITY), town.getResourceHolder().getOccupiedCapacity());
                long used = (long) Math.ceil(capacity.used() - 1e-9), total = (long) Math.floor(capacity.total() + 1e-9);
                float fill = capacity.total() <= TeamTownResourceHolder.DELTA ? -1 : (float) (capacity.used() / capacity.total());
                if (capacity.overcommitted())
                    yield new Summary(s("warehouse.title"), Component.literal(count(used) + " / " + count(total)), fill, NoticeBoardTheme.RED,
                            s("warehouse.over", count((long) Math.ceil(capacity.shortfall() - 1e-9))), NoticeBoardTheme.RED);
                if (total <= 0)
                    yield new Summary(s("warehouse.title"), Component.literal("0"), -1, 0, s("warehouse.none"), NoticeBoardTheme.AMBER);
                // What is left is what the two shown figures leave, so the three numbers always add up.
                long left = Math.max(0, total - used);
                boolean nearlyFull = capacity.available() <= capacity.total() * 0.1;
                yield new Summary(s("warehouse.title"), Component.literal(count(used) + " / " + count(total)), fill,
                        nearlyFull ? NoticeBoardTheme.AMBER : NoticeBoardTheme.GREEN_FILL,
                        s(nearlyFull ? "warehouse.nearly_full" : "warehouse.room", count(left)), nearlyFull ? NoticeBoardTheme.AMBER : NoticeBoardTheme.GREEN);
            }
            case TRANSPORT_CAPACITY -> {
                TownTransportSummary transport = town.getTransportSnapshot().summary();
                long reserved = (long) Math.ceil(transport.reservedCapacity() - 1e-9), total = (long) Math.floor(transport.totalCapacity() + 1e-9);
                boolean shortage = TransportReservationModel.meaningfullyGreater(transport.reservedCapacity(), transport.totalCapacity());
                if (total <= 0 && !shortage)
                    yield new Summary(s("transport.title"), Component.literal("0"), -1, 0, s("transport.none"), NoticeBoardTheme.AMBER);
                float fill = transport.totalCapacity() <= TeamTownResourceHolder.DELTA ? 1 : (float) (transport.reservedCapacity() / transport.totalCapacity());
                if (shortage)
                    yield new Summary(s("transport.title"), Component.literal(count(reserved) + " / " + count(total)), fill, NoticeBoardTheme.RED,
                            s("transport.short", count((long) Math.ceil(transport.shortfall() - 1e-9))), NoticeBoardTheme.RED);
                yield new Summary(s("transport.title"), Component.literal(count(reserved) + " / " + count(total)), fill, NoticeBoardTheme.BLUE,
                        s(reserved == 0 ? "transport.idle" : "transport.room", count((long) Math.floor(transport.remainingRegistrableCapacity() + 1e-9))),
                        NoticeBoardTheme.MUTED);
            }
            case RESEARCH_LABOR -> {
                double total = 0;
                for (int level = 0; level <= VirtualResourceType.RESEARCH_LABOR.getMaxLevel(); level++)
                    total += town.getResourceHolder().get(VirtualResourceType.RESEARCH_LABOR.generateAttribute(level));
                long hands = (long) Math.floor(total + 1e-9);
                yield new Summary(s("research.title"), Component.literal(count(hands)), -1, 0,
                        total <= TeamTownResourceHolder.DELTA ? s("research.none") : hands < 1 ? s("research.under_one") : s("research.some"),
                        total <= TeamTownResourceHolder.DELTA ? NoticeBoardTheme.AMBER : NoticeBoardTheme.MUTED);
            }
            default -> null;
        };
    }

    private static Component s(String key, Object... args) {
        return Component.translatable("gui.frostedheart.board.town.services." + key, args);
    }

    private static String count(long value) {
        return com.teammoeg.frostedheart.content.town.ui.TownFormat.count(Math.max(0, value));
    }

    private void renderList(
            GuiGraphics graphics, Font font, int x, int y,
            List<VirtualResourceType> types
    ) {
        int maxScroll = Math.max(0, types.size() - visibleRows());
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        for (int row = 0; row < visibleRows(); row++) {
            int index = listScroll + row;
            if (index >= types.size()) break;
            VirtualResourceType type = types.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            boolean hovered = getMouseX() >= 2 && getMouseX() < LIST_WIDTH - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT;
            // Each service is a paper tag; the chosen one sticks out a little.
            int indent = type == selectedType ? 4 : 0;
            (type == selectedType || hovered ? NoticeBoardIcons.TAG_HOVER : NoticeBoardIcons.TAG)
                    .draw(graphics, x + 2 + indent, rowY, LIST_WIDTH - 8, ROW_HEIGHT - 3);
            CIcons.getIcon(icon(type)).draw(graphics, x + 5 + indent, rowY + 1, 16, 16);
            graphics.drawString(
                    font,
                    TownTextLayout.ellipsize(font, resourceName(type).getString(), LIST_WIDTH - 34),
                    x + 24 + indent,
                    rowY + 5,
                    type == selectedType ? NoticeBoardTheme.NOTE_INK : NoticeBoardTheme.INK,
                    false);
        }
        if (maxScroll > 0) {
            int trackY = y + LIST_TOP;
            int trackHeight = visibleRows() * ROW_HEIGHT;
            int thumbHeight = Math.max(
                    MIN_THUMB_HEIGHT, trackHeight * visibleRows() / types.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * listScroll / maxScroll;
            NoticeBoardTheme.INSTANCE.drawSliderBackground(graphics, x + LIST_WIDTH - 4, trackY, 6, trackHeight, false);
            NoticeBoardTheme.INSTANCE.drawSliderBar(graphics, x + LIST_WIDTH - 4, thumbY, 6, thumbHeight, false);
        }
    }

    private void renderDetailHeader(GuiGraphics graphics, Font font, int x, int y) {
        VirtualResourceType type = selectedType;
        if (type == null) return;
        CIcons.getIcon(icon(type)).draw(graphics, x + DETAIL_LEFT + 2, y + 2, 16, 16);
        com.teammoeg.frostedheart.content.town.client.board.BoardDraw.textOnWood(graphics,
                TownTextLayout.ellipsize(
                        font, resourceName(type).getString(), getWidth() - DETAIL_LEFT - 24),
                x + DETAIL_LEFT + 21,
                y + 6,
                NoticeBoardTheme.ON_WOOD);
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
            case RESEARCH_LABOR -> researchLaborRows(town.getResourceHolder());
            default -> genericRows(town.getResourceHolder(), selectedType);
        };
    }

    /**
     * Today's research labor by education level, shown in whole standard researchers as the institutes do.
     */
    private static List<TownInfoPanel.Row> researchLaborRows(TeamTownResourceHolder resources) {
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        double total = 0;
        for (int level = 0; level <= VirtualResourceType.RESEARCH_LABOR.getMaxLevel(); level++)
            total += resources.get(VirtualResourceType.RESEARCH_LABOR.generateAttribute(level));
        rows.add(total <= TeamTownResourceHolder.DELTA ? status("no_research_labor", 0xFFFFAA00) : status("available", 0xFF55FF55));
        rows.add(TownInfoPanel.Row.text(Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.research_labor_total", (long) Math.floor(total + 1e-9))));
        for (int level = 0; level <= VirtualResourceType.RESEARCH_LABOR.getMaxLevel(); level++) {
            double value = resources.get(VirtualResourceType.RESEARCH_LABOR.generateAttribute(level));
            if (value <= TeamTownResourceHolder.DELTA) continue;
            rows.add(TownInfoPanel.Row.text(Component.translatable(
                    "gui.frostedheart.town_manager.virtual_resource.research_labor_level",
                    Component.translatable("gui.frostedheart.town_manager.education_level." + level),
                    (long) Math.floor(value + 1e-9))));
        }
        rows.add(TownInfoPanel.Row.colored(Component.translatable(
                "gui.frostedheart.town_manager.virtual_resource.research_labor_explain"), 0xFFAAAAAA));
        return rows;
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
        // The player view rounds as the summary note does: used up, total down, and what is left is their difference.
        boolean detailed = com.teammoeg.frostedheart.content.town.client.TownUiModeClient.detailed();
        double total = detailed ? capacity.total() : Math.floor(capacity.total() + 1e-9);
        double used = detailed ? capacity.used() : Math.ceil(capacity.used() - 1e-9);
        double available = detailed ? capacity.available() : Math.max(0, total - used);
        rows.add(amount("warehouse_total", total, 0xFFFFFFFF));
        rows.add(amount(
                "warehouse_occupied",
                used,
                capacity.overcommitted() ? 0xFFFF5555 : 0xFFFFFFFF));
        rows.add(amount(
                "warehouse_available",
                available,
                capacity.available() <= capacity.total() * 0.1
                        ? 0xFFFFAA00 : 0xFF55FF55));
        if (capacity.overcommitted()) {
            rows.add(amount("capacity_shortfall", capacity.shortfall(), 0xFFFF5555));
        }
        rows.add(percent("warehouse_utilization", capacity.utilizationFraction(), 0xFFFFFFFF));
        return rows;
    }

    private List<TownInfoPanel.Row> transportRows(TeamTown town) {
        TownTransportSnapshot snapshot = town.getTransportSnapshot();
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
                    distanceFactor(reservation, snapshot)),
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
            TownTransportSnapshot snapshot
    ) {
        if (reservation.admissionStatus() == TransportAdmissionStatus.UNAVAILABLE) {
            return "-";
        }
        double costPerBlock = reservation.endpointKind() == TransportEndpointKind.P2P_DIRECT_LINK
                ? snapshot.p2pDistanceCostPerBlock()
                : snapshot.warehouseDistanceCostPerBlock();
        double factor = 1.0 + costPerBlock * reservation.scaleMetric();
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
        if (row < 0 || row >= visibleRows()) return false;
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
        int maxScroll = Math.max(0, resourceTypes().size() - visibleRows());
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
        if (row < 0 || row >= visibleRows()) return;
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
            case RESEARCH_LABOR -> Items.WRITABLE_BOOK;
            default -> Items.PAPER;
        };
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value) && Math.abs(value) < 1.0e9) {
            return String.valueOf((long) value);
        }
        // The player view shows whole numbers; the detailed mode keeps one decimal for checking.
        if (!com.teammoeg.frostedheart.content.town.client.TownUiModeClient.detailed()) return String.valueOf((long) Math.floor(value + 1e-9));
        return String.format(Locale.ROOT, "%.1f", value);
    }

}
