package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

@Getter
public class VirtualResourceKey implements ITownResourceKey{
    public final VirtualResourceType type;
    private final int level;

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



    //快速生成Key
    public static VirtualResourceKey of(VirtualResourceType type, int level) {
        return new VirtualResourceKey(type, level);
    }


    public static VirtualResourceKey of(VirtualResourceType type) {
        return new VirtualResourceKey(type);
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
