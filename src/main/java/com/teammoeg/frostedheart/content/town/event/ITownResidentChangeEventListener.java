package com.teammoeg.frostedheart.content.town.event;

import java.util.EventListener;

public interface ITownResidentChangeEventListener extends EventListener {
    public void onResidentChange(TownResidentChangeEvent event);
}
