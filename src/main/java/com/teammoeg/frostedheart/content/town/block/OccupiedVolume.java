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

package com.teammoeg.frostedheart.content.town.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.util.struct.WorldMarker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;

import java.util.*;

@Slf4j
@Getter
public class OccupiedVolume {
    public static final Codec<OccupiedVolume> CODEC = RecordCodecBuilder.create(t -> t.group(
            WorldMarker.CODEC.fieldOf("occupiedBlocks").forGetter(OccupiedVolume::getOccupiedBlocks),
            Codec.INT.fieldOf("maxX").forGetter(OccupiedVolume::getMaxX),
            Codec.INT.fieldOf("maxY").forGetter(OccupiedVolume::getMaxY),
            Codec.INT.fieldOf("maxZ").forGetter(OccupiedVolume::getMaxZ),
            Codec.INT.fieldOf("minX").forGetter(OccupiedVolume::getMinX),
            Codec.INT.fieldOf("minY").forGetter(OccupiedVolume::getMinY),
            Codec.INT.fieldOf("minZ").forGetter(OccupiedVolume::getMinZ)
    ).apply(t, OccupiedVolume::new));

    /**
     * -- GETTER --
     *  DO NOT MODIFY THIS!
     *  If you need to change occupied volume, use setOccupiedVolume/add/remove instead.
     *
     */
    private final WorldMarker occupiedBlocks;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE, minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    public static final OccupiedVolume EMPTY = new OccupiedVolume(new WorldMarker(Map.of()), 0, 0, 0, 0, 0, 0);

    public OccupiedVolume(WorldMarker occupiedBlocks, int maxX, int maxY, int maxZ, int minX, int minY, int minZ) {
        this.occupiedBlocks = occupiedBlocks;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
    }

    public OccupiedVolume(WorldMarker occupiedBlocks){
        this.occupiedBlocks = occupiedBlocks;
        occupiedBlocks.forEach(this::updateBoundingBox);
    }

    public OccupiedVolume(){
        this.occupiedBlocks = new WorldMarker(Map.of());
    }


    public void add(BlockPos pos){
        if(this.occupiedBlocks.set(pos, true)){
            updateBoundingBox(pos);
        }
    }

    public void updateBoundingBox(BlockPos pos){
        this.maxX = Math.max(this.maxX, pos.getX());
        this.maxY = Math.max(this.maxY, pos.getY());
        this.maxZ = Math.max(this.maxZ, pos.getZ());
        this.minX = Math.min(this.minX, pos.getX());
        this.minY = Math.min(this.minY, pos.getY());
        this.minZ = Math.min(this.minZ, pos.getZ());
    }

    public boolean boundingBoxIntersect(OccupiedVolume other){
        if(this == EMPTY || other == EMPTY || other == null) return false;
        return this.minX <= other.maxX && this.maxX >= other.minX && this.minY <= other.maxY && this.maxY >= other.minY && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    /**
     * 先通过外框初步判断重合可能性，后用WorldMarker判断是否真的重合
     * @param other 另一个OccupiedVolume
     * @return 两个OccupiedVolume是否包含相同的方块
     */
    public boolean intersects(OccupiedVolume other){
        if(!boundingBoxIntersect(other)){
            return false;
        }
        return occupiedBlocks.intersects(other.occupiedBlocks);
    }

    @Override
    public int hashCode(){
        return occupiedBlocks.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof OccupiedVolume){
            return occupiedBlocks.equals(((OccupiedVolume)obj).occupiedBlocks);
        }
        return false;
    }
}
