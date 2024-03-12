package com.teammoeg.frostedheart.content.town;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.teammoeg.frostedheart.util.io.NBTSerializable;

import net.minecraft.nbt.CompoundNBT;

/**
 * 用于存储一个区块中含有的自然资源量。主要用于存储矿物等不可再生自然资源。
 */
public class ChunkTownResourceCapability implements NBTSerializable {
    //Integer: 表示某种资源在这个区块的相对丰富程度，最大为10。
    public final Map<TownResourceType, Integer> resourceAbundance;
    public static final HashMap<String, TownResourceType> TOWN_RESOURCE_TYPE_KEY = new HashMap<>();
    static{
        for(TownResourceType townResourceType : TownResourceType.values()){
            TOWN_RESOURCE_TYPE_KEY.put(townResourceType.getKey(), townResourceType);
        }
    }

    public ChunkTownResourceCapability(){
        this.resourceAbundance = new EnumMap<>(TownResourceType.class);
        resourceAbundance.put(TownResourceType.STONE, 10);
    }

    public int getOrGenerateAbundance(TownResourceType resourceType){
        if(resourceAbundance.get(resourceType) == null || resourceAbundance.get(resourceType) < 0){
            resourceAbundance.put(resourceType, new Random().nextInt());
        } else if(resourceAbundance.get(resourceType) > 10) {
            resourceAbundance.put(resourceType, 10);
        }
        return resourceAbundance.get(resourceType);
    }

    public TownResourceType getTownResourceType(String key){
        return TOWN_RESOURCE_TYPE_KEY.get(key);
    }

    @Override
    public void save(CompoundNBT nbt, boolean isPacket) {
        for(Map.Entry<TownResourceType, Integer> abundanceEntry : resourceAbundance.entrySet()){
            if(abundanceEntry.getValue()!=null) {
                nbt.putInt(abundanceEntry.getKey().getKey(), abundanceEntry.getValue());
            }
        }
    }

    @Override
    public void load(CompoundNBT nbt, boolean isPacket) {
        for(String key : TOWN_RESOURCE_TYPE_KEY.keySet()){
            this.resourceAbundance.put(getTownResourceType(key), nbt.getInt(key) >=0 && nbt.getInt(key)<=10 ? null : nbt.getInt(key));
        }
    }
}
