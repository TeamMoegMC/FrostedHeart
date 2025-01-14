package com.teammoeg.frostedheart.content.town.resource;

/**
 * A class that contains ITownResourceType and level of the resource.
 */
public interface ITownResourceKey {
    ITownResourceType getType();
    int getLevel();

    static ITownResourceKey of(ITownResourceType type, int level){
        return type.generateKey(level);
    }
}
