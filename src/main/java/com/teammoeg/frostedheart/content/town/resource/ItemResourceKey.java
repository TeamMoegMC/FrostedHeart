package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Used in the storage of town resource.
 * Holds the resource type and the level.
 * The amount of a resource with specific type and level can be read using this key.
 */
public class ItemResourceKey implements ITownResourceKey {
    public final ItemResourceType type;
    private int level;

    public static final Codec<ItemResourceKey> CODEC = RecordCodecBuilder.create(t -> t.group(
            ItemResourceType.CODEC.fieldOf("type").forGetter(o->o.type),
            Codec.INT.fieldOf("level").forGetter(o->o.level)
            ).apply(t, ItemResourceKey::new)
    );


    ItemResourceKey(ItemResourceType type, int level){
        this.type=type;
        if(type.isLevelValid(level)){
            this.level=level;
        } else {
            throw new IllegalArgumentException("Level "+level+" is not valid for resource "+type.getKey());
        }
    }

        ItemResourceKey(ItemResourceType type){
        this.type=type;
        this.level = 0;
    }



    //快速生成Key
    public static ItemResourceKey of(ItemResourceType type, int level) {
        return new ItemResourceKey(type, level);
    }

    public static ItemResourceKey of(ItemResourceType type) {
        return new ItemResourceKey(type);
    }

    /**
     * 从物品的Tag中获取资源类型和等级，并生成对应的ItemResourceKey
     * 若没有对应的资源类型，默认为OTHER
     * 若没有等级信息，默认为0
     */
    public static ItemResourceKey fromItemStack(ItemStack itemStack){
        AtomicReference<ItemResourceType> type = new AtomicReference<>(ItemResourceType.OTHER);
        AtomicInteger level = new AtomicInteger(0);
        itemStack.getTags()
                .map(TagKey::location)
                .filter(location -> location.getNamespace().equals(FHTags.NameSpace.MOD.id))
                .map(ResourceLocation::getPath)
                .filter(path -> path.startsWith("town_resource_"))
                .forEach(path -> {
                    if(path.startsWith("town_resource_type_")){
                        type.set(ItemResourceType.from(path.replace("town_resource_type_", "")));
                    } else if(path.startsWith("town_resource_level_")){
                        level.set(Integer.parseInt(path.replace("town_resource_level_", "")));
                    }
                });
        return new ItemResourceKey(type.get(), level.get());
    }

    public ItemResourceType getType(){
        return type;
    }

    public int getLevel() {
        return level;
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
