package com.teammoeg.frostedheart.content.town.event;

import java.util.EventListener;

public interface ITownBuildingChangeEventListener extends EventListener {
    public void onBuildingChange(TownBuildingChangeEvent event);
}
