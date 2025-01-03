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
import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 具有对应物品的城镇资源类型，如木头、金属、食物等
 */
public enum ItemResourceType implements ITownResourceType {

    OTHER(0),
    WOOD(0),
    STONE(0),
    ORE,
    METAL,
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
     * default is 1.
     */
    @Getter
    public final int maxLevel;

    public static ItemResourceType from(String t) {
        return ItemResourceType.valueOf(t.toUpperCase());
    }

    /**
     * Create a new type
     */
    ItemResourceType() {
        this.maxLevel=1;
    }

    ItemResourceType(int maxLevel){
        this.maxLevel=maxLevel;
    }

    @Override
    public String getKey() {
        return this.name().toLowerCase();
    }

    @Override
    public boolean isLevelValid(int level){
        return level >= 0 && level <= this.maxLevel;
    }

    @Override
    public ItemResourceKey generateKey(int level) {
        return new ItemResourceKey(this, level);
    }

    /**
     * 从物品的Tag中获取资源类型，并获取对应的ItemResourceKey
     * 若没有对应的资源类型，默认为OTHER
     */
    public static ItemResourceType fromItem(ItemStack item) {
        return item.getTags()
                .map(TagKey::location)
                .filter(location -> location.getNamespace().equals(FHTags.NameSpace.MOD.id))
                .map(ResourceLocation::getPath)
                .filter(path -> path.startsWith("town_resource_type_"))
                .findFirst()
                .map(path -> ItemResourceType.from(path.replace("town_resource_type_", "")))
                .orElse(ItemResourceType.OTHER);
    }

    /**
     * 用于校正TownResourceKey的等级，确保其符合当前类型
     * @param level level might be invalid for this type
     * @return the corrected level
     */
    public int correctLevel(int level){
        return Math.max(Math.min(level, this.maxLevel), 0);
    }

}
