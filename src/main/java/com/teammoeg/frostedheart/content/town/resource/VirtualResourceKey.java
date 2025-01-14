package com.teammoeg.frostedheart.content.town.resource;

import com.google.common.collect.Interner;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

/**
 * Town resource key of Items.
 * Holds the resource type and the level.
 * The amount of a resource with specific type and level can be read using this key.
*/
@Getter
public class VirtualResourceKey implements ITownResourceKey{
    public final VirtualResourceType type;
    private final int level;

    /**
     * 用于缓存，避免创建重复的key，占用额外内存。
     */
    public static final Interner<VirtualResourceKey> INTERNER = com.google.common.collect.Interners.newWeakInterner();

    public static final Codec<VirtualResourceKey> CODEC = RecordCodecBuilder.create(t -> t.group(
                    VirtualResourceType.CODEC.fieldOf("type").forGetter(o->o.type),
                    Codec.INT.fieldOf("level").forGetter(o->o.level)
            ).apply(t, VirtualResourceKey::new)
    );


    VirtualResourceKey(VirtualResourceType type, int level){
        this.type=type;
        if(type.isLevelValid(level)){
            this.level=level;
        } else {
            throw new IllegalArgumentException("Level "+level+" is not valid for resource "+type.getKey());
        }
    }

    VirtualResourceKey(VirtualResourceType type){
        this.type=type;
        this.level = 0;
    }



    /**
     * 创建一个VirtualResourceKey，并使用缓存，避免重复创建占用内存
     */
    public static VirtualResourceKey of(VirtualResourceType type, int level) {
        return INTERNER.intern(new VirtualResourceKey(type, level));
    }


    /**
     * 创建一个VirtualResourceKey，并使用缓存，避免重复创建占用内存
     * 默认等级为0
     */
    public static VirtualResourceKey of(VirtualResourceType type) {
        return INTERNER.intern(new VirtualResourceKey(type));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o instanceof VirtualResourceKey otherKey){
            return type==otherKey.type&&level==otherKey.level;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode()*31+level;
    }

    public String toString(){
        return type.getKey()+"_level:"+level;
    }

}
