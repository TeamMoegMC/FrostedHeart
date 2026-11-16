package com.teammoeg.frostedheart.content.town.buildings.dispatch;

import java.util.ArrayList;
import java.util.List;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseScreen;
import com.teammoeg.frostedheart.content.town.tabs.TownInfoPanel;
import net.minecraft.network.chat.Component;

public class LaborDispatchScreen extends StandardTownBuildingScreen<LaborDispatchMenu> {
    public LaborDispatchScreen(LaborDispatchMenu menu) { super(menu); }
    @Override
    public void addChildUIElements() {
        super.addChildUIElements();
        add(new TownInfoPanel(this, 8, 8, 160, 118, () -> {
            var building = getCBEMenu().getBuilding().orElse(null);
            var town = getCBEMenu().getTown().orElse(null);
            List<TownInfoPanel.Row> rows = new ArrayList<>();
            rows.add(TownInfoPanel.Row.text(Component.translatable("container.frostedheart.labor_dispatch")));
            if (building != null) {
                rows.add(MineBaseScreen.status("gui.frostedheart.town.workable", building.isBuildingWorkable()));
                if (!building.isBuildingWorkable()) {
                    MineBaseScreen.appendCommonFailures(rows, building);
                    if (building.getArea() < 9) rows.add(MineBaseScreen.failure("gui.frostedheart.town.failure.area", building.getArea(), 9));
                    if (building.getVolume() < 18) rows.add(MineBaseScreen.failure("gui.frostedheart.town.failure.volume", building.getVolume(), 18));
                }
            }
            rows.add(TownInfoPanel.Row.text(Component.translatable("gui.frostedheart.labor_dispatch.workers", building == null || town == null ? 0 : building.availableWorkers(town))));
            rows.add(TownInfoPanel.Row.text(Component.translatable("gui.frostedheart.labor_dispatch.slots", building == null ? 0 : building.getMaxResidents())));
            return rows;
        }));
    }
}
