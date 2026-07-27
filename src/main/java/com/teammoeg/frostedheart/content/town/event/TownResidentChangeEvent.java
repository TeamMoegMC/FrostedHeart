package com.teammoeg.frostedheart.content.town.event;

import java.util.EventObject;
import java.util.UUID;

public class TownResidentChangeEvent extends EventObject {
    public final UUID changedResidentID;
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public TownResidentChangeEvent(Object source, UUID changedResidentID) {
        super(source);
        this.changedResidentID = changedResidentID;
    }
}
