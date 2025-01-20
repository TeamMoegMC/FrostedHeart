package com.teammoeg.frostedheart.content.town.resource;

/**
 * The interface of town resource type.
 * Town resource type should be a enum.
 * Town resource type have a max level as integer.
 * Max level can be 0 or any positive integer.
 */
public interface ITownResourceType {
    int getMaxLevel();

    /**
     * 生成这个TownResourceType的小写字符串。
     * 并非ItemResourceKey.
     * @return 该ItemResourceType名字的小写字符串。
     */
    String getKey();

    /**
     * Generate town resource key of this resource type with given level.
     * @param level The level of the resource. Shouldn't be negative or more than max level.
     * @return TownResourceKey of this type and given level.
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

    /**
     * Check if the given level is valid for this resource type.
     */
    default boolean isLevelValid(int level){
        return level>=0&&level<=getMaxLevel();
    }
}
