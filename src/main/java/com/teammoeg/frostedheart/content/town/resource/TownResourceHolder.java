package com.teammoeg.frostedheart.content.town.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 此类用于封装resources，仅提供addUnsafe和costUnsafe两个方法来修改resources，确保在修改资源数量时，occupiedCapacity也会随之修改。
 * 为保证OccupiedCapacity本身不会被意外修改，它并非作为一个service resource储存在resources中，而是以private变量的形式存在这里。
 * 此类中提供的方法都有可能产生不合法的数据(如负数的资源或超过上限的资源)，请使用TownResourceManager来修改资源数量。
 */
public class TownResourceHolder {
    /**
     * The collection that saves all the resources(include service) of town.
     * About 100000 unit is 1m^3, and 1000 unit is one item.
     * Concerned about extreme situation,stored as long.
     */
    private final Map<TownResourceKey, Double> resources = new HashMap<>();
    private long occupiedCapacity = 0;

    public static final Codec<TownResourceHolder> CODEC = RecordCodecBuilder.create(t -> t.group(
            CodecUtil.mapCodec(TownResourceKey.CODEC, Codec.LONG).fieldOf("resources").forGetter(o->o.resources),
            Codec.LONG.fieldOf("occupiedCapacity").forGetter(o->o.occupiedCapacity)
            ).apply(t, TownResourceHolder::new)
    );


    public TownResourceHolder() {}
    public TownResourceHolder(Map<TownResourceKey, Long> resources, long occupiedCapacity) {
        this.resources.putAll(resources);
        this.occupiedCapacity = occupiedCapacity;
    }

    public TownResourceHolder(Map<TownResourceKey, Long> resources) {
        this.resources.putAll(resources);
        resources.entrySet().stream()
                .filter(entry -> entry.getKey().type.needCapacity)
                .forEach(entry -> occupiedCapacity += entry.getValue());
    }

    public long get(TownResourceKey key){
        return resources.getOrDefault(key, 0L);
    }

    /**
     * Get all resources.
     * ONLY used to read all resources, CAN'T modify the map stored in this class.
     * @return
     */
    public Map<TownResourceKey, Long> getAll(){
        return Map.copyOf(resources);
    }

    public long getOccupiedCapacity(){
        return occupiedCapacity;
    }

    /**
     * Add resource to the town without checking capacity.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void addUnsafe(TownResourceKey key,long amount){
        resources.merge(key, amount, Long::sum);
        if(key.type.needCapacity){
            this.occupiedCapacity += amount;
        }
    }

    /**
     * Cost resource from the town without checking resource left.
     * If you want to change the resource, please use methods in TownResourceManager.
     */
    void costUnsafe(TownResourceKey key,long amount){
        resources.merge(key, -amount, Long::sum);
        if(key.type.needCapacity){
            this.occupiedCapacity -= amount;
        }
    }

    /**
     * Set resource.
     */
    void set(TownResourceKey key,long amount){
        if(key.type.needCapacity){
            long different = amount - get(key);
            this.addUnsafe(key, different);
        }
        else resources.put(key, amount);
    }
}
