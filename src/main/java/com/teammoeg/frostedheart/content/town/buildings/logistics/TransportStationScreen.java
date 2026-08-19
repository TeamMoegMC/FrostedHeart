/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.tabs.TownWorkforcePanel;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Foundation-stage transport station screen.
 * <p>
 * The station has no daily output yet, so its workforce view intentionally
 * reports zero productivity, contribution, and proficiency gain.
 */
public class TransportStationScreen extends StandardTownBuildingScreen<TransportStationMenu> {
    public TransportStationScreen(TransportStationMenu menu) {
        super(menu);
    }

    @Override
    protected void initTabs() {
        addTab(new OverviewTab(this));
        addTab(new WorkersTab(this));
    }

    private abstract static class TransportStationTab extends AbstractTownTab<TransportStationMenu> {
        private TransportStationTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getIcon() {
            return INACTIVE_TAB;
        }

        @Override
        public CIcons.CIcon getActiveIcon() {
            return ACTIVE_TAB;
        }
    }

    private static final class OverviewTab extends TransportStationTab {
        private OverviewTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(FHBlocks.TRANSPORT_STATION.get());
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.transport_station.overview");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> overviewRows(getMenu())));
        }
    }

    private static final class WorkersTab extends TransportStationTab {
        private WorkersTab(TransportStationScreen screen) {
            super(screen);
        }

        @Override
        public CIcons.CIcon getContentIcon() {
            return CIcons.getIcon(Items.CHEST);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.frostedheart.transport_station.workers");
        }

        @Override
        public void build(UILayer layer) {
            layer.add(new TownWorkforcePanel(
                    layer, 8, 4,
                    getMenu()::getResidents,
                    resident -> getMenu().getBuilding()
                            .map(building -> building.canResidentWork(resident))
                            .orElse(false),
                    TransportStationScreen::transportProficiency,
                    resident -> 0.0,
                    resident -> 0.0,
                    resident -> 0.0,
                    Component.translatable("gui.frostedheart.transport_station.transport_proficiency"),
                    value -> Component.translatable(
                            "gui.frostedheart.transport_station.personal_contribution",
                            MineBaseScreen.two(value))
            ));
        }
    }

    private static List<TownInfoPanel.Row> overviewRows(TransportStationMenu menu) {
        TransportStationBuilding building = menu.getBuilding().orElse(null);
        if (building == null) return MineBaseScreen.missingBuildingRows();

        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.transport_station.overview"));
        rows.add(MineBaseScreen.status(
                "gui.frostedheart.town.workable", building.isBuildingWorkable()));
        if (!building.isBuildingWorkable()) {
            MineBaseScreen.appendCommonFailures(rows, building);
            if (building.getArea() < config.minimumFloorAreaBlocks.get()) {
                rows.add(MineBaseScreen.failure(
                        "gui.frostedheart.town.failure.area",
                        building.getArea(), config.minimumFloorAreaBlocks.get()));
            }
            if (building.getVolume() < config.minimumInteriorVolumeBlocks.get()) {
                rows.add(MineBaseScreen.failure(
                        "gui.frostedheart.town.failure.volume",
                        building.getVolume(), config.minimumInteriorVolumeBlocks.get()));
            }
        }
        rows.add(MineBaseScreen.text("gui.frostedheart.town.workers_count",
                menu.getResidents().size(), building.getMaxResidents()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.area", building.getArea()));
        rows.add(MineBaseScreen.text("gui.frostedheart.town.volume", building.getVolume()));
        return rows;
    }

    private static double transportProficiency(Resident resident) {
        return resident.getWorkProficiency().getDouble(TransportStationBuilding.class.getSimpleName());
    }
}
