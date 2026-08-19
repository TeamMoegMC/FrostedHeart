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
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentBuilding;
import com.teammoeg.frostedheart.content.town.building.ITownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.TownTextLayout;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.Locale;

/**
 * 城镇方块页签的内容面板。左侧为可滚动的建筑名单，点击选择后
 * 右侧显示该建筑的类型、坐标、运行状态（及不可用原因）与居民容量。
 * 布局与交互参照 {@link TownResidentsPanel}。
 * <p>
 * Content panel of the town buildings tab. A scrollable building list on the
 * left; clicking a building shows its type, coordinates, operating status
 * (with failure reasons) and resident capacity on the right.
 */
public class TownBuildingsPanel extends UIElement {

    private static final int WIDTH = TownManagerScreen.CONTENT_WIDTH;
    private static final int HEIGHT = TownManagerScreen.CONTENT_HEIGHT;
    private static final int LIST_WIDTH = 96;
    private static final int LIST_TOP = 18;
    private static final int ROW_HEIGHT = 16;
    private static final int VISIBLE_ROWS = (HEIGHT - LIST_TOP - 4) / ROW_HEIGHT;
    private static final int DETAIL_LINE_HEIGHT = 12;
    private static final int DETAIL_TOP = 6;
    private static final int DETAIL_VISIBLE = (HEIGHT - DETAIL_TOP - 4) / DETAIL_LINE_HEIGHT;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int DETAIL_TEXT_WIDTH = WIDTH - LIST_WIDTH - SCROLLBAR_WIDTH - 13;
    private static final int MIN_THUMB_HEIGHT = 10;

    private final Supplier<TeamTown> townSource;

    @Nullable
    private BlockPos selectedBuilding;
    private int listScroll;
    private int detailScroll;
    private boolean draggingDetailBar;

