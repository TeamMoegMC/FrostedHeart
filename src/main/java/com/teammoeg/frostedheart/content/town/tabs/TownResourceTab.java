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
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.network.WarehouseC2SRequestPacket;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.VirtualItemGridElement;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseScreen;

public class TownResourceTab extends AbstractTownTab<WarehouseMenu> {

    public TownResourceTab(AbstractTownWorkerBlockScreen<WarehouseMenu> screen) {
        super(screen);
    }

    @Override
    public void build(UILayer layer) {
        FHNetwork.INSTANCE.sendToServer(new WarehouseC2SRequestPacket());
        VirtualItemGridElement grid = new VirtualItemGridElement(layer, 7, 18,
                () -> getMenu().getResources()
        );
        layer.add(grid);
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
