/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.util.io.CodecUtil;

public enum TownResourceType {
    /**
     * Max storage of town.
     * About 100000 unit is 1m^3, and 1000 unit is one item.
     */
    MAX_CAPACITY(new TownResourceTypeBuilder().isService()),
    WOOD(new TownResourceTypeBuilder().noLevel()),
    STONE(new TownResourceTypeBuilder().noLevel()),
    ORE,
    METAL,
    TOOL,
    FOOD_PROTEIN,
    FOOD_FRUIT_AND_VEGETABLES,
    FOOD_EDIBLE_OIL,
    FOOD_GRAINS;

	public static final Codec<TownResourceType> CODEC=CodecUtil.enumCodec(TownResourceType.class);

    /**
     * the largest level this type of resource can be.
     * 0: this type doesn't have level
     * 1: the lowest max level if this type has mre than level
     * default is 1.
     */
    public final int maxLevel;
    public final boolean needCapacity;
    public final boolean isService;

    public static TownResourceType from(String t) {
        return TownResourceType.valueOf(t.toUpperCase());
    }

    /**
     * Create a new type
     */
    TownResourceType() {
        this.maxLevel=1;
        this.needCapacity = true;
        this.isService = false;
    }

    TownResourceType(int maxLevel, boolean needCapacity, boolean isService){
        this.maxLevel=maxLevel;
        this.needCapacity = needCapacity;
        this.isService = isService;
    }

    TownResourceType(TownResourceTypeBuilder builder){
        this.maxLevel=builder.maxLevel;
        this.needCapacity =builder.needCapacity;
        this.isService = builder.isService;
    }

    public String getKey() {
        return this.name().toLowerCase();
    }

    public boolean isLevelValid(int level){
        return level >= 0 && level <= this.maxLevel;
    }

    /**
     * 用于校正TownResourceKey的等级，确保其符合当前类型
     * @param level level might be invalid for this type
     * @return the corrected level
     */
    public int correctLevel(int level){
        return Math.max(Math.min(level, this.maxLevel), 0);
    }

    public boolean noSize(){
        return !this.needCapacity;
    }


    static class TownResourceTypeBuilder{
        int maxLevel = 1;
        boolean needCapacity = true;
        boolean isService = false;

        public void init(){
            maxLevel = 1;
            needCapacity = true;
        }

        public void noSpace(){
            needCapacity = false;
        }

        public TownResourceTypeBuilder noLevel(){
            return maxLevel(0);
        }

        public TownResourceTypeBuilder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public TownResourceTypeBuilder isService(){
            this.maxLevel = 0;
            this.needCapacity = false;
            this.isService = true;
            return this;
        }
    }
    
}
