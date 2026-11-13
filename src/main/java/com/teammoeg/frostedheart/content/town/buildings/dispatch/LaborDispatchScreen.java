package com.teammoeg.frostedheart.content.town.buildings.dispatch;

import java.util.List;
import com.teammoeg.frostedheart.content.town.StandardTownBuildingScreen;
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
            return List.of(
                TownInfoPanel.Row.text(Component.translatable("container.frostedheart.labor_dispatch")),
                TownInfoPanel.Row.text(Component.translatable("gui.frostedheart.labor_dispatch.workers", building == null || town == null ? 0 : building.availableWorkers(town))),
                TownInfoPanel.Row.text(Component.translatable("gui.frostedheart.labor_dispatch.slots", building == null ? 0 : building.getMaxResidents())));
        }));
    }
}
