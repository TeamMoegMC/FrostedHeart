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

package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.cui.base.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TownInformationTab extends AbstractTownTab<WarehouseMenu> {

    public TownInformationTab(AbstractTownWorkerBlockScreen<WarehouseMenu> screen) {
        super(screen);
    }

    @Override
    public void build(UILayer layer) {
        BuildingInfoElement infoElement = new BuildingInfoElement(
                layer,
                8, 4,
                160, 130,
                this::collectBuildingInfo
        );
        layer.add(infoElement);
    }

    private List<Component> collectBuildingInfo() {
        List<Component> lines = new ArrayList<>();

        WarehouseMenu menu = screen.getCBEMenu();
        lines.add(Component.translatable("gui.frostedheart.warehouse.overview")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(BuildingInfoElement.separator());

        if (!menu.hasBuilding()) {
            lines.add(Component.translatable("gui.frostedheart.warehouse.unavailable")
                    .withStyle(ChatFormatting.RED));
            return lines;
        }

        lines.add(localizedStatus("gui.frostedheart.warehouse.workable", menu.isWorkable()));
        if (!menu.isWorkable()) {
            if (!menu.isInitialized()) {
                lines.add(failure("gui.frostedheart.warehouse.failure.not_initialized"));
            }
            if (menu.isAreaOverlapped()) {
                lines.add(failure("gui.frostedheart.warehouse.failure.area_overlapped"));
            }
            if (!menu.isStructureValid()) {
                lines.add(failure("gui.frostedheart.warehouse.failure.invalid_structure"));
            }
        }
        lines.add(BuildingInfoElement.separator());

        lines.add(value("gui.frostedheart.warehouse.item_capacity", Math.round(menu.getCapacity())));
        lines.add(value("gui.frostedheart.warehouse.area", menu.getArea()));
        lines.add(value("gui.frostedheart.warehouse.volume", menu.getVolume()));
        return lines;
    }

    @Override
    public CIcons.CIcon getIcon() {
        return WarehouseScreen.INACTIVE_TAB;
    }

    @Override
    public CIcons.CIcon getActiveIcon() {
        return WarehouseScreen.ACTIVE_TAB;
    }

    @Override
    public CIcons.CIcon getContentIcon() {
        return CIcons.getIcon(FHBlocks.WAREHOUSE.get());
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.frostedheart.warehouse.overview");
    }

    private static Component localizedStatus(String key, boolean value) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": "))
                .append(Component.translatable(value
                                ? "gui.frostedheart.warehouse.yes"
                                : "gui.frostedheart.warehouse.no")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static Component failure(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.RED);
    }

    private static Component value(String key, long value) {
        return Component.translatable(key)
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(": " + value).withStyle(ChatFormatting.AQUA));
    }
}
