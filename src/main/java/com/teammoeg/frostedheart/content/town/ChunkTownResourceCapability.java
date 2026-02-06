/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.DoubleSupplier;

import com.teammoeg.chorda.io.NBTSerializable;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

/**
 * 用于存储一个区块中含有的自然资源量。主要用于存储矿物等不可再生自然资源。
 */
public class ChunkTownResourceCapability implements NBTSerializable {
    /**
     * 区块中各种资源储量。
     * 键为资源类型，值为该资源在区块中的储量。
     * 值的范围在0-1之间
     */
    private final Map<ChunkTownResourceType, Double> resourceReserves;
    private static final HashMap<String, ChunkTownResourceType> CHUNK_RESOURCE_TYPE_KEY = new HashMap<>();
    static{
        for(ChunkTownResourceType ChunkTownResourceType : ChunkTownResourceType.values()){
            CHUNK_RESOURCE_TYPE_KEY.put(ChunkTownResourceType.getKey(), ChunkTownResourceType);
        }
    }

    public ChunkTownResourceCapability(){
        this.resourceReserves = new EnumMap<>(ChunkTownResourceType.class);
    }

    public double getOrGenerateReserves(ChunkTownResourceType resourceType){
        if(resourceReserves.get(resourceType) == null){
            resourceReserves.put(resourceType, resourceType.reservesGenerator.getAsDouble());
        }
        return resourceReserves.get(resourceType);
    }

    public void costReserves(ChunkTownResourceType resourceType, double amount){
        if(amount <= this.getOrGenerateReserves(resourceType)) {
            resourceReserves.put(resourceType, resourceReserves.get(resourceType) - amount);
        }
    }

    public ChunkTownResourceType getChunkTownResourceType(String key){
        return CHUNK_RESOURCE_TYPE_KEY.get(key);
    }

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        for(Map.Entry<ChunkTownResourceType, Double> abundanceEntry : resourceReserves.entrySet()){
            if(abundanceEntry.getValue()!=null) {
                nbt.putDouble(abundanceEntry.getKey().getKey(), abundanceEntry.getValue());
            }
        }
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        for(String key : CHUNK_RESOURCE_TYPE_KEY.keySet()){
            this.resourceReserves.put(getChunkTownResourceType(key), nbt.getDouble(key) >=0 ? null : nbt.getDouble(key));
        }
    }

    /**
     * 表示区块中，对应某种城镇工作的自然资源。
     */
    @Getter
    public enum ChunkTownResourceType {
        ORE( ()->{
            Random random = new Random();
            double randomNum = random.nextDouble();
            if(randomNum > 0.5){
                return 0;//有概率没有矿物
            }
            return Math.pow(randomNum * 2, 0.75);//随便填的
        } ),//用于矿场
        ANIMAL(),//用于猎场
        TREE( () -> {
            Random random = new Random();
            return 0.5 + random.nextDouble() * 0.5;//群系正确的情况下始终不太低
        });//用于伐木场（如果以后有的话）

        /**
         * 用于生成资源的初始相对储量，范围在0-1之间。
         */
        final java.util.function.DoubleSupplier reservesGenerator;

        ChunkTownResourceType(){
            this.reservesGenerator = Math::random;
        }
        ChunkTownResourceType(DoubleSupplier reservesGenerator){
            this.reservesGenerator = reservesGenerator;
        }

        public String getKey(){
            return this.name().toLowerCase();
        }

    }
}
