package com.teammoeg.frostedheart.content.town.event;

import java.util.EventObject;

import com.teammoeg.frostedheart.content.town.resource.ITownResourceKey;

public class TownResourceChangeEvent extends EventObject {
    public final ITownResourceKey changedResourceKey;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public TownResourceChangeEvent(Object source, ITownResourceKey changedResourceKey ) {
        super(source);
        this.changedResourceKey = changedResourceKey;
    }
}
