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
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemGridElement;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBlockEntity;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseScreen;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        if (!menu.hasBuilding()) {
            return lines;
        }
        lines.add(BuildingInfoElement.title("Building Info"));
        lines.add(BuildingInfoElement.separator());

        lines.add(BuildingInfoElement.status("Workable", menu.isWorkable()));
        lines.add(BuildingInfoElement.status("Initialized", menu.isInitialized()));
        lines.add(BuildingInfoElement.status("Structure Valid", menu.isStructureValid()));
        lines.add(BuildingInfoElement.status("Area Overlapped", menu.isAreaOverlapped()));
        lines.add(BuildingInfoElement.separator());

        lines.add(BuildingInfoElement.keyValue("Volume", menu.getVolume()));
        lines.add(BuildingInfoElement.keyValue("Area", menu.getArea()));
        lines.add(BuildingInfoElement.keyValue("Capacity",
                BigDecimal.valueOf(menu.getCapacity())
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue()));
        return lines;
    }

    @Override
    public CIcons.CIcon getIcon() {
        return WarehouseScreen.inactiveButton;
    }

    @Override
    public CIcons.CIcon getActiveIcon() {
        return WarehouseScreen.activeButton;
    }
}