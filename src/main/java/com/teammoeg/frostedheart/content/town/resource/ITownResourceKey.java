package com.teammoeg.frostedheart.content.town.resource;

/**
 * A class that contains ITownResourceType and level of the resource.
 * You can change certain resources with this town resource key in TownResourceManager.
 */
public interface ITownResourceKey {
    ITownResourceType getType();
    int getLevel();

    /**
     * Create a new town resource key of given type and level.
     * Can accept interface ITownResourceType.
     */
    static ITownResourceKey of(ITownResourceType type, int level){
        return type.generateKey(level);
    }
}
