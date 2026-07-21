package com.teammoeg.frostedheart.content.town.event;

import java.util.EventListener;

public interface ITownResourceChangeEventListener extends EventListener {
    public void onResourceChange(TownResourceChangeEvent event);
}
