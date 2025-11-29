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

import lombok.Getter;

/**
 * <strong>THIS IS ALSO A VIRTUAL RESOURCE KEY THAT DIRECTLY STORED IN {@link TeamTownResourceHolder}!</strong>
 * <p>
 * Town resource attribute of virtual resources.
 * Holds the resource type and the level.
 * The amount of a resource with specific type and level can be read using this class.
 */
@Getter
public class VirtualResourceAttribute implements ITownResourceAttribute, ITownResourceKey {
    public final VirtualResourceType type;
    private final int level;

    /**
     * 用于缓存，避免创建重复的attribute，占用额外内存。
     */
    public static final Interner<VirtualResourceAttribute> INTERNER = com.google.common.collect.Interners.newWeakInterner();

    public static final Codec<VirtualResourceAttribute> CODEC = RecordCodecBuilder.create(t -> t.group(
                    VirtualResourceType.CODEC.fieldOf("resourceType").forGetter(o->o.type),
                    Codec.INT.optionalFieldOf("level",0).forGetter(o->o.level)
            ).apply(t, VirtualResourceAttribute::of)
    );


    private VirtualResourceAttribute(VirtualResourceType type, int level){
        this.type=type;
        if(type.isLevelValid(level)){
            this.level=level;
        } else {
            throw new IllegalArgumentException("Level "+level+" is not valid for resource "+type.getKey());
        }
    }

    private VirtualResourceAttribute(VirtualResourceType type){
        this.type=type;
        this.level = 0;
    }



    /**
     * 创建一个VirtualResourceAttribute，并使用缓存，避免重复创建占用内存
     */
    public static VirtualResourceAttribute of(VirtualResourceType type, int level) {
        return INTERNER.intern(new VirtualResourceAttribute(type, level));
    }


    /**
     * 创建一个VirtualResourceAttribute，并使用缓存，避免重复创建占用内存
     * 默认等级为0
     */
    public static VirtualResourceAttribute of(VirtualResourceType type) {
        return INTERNER.intern(new VirtualResourceAttribute(type));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o instanceof VirtualResourceAttribute otherAttribute){
            return type==otherAttribute.type&&level==otherAttribute.level;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode()*31+level;
    }

    public String toString(){
        return "{Virtual resource: " + type.getKey()+"_level" + level + "}";
    }

}
