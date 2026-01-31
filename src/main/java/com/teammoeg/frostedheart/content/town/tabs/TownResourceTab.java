package com.teammoeg.frostedheart.content.town.tabs;

import com.teammoeg.chorda.client.cui.UILayer;
import com.teammoeg.chorda.client.icon.CIcons;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlockScreen;
import com.teammoeg.frostedheart.content.town.network.WarehouseC2SRequestPacket;
import com.teammoeg.frostedheart.content.town.warehouse.VirtualItemGridElement;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseMenu;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseScreen;

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
