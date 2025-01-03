package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.util.io.CodecUtil;

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

    default boolean isLevelValid(int level){
        return level>=0&&level<=getMaxLevel();
    }
}
