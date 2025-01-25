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

import com.google.common.collect.Interner;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import lombok.Getter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Town resource key of Items.
 * Holds the resource type and the level.
 * The amount of a resource with specific type and level can be read using this key.

 * 每一个可能存在的ItemResourceKey（每一个ItemResourceType的所有合法等级对应的所有ItemResourceKey），都有一个对应的TagKey，在FHTags.Items中自动生成。
 * 生成的TagKey，在FHTags中存入了两个Map，用于在TagKey和ItemResourceKey之间快速转换。
 */
@Getter
public class ItemResourceKey implements ITownResourceKey {
    public final ItemResourceType type;
    private final int level;

    /**
     * 用于缓存，避免创建重复的key，占用额外内存。
     */
    private static final Interner<ItemResourceKey> INTERNER = com.google.common.collect.Interners.newWeakInterner();

    public static final Codec<ItemResourceKey> CODEC = RecordCodecBuilder.create(t -> t.group(
            ItemResourceType.CODEC.fieldOf("type").forGetter(o->o.type),
            Codec.INT.fieldOf("level").forGetter(o->o.level)
            ).apply(t, ItemResourceKey::new)
    );


    private ItemResourceKey(ItemResourceType type, int level){
        this.type=type;
        if(type.isLevelValid(level)){
            this.level=level;
        } else {
            throw new IllegalArgumentException("Level "+level+" is not valid for resource "+type.getKey());
        }
    }

    /**
     * 创建一个ItemResourceKey，默认等级为0
     */
    private ItemResourceKey(ItemResourceType type){
        this.type=type;
        this.level = 0;
    }


    /**
     * 创建一个ItemResourceKey，并使用缓存，避免重复创建占用内存
     */
    public static ItemResourceKey of(ItemResourceType type, int level) {
        return INTERNER.intern(new ItemResourceKey(type, level));
    }

    /**
     * 创建一个ItemResourceKey，并使用缓存，避免重复创建占用内存
     * 默认等级为0
     */
    public static ItemResourceKey of(ItemResourceType type) {
        return new ItemResourceKey(type);
    }

    /**
     * 读取物品的tag，并获取该物品所有的ItemResourceKey
     */
    public static List<ItemResourceKey> fromItemStack(ItemStack itemStack){
        return itemStack.getTags()
                .filter(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::containsKey)
                .map(FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY::get)
                .toList();
    }

    /**
     * 将ItemResourceKey转换为对应的TagKey
     * @return 该ItemResourceKey对应的tagKey
     */
    public TagKey<Item> toTagKey(){
        return FHTags.Items.MAP_TOWN_RESOURCE_KEY_TO_TAG.get(this);
    }

    /**
     * 将tagKey转换为对应的ItemResourceKey
     * @param tagKey 具有对应ItemResourceKey的tagKey
     */
    public static ItemResourceKey fromTagKey(TagKey<Item> tagKey){
        return FHTags.Items.MAP_TAG_TO_TOWN_RESOURCE_KEY.get(tagKey);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o instanceof ItemResourceKey otherKey){
            return type==otherKey.type&&level==otherKey.level;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode()*31+level;
    }

}
