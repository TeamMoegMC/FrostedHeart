package com.teammoeg.frostedheart.content.town.event;

import net.minecraft.core.BlockPos;

import java.util.EventObject;

public class TownBuildingChangeEvent extends EventObject {
    public final BlockPos changedBuildingPos;
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public TownBuildingChangeEvent(Object source, BlockPos changedBuildingPos) {
        super(source);
        this.changedBuildingPos = changedBuildingPos;
    }
}
