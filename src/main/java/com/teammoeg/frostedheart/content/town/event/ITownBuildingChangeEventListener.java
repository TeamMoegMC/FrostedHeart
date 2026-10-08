package com.teammoeg.frostedheart.content.town.event;

import java.util.EventListener;
@FunctionalInterface
public interface ITownBuildingChangeEventListener extends EventListener {
    public void onBuildingChange(TownBuildingChangeEvent event);
    public default void onBuildingZoneChange(TownBuildingChangeEvent event) {};
}
