package com.teammoeg.frostedheart.content.town.resource;

public interface ITownResourceType {
    int getMaxLevel();

    /**
     * Get key of this resource type as string.
     * @return lowercase string of the name.
     */
    String getKey();

    /**
     * Generate key of this resource type with given level.
     * @param level level
     * @return TownResourceKey
     */
    ITownResourceKey generateKey(int level);

    static ITownResourceType from(String key){
        for(ITownResourceType type:ItemResourceType.values()){
            if(type.getKey().equals(key)) return type;
        }
        for(ITownResourceType type:VirtualResourceType.values()){
            if(type.getKey().equals(key)) return type;
        }
        return null;
    }

    default boolean isLevelValid(int level){
        return level>=0&&level<=getMaxLevel();
    }
}