    public TownBuildingsPanel(UIElement parent, int x, int y, Supplier<TeamTown> townSource) {
        super(parent);
        this.townSource = townSource;
        setPos(x, y);
        setSize(WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height, RenderingHint hint) {
        Font font = Minecraft.getInstance().font;
        List<AbstractTownBuilding> buildings = buildings();
        AbstractTownBuilding selected = normalizeSelection(buildings);
        drawPanel(graphics, x, y, width, height);
        graphics.fill(x + LIST_WIDTH, y + 1, x + LIST_WIDTH + 1, y + height - 1, 0xFF777777);
        graphics.drawString(font,
                Component.translatable("gui.frostedheart.town_manager.buildings"),
                x + 4, y + 6, 0xFFFFAA00, true);
        renderList(graphics, font, x, y, buildings);
        renderDetails(graphics, font, x, y, detailLines(selected));
    }

    private void renderList(GuiGraphics graphics, Font font, int x, int y, List<AbstractTownBuilding> buildings) {
        int maxScroll = Math.max(0, buildings.size() - VISIBLE_ROWS);
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = listScroll + row;
            if (index >= buildings.size()) break;
            AbstractTownBuilding building = buildings.get(index);
            int rowY = y + LIST_TOP + row * ROW_HEIGHT;
            if (building.getPos().equals(selectedBuilding)) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0xA0446688);
            } else if (getMouseX() >= 2 && getMouseX() < LIST_WIDTH - 3
                    && getMouseY() >= LIST_TOP + row * ROW_HEIGHT
                    && getMouseY() < LIST_TOP + (row + 1) * ROW_HEIGHT) {
                graphics.fill(x + 2, rowY, x + LIST_WIDTH - 3, rowY + ROW_HEIGHT - 1, 0x60444444);
            }
            graphics.drawString(font,
                    TownTextLayout.ellipsize(font,
                            TownResidentsPanel.buildingName(building.getClass().getSimpleName()).getString(),
                            LIST_WIDTH - 9),
                    x + 5, rowY + 4,
                    building.isStructureValid() ? 0xFFFFFFFF : 0xFFFF5555, true);
        }
        if (maxScroll > 0) {
            int trackY = y + LIST_TOP;
            int trackHeight = VISIBLE_ROWS * ROW_HEIGHT;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * VISIBLE_ROWS / buildings.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * listScroll / maxScroll;
            graphics.fill(x + LIST_WIDTH - 3, trackY, x + LIST_WIDTH - 1, trackY + trackHeight, 0xFF222222);
            graphics.fill(x + LIST_WIDTH - 3, thumbY, x + LIST_WIDTH - 1, thumbY + thumbHeight, 0xFFAAAAAA);
        }
    }

    private void renderDetails(GuiGraphics graphics, Font font, int x, int y, List<Line> lines) {
        List<VisualLine> visualLines = wrapDetailLines(font, lines);
        int maxScroll = Math.max(0, visualLines.size() - DETAIL_VISIBLE);
        detailScroll = Mth.clamp(detailScroll, 0, maxScroll);
        int textX = x + LIST_WIDTH + 5;
        graphics.enableScissor(x + LIST_WIDTH + 1, y + 1, x + WIDTH - SCROLLBAR_WIDTH - 3, y + HEIGHT - 1);
        for (int row = 0; row < DETAIL_VISIBLE; row++) {
            int index = detailScroll + row;
            if (index >= visualLines.size()) break;
            VisualLine line = visualLines.get(index);
            graphics.drawString(font, line.text(),
                    textX, y + DETAIL_TOP + row * DETAIL_LINE_HEIGHT, line.color(), true);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int trackX = x + WIDTH - SCROLLBAR_WIDTH - 2;
            int trackY = y + 2;
            int trackHeight = HEIGHT - 4;
            int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * DETAIL_VISIBLE / visualLines.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * detailScroll / maxScroll;
            graphics.fill(trackX, trackY, trackX + SCROLLBAR_WIDTH, trackY + trackHeight, 0xFF202020);
            graphics.fill(trackX + 1, thumbY, trackX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                    draggingDetailBar ? 0xFFD0D0D0 : 0xFF909090);
        }
    }

    private List<Line> detailLines(@Nullable AbstractTownBuilding building) {
        if (building == null) {
            return List.of(new Line(
                    Component.translatable("gui.frostedheart.town_manager.no_buildings"), 0xFFAAAAAA));
        }
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(TownResidentsPanel.buildingName(building.getClass().getSimpleName()), 0xFFFFAA00));
        BlockPos pos = building.getPos();
        lines.add(new Line(Component.translatable("gui.frostedheart.town.coordinates",
                pos.getX(), pos.getY(), pos.getZ()), 0xFFFFFFFF));

        showWorkableInfo(lines, building);

        if (building instanceof HouseBuilding house) {
            addHouseDetails(lines, house);
        } else if (building instanceof HuntingBaseBuilding hunting) {
            addHuntingDetails(lines, hunting);
        } else if (building instanceof TransportStationBuilding transportStation) {
            addTransportStationDetails(lines, transportStation);
        }

        if (building instanceof ITownResidentBuilding residentBuilding) {
            addResidentCount(lines, pos, residentBuilding);
        }
        return lines;
    }

    private static void addHouseDetails(List<Line> lines, HouseBuilding house) {
        lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.effective_temperature",
                formatOneDecimal(house.getEffectiveTemperature())),
                house.isTemperatureValid() ? 0xFF55FF55 : 0xFFFF5555));
        HouseBuilding.DailyReport report = house.getDailyReport();
        if (report.hasData()) {
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.food_satisfaction",
                    formatPercent(report.foodSatisfaction())), 0xFFFFFFFF));
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.comfort",
                    formatPercent(report.comfortRating())), 0xFFFFFFFF));
        } else {
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.daily_data_unavailable"),
                    0xFF777777));
        }
        FHConfig.Server.Town.Housing config = FHConfig.SERVER.TOWN.HOUSING;
        if (house.getArea() < config.minimumFloorAreaBlocks.get()) {
            lines.add(reason("gui.frostedheart.town.failure.area",
                    house.getArea(), config.minimumFloorAreaBlocks.get()));
        }
        if (house.getVolume() < config.minimumInteriorVolumeBlocks.get()) {
            lines.add(reason("gui.frostedheart.town.failure.volume",
                    house.getVolume(), config.minimumInteriorVolumeBlocks.get()));
        }
        if (!house.isTemperatureValid()) {
            lines.add(reason("gui.frostedheart.town.failure.temperature"));
        }
    }

    private static void addHuntingDetails(List<Line> lines, HuntingBaseBuilding hunting) {
        lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.effective_temperature",
                formatOneDecimal(hunting.getEffectiveTemperature())),
                hunting.isTemperatureValid() ? 0xFF55FF55 : 0xFFFF5555));
        HuntingBaseBuilding.HuntingDailyReport report = hunting.getDailyReport();
        if (report.hasData()) {
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.hunting_rolls",
                    report.plannedRolls(), report.executedRolls()), 0xFFFFFFFF));
        } else {
            lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.daily_data_unavailable"),
                    0xFF777777));
        }
        Component stopReason;
        if (!hunting.isTemperatureValid()) {
            stopReason = Component.translatable("gui.frostedheart.town_manager.stop_reason.temperature");
        } else if (!hunting.isSpaceValid()) {
            stopReason = Component.translatable("gui.frostedheart.town_manager.stop_reason.space");
        } else {
            stopReason = Component.translatable("gui.frostedheart.town_manager.stop_reason."
                    + report.stopReason().name().toLowerCase(Locale.ROOT));
        }
        lines.add(new Line(Component.translatable("gui.frostedheart.town_manager.stop_reason", stopReason),
                report.stopReason().name().equals("NONE") && hunting.isTemperatureValid()
                        && hunting.isSpaceValid() ? 0xFF55FF55 : 0xFFFFAA00));
        FHConfig.Server.Town.Hunting config = FHConfig.SERVER.TOWN.HUNTING;
        if (hunting.getArea() < config.minimumFloorAreaBlocks.get()) {
            lines.add(reason("gui.frostedheart.town.failure.area",
                    hunting.getArea(), config.minimumFloorAreaBlocks.get()));
        }
        if (hunting.getVolume() < config.minimumInteriorVolumeBlocks.get()) {
            lines.add(reason("gui.frostedheart.town.failure.volume",
                    hunting.getVolume(), config.minimumInteriorVolumeBlocks.get()));
        }
        if (!hunting.isTemperatureValid()) {
            lines.add(reason("gui.frostedheart.town.failure.temperature"));
        }
    }

    private static void addTransportStationDetails(
            List<Line> lines,
            TransportStationBuilding transportStation
    ) {
        lines.add(new Line(Component.translatable("gui.frostedheart.town.area",
                transportStation.getArea()), 0xFFFFFFFF));
        lines.add(new Line(Component.translatable("gui.frostedheart.town.volume",
                transportStation.getVolume()), 0xFFFFFFFF));

        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        if (transportStation.getArea() < config.minimumFloorAreaBlocks.get()) {
            lines.add(reason("gui.frostedheart.town.failure.area",
                    transportStation.getArea(), config.minimumFloorAreaBlocks.get()));
        }
        if (transportStation.getVolume() < config.minimumInteriorVolumeBlocks.get()) {
            lines.add(reason("gui.frostedheart.town.failure.volume",
                    transportStation.getVolume(), config.minimumInteriorVolumeBlocks.get()));
        }
    }

    private static String formatOneDecimal(double value) {
        // Keep the numeric argument unit-free. Each locale owns its unit text;
        // the English default font used by the pack does not provide a degree glyph.
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", Math.max(0.0, Math.min(1.0, value)) * 100.0);
    }

    private static void showWorkableInfo(List<Line> lines, AbstractTownBuilding building) {
        boolean workable = building.isBuildingWorkable();
        lines.add(new Line(Component.translatable("gui.frostedheart.town.workable",
                Component.translatable(workable
                        ? "gui.frostedheart.common.yes"
                        : "gui.frostedheart.common.no")), workable ? 0xFF55FF55 : 0xFFFF5555));
        if (!workable) {
            if (!building.isInitialized()) {
                lines.add(reason("gui.frostedheart.town.failure.uninitialized"));
            }
            if (building.isOccupiedAreaOverlapped()) {
                lines.add(reason("gui.frostedheart.town.failure.overlap"));
            }
            if (!building.isStructureValid()) {
                lines.add(reason("gui.frostedheart.town.failure.structure"));
            }
        }
    }

    private void addResidentCount(List<Line> lines, BlockPos pos, ITownResidentBuilding residentBuilding) {
        TeamTown town = townSource.get();
        int count = 0;
        if (town != null) {
            boolean isHouse = !(residentBuilding instanceof ITownResidentWorkBuilding);
            for (Resident r : town.getAllResidents()) {
                BlockPos assigned = isHouse ? r.getHousePos() : r.getWorkPos();
                if (pos.equals(assigned)) {
                    count++;
                }
            }
        }
        String labelKey = residentBuilding instanceof ITownResidentWorkBuilding
                ? "gui.frostedheart.town_manager.workers"
                : "gui.frostedheart.town_manager.resident_count";
        lines.add(new Line(Component.empty(), 0xFFFFFFFF));
        lines.add(new Line(Component.translatable(labelKey)
                .append(Component.literal(": " + count
                        + " / " + residentBuilding.getMaxResidents())), 0xFFFFFFFF));
    }

    @Override
    public boolean onMousePressed(MouseButton button) {
        if (!isMouseOver() || button != MouseButton.LEFT) return false;
        if (getMouseX() >= WIDTH - SCROLLBAR_WIDTH - 2 && detailScrollable()) {
            draggingDetailBar = true;
            updateDetailScrollFromMouse();
            return true;
        }
        if (getMouseX() >= LIST_WIDTH || getMouseY() < LIST_TOP) return false;
        int row = (int) ((getMouseY() - LIST_TOP) / ROW_HEIGHT);
        if (row < 0 || row >= VISIBLE_ROWS) return false;
        List<AbstractTownBuilding> buildings = buildings();
        int index = listScroll + row;
        if (index >= buildings.size()) return false;
        selectedBuilding = buildings.get(index).getPos();
        detailScroll = 0;
        return true;
    }

    @Override
    public boolean onMouseScrolled(double scroll) {
        if (!isMouseOver()) return false;
        if (getMouseX() < LIST_WIDTH) {
            int max = Math.max(0, buildings().size() - VISIBLE_ROWS);
            if (max == 0) return false;
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scroll), 0, max);
            return true;
        }
        int max = Math.max(0, wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(buildings()))).size() - DETAIL_VISIBLE);
        if (max == 0) return false;
        detailScroll = Mth.clamp(detailScroll - (int) Math.signum(scroll), 0, max);
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseButton button, double dragX, double dragY) {
        if (!draggingDetailBar) return super.onMouseDragged(button, dragX, dragY);
        updateDetailScrollFromMouse();
        return true;
    }

    @Override
    public void onMouseReleased(MouseButton button) {
        draggingDetailBar = false;
        super.onMouseReleased(button);
    }

    private void updateDetailScrollFromMouse() {
        int lineCount = wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(buildings()))).size();
        int max = Math.max(0, lineCount - DETAIL_VISIBLE);
        if (max == 0) {
            detailScroll = 0;
            return;
        }
        int trackHeight = HEIGHT - 4;
        int thumbHeight = Math.max(MIN_THUMB_HEIGHT, trackHeight * DETAIL_VISIBLE / lineCount);
        int travel = trackHeight - thumbHeight;
        if (travel <= 0) return;
        double top = getMouseY() - 2 - thumbHeight / 2.0;
        detailScroll = (int) Math.round(Mth.clamp(top / travel, 0.0, 1.0) * max);
    }

    private boolean detailScrollable() {
        return wrapDetailLines(Minecraft.getInstance().font,
                detailLines(normalizeSelection(buildings()))).size() > DETAIL_VISIBLE;
    }

    @Nullable
    private AbstractTownBuilding normalizeSelection(List<AbstractTownBuilding> buildings) {
        if (buildings.isEmpty()) {
            selectedBuilding = null;
            listScroll = 0;
            detailScroll = 0;
            return null;
        }
        for (AbstractTownBuilding building : buildings) {
            if (building.getPos().equals(selectedBuilding)) return building;
        }
        selectedBuilding = buildings.get(0).getPos();
        detailScroll = 0;
        return buildings.get(0);
    }

    private List<AbstractTownBuilding> buildings() {
        TeamTown town = townSource.get();
        if (town == null) return List.of();
        return List.copyOf(town.getTownBuildings().values());
    }

    private static Line reason(String key) {
        return new Line(Component.literal("• ").append(Component.translatable(key)), 0xFFFF5555);
    }

    private static Line reason(String key, Object... arguments) {
        return new Line(Component.literal("• ").append(Component.translatable(key, arguments)), 0xFFFF5555);
    }

    private static List<VisualLine> wrapDetailLines(Font font, List<Line> lines) {
        List<VisualLine> wrapped = new ArrayList<>();
        for (Line line : lines) {
            for (FormattedCharSequence text : TownTextLayout.wrap(font, line.text(), DETAIL_TEXT_WIDTH)) {
                wrapped.add(new VisualLine(text, line.color()));
            }
        }
        return wrapped;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xE0101010);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF8B8B8B);
    }

    private record Line(Component text, int color) {}

    private record VisualLine(FormattedCharSequence text, int color) {}
}
