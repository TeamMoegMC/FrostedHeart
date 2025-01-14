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
import lombok.Getter;

/**
 * 具有对应物品的城镇资源类型，如木头、金属、食物等
 */
@Getter
public enum ItemResourceType implements ITownResourceType {

    OTHER,
    WOOD,
    STONE,
    ORE,
    METAL(1),
    FUEL,
    TOOL,
    FOOD_PROTEIN,
    FOOD_FRUIT_AND_VEGETABLES,
    FOOD_EDIBLE_OIL,
    FOOD_GRAINS;

	public static final Codec<ItemResourceType> CODEC=CodecUtil.enumCodec(ItemResourceType.class);

    /**
     * the largest level this type of resource can be.
     * 0: this type doesn't have level
     * if maxLevel is n, the level range is [0,n]
     * default is 0.
     */
    public final int maxLevel;

    /**
     * 根据字符串获取ItemResourceType。大小写均可。
     * 如果字符串不对应任何一个枚举值的名字，可能会引发IllegalArgumentException。
     * @param stringOfType 对应此枚举类中某个字段的字符串
     * @return 对应的ItemResourceType
     */
    public static ItemResourceType from(String stringOfType) {
        return ItemResourceType.valueOf(stringOfType.toUpperCase());
    }

    /**
     * Create a new type
     */
    ItemResourceType() {
        this.maxLevel=0;
    }

    ItemResourceType(int maxLevel){
        this.maxLevel=maxLevel;
    }

    /**
     * 生成这个ItemResourceType的小写字符串。
     * 并非ItemResourceKey.
     * @return 该ItemResourceType名字的小写字符串。
     */
    @Override
    public String getKey() {
        return this.name().toLowerCase();
    }

    /**
     * 判断给定的等级是否是合法的，即是否在[0,maxLevel]之间。
     * @param level 给定的等级
     * @return 等级是否合法
     */
    @Override
    public boolean isLevelValid(int level){
        return level >= 0 && level <= this.maxLevel;
    }

    /**
     * 生成该type为ItemResourceType，level为传入值的ItemResourceKey。
     * @param level The level of the resource. Shouldn't be negative or more than max level.
     * @return The generated ItemResourceKey.
     */
    @Override
    public ItemResourceKey generateKey(int level) {
        return ItemResourceKey.of(this, level);
    }
}
