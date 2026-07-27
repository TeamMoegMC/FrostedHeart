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
 */

package com.teammoeg.frostedheart.content.town.buildings.mine;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.tabs.AbstractTownTab;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import com.teammoeg.frostedheart.content.town.terrainresource.TerrainResourceType;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MineScreen extends StandardTownBuildingScreen<MineMenu> {
    public MineScreen(MineMenu menu) {
        super(menu);
    }

    @Override
    protected void initTabs() {
        addTab(new OverviewTab(this));
        addTab(new DepositTab(this));
    }

    private abstract static class MineTab extends AbstractTownTab<MineMenu> {
        private MineTab(MineScreen screen) { super(screen); }
        @Override public CIcons.CIcon getIcon() { return INACTIVE_TAB; }
        @Override public CIcons.CIcon getActiveIcon() { return ACTIVE_TAB; }
    }

    private static final class OverviewTab extends MineTab {
        private OverviewTab(MineScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(FHBlocks.MINE.get()); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.mine.overview");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> overviewRows(getMenu())));
        }
    }

    private static final class DepositTab extends MineTab {
        private DepositTab(MineScreen screen) { super(screen); }
        @Override public CIcons.CIcon getContentIcon() { return CIcons.getIcon(Items.COAL); }
        @Override public Component getTitle() {
            return Component.translatable("gui.frostedheart.mine.deposit");
        }
        @Override public void build(UILayer layer) {
            layer.add(new TownInfoPanel(layer, 8, 4, 160, 130,
                    () -> depositRows(getMenu())));
        }
    }

    private static List<TownInfoPanel.Row> overviewRows(MineMenu menu) {
        MineBuilding mine = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (mine == null) return MineBaseScreen.missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        rows.add(MineBaseScreen.title("gui.frostedheart.mine.overview"));
        rows.add(MineBaseScreen.status(
                "gui.frostedheart.mine.building_usable", mine.isBuildingWorkable()));
        if (!mine.isBuildingWorkable()) {
            MineBaseScreen.appendCommonFailures(rows, mine);
        }

        MineBaseBuilding connectedBase = town == null ? null
                : town.getTownBuildings().values().stream()
                .filter(MineBaseBuilding.class::isInstance)
                .map(MineBaseBuilding.class::cast)
                .filter(base -> base.getLinkedMines().contains(mine.getPos()))
                .sorted(Comparator.comparingLong(base -> base.getPos().asLong()))
                .findFirst()
                .orElse(null);
        if (connectedBase == null) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine.not_connected"),
                    0xFFFF5555));
        } else {
            rows.add(MineBaseScreen.text(
                    "gui.frostedheart.mine.connected_base",
                    connectedBase.getPos().getX(),
                    connectedBase.getPos().getY(),
                    connectedBase.getPos().getZ()));
            if (!connectedBase.isBuildingWorkable()) {
                rows.add(TownInfoPanel.Row.colored(
                        Component.translatable("gui.frostedheart.mine.connected_base_unworkable"),
                        0xFFFF5555));
            }
        }
        Component biome = Component.translatable(
                Util.makeDescriptionId("biome", mine.getBiomePath()));
        rows.add(MineBaseScreen.text("gui.frostedheart.mine.biome", biome));
        return rows;
    }

    private static List<TownInfoPanel.Row> depositRows(MineMenu menu) {
        MineBuilding mine = menu.getBuilding().orElse(null);
        TeamTown town = menu.getTown().orElse(null);
        if (mine == null) return MineBaseScreen.missingBuildingRows();
        List<TownInfoPanel.Row> rows = new ArrayList<>();
        ChunkPos chunk = new ChunkPos(mine.getPos());
        double maximum = TerrainResourceType.ORE.getResourcePerSq();
        double remaining = town == null
                ? maximum
                : town.getRemainingTerrainResource(TerrainResourceType.ORE, chunk);
        double ratio = maximum <= 0.0 ? 0.0 : remaining / maximum;

        rows.add(MineBaseScreen.title("gui.frostedheart.mine.chunk_deposit"));
        rows.add(MineBaseScreen.text("gui.frostedheart.mine.chunk_coordinates", chunk.x, chunk.z));
        rows.add(MineBaseScreen.text(
                "gui.frostedheart.mine.remaining_deposit",
                Math.round(remaining), Math.round(maximum)));
        rows.add(TownInfoPanel.Row.colored(
                Component.translatable("gui.frostedheart.mine.remaining_percent",
                        String.format(Locale.ROOT, "%.1f", ratio * 100.0)),
                remaining <= 0.0 ? 0xFFFF5555 : 0xFFFFFFFF));
        if (remaining <= 0.0) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine.exhausted"),
                    0xFFFF5555));
        }
        rows.add(TownInfoPanel.Row.colored(
                Component.translatable("gui.frostedheart.mine.shared_chunk_deposit"),
                0xFFAAAAAA));
        rows.add(TownInfoPanel.Row.empty());
        rows.add(MineBaseScreen.title("gui.frostedheart.mine.output_composition"));

        Map<Item, Integer> weights = MineBuilding.getWeights(mine.getBiomePath());
        if (!MineBuilding.hasBiomeRecipe(mine.getBiomePath())) {
            rows.add(TownInfoPanel.Row.colored(
                    Component.translatable("gui.frostedheart.mine.default_composition"),
                    0xFFAAAAAA));
        }
        double totalWeight = weights.values().stream()
                .filter(weight -> weight > 0)
                .mapToDouble(Integer::doubleValue)
                .sum();
        weights.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()
                        .thenComparing(entry -> entry.getKey().getDescriptionId()))
                .forEach(entry -> {
                    double percentage = totalWeight <= 0.0
                            ? 0.0
                            : entry.getValue() * 100.0 / totalWeight;
                    rows.add(TownInfoPanel.Row.item(
                            entry.getKey(),
                            Component.translatable(
                                    "gui.frostedheart.mine.output_weight",
                                    entry.getKey().getDescription(),
                                    String.format(Locale.ROOT, "%.1f", percentage))));
                });
        return rows;
    }
}
