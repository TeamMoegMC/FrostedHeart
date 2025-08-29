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

package com.teammoeg.frostedheart.content.town;

import lombok.Getter;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Getter
public class OccupiedArea {
    /**
     * -- GETTER --
     *  DO NOT MODIFY THIS SET!
     *  If you need to change occupied area, use setOccupiedArea/add/remove instead.
     *
     */
    private final Set<ColumnPos> occupiedArea;
    private int maxX, maxZ, minX, minZ;
    public static final OccupiedArea EMPTY = new OccupiedArea(Set.of());

    public OccupiedArea(Set<ColumnPos> occupiedArea) {
        this.occupiedArea = occupiedArea;
        if(occupiedArea == null || occupiedArea.isEmpty()) return;
        for (ColumnPos pos : occupiedArea) {
            if (pos.x() > maxX) maxX = pos.x();
            if (pos.x() < minX) minX = pos.x();
            if (pos.z() > maxZ) maxZ = pos.z();
            if (pos.z() < minZ) minZ = pos.z();
        }
        this.calculateMaxXandZ();
    }
    public OccupiedArea(){
        this.occupiedArea = new HashSet<>();
        this.maxX = 0;
        this.maxZ = 0;
        this.minX = 0;
        this.minZ = 0;
    }

    /**
     * <b>WARNING</b>! Make sure the max coordinates you input be correct.
     */
    private OccupiedArea(Set<ColumnPos> occupiedArea, int maxX, int maxZ, int minX, int minZ){
        this.occupiedArea = occupiedArea;
    }

    public OccupiedArea getEmpty(){
        return EMPTY;
    }

    /**
     * calculate maxX and maxZ.
     * 计算结果会保存在maxX和maxZ字段中
     */
    public void calculateMaxXandZ(){
        if(occupiedArea.isEmpty()) {
            this.maxX = 0;
            this.maxZ = 0;
            this.minX = 0;
            this.minZ = 0;
        }
        Iterator<ColumnPos> iterator = occupiedArea.iterator();
        ColumnPos pos = iterator.next();
        maxX = minX = pos.x();
        maxZ = minZ = pos.z();
        while (iterator.hasNext()) {
            pos = iterator.next();
            updateMaxXandZ(pos);
        }
    }

    public void updateMaxXandZ(ColumnPos pos){
        if (pos.x() > maxX) maxX = pos.x();
        if (pos.z() > maxZ) maxZ = pos.z();
        if (pos.x() < minX) minX = pos.x();
        if (pos.z() < minZ) minZ = pos.z();
    }

    public void add(ColumnPos pos){
        if(this.occupiedArea.add(pos)){
            updateMaxXandZ(pos);
        }
    }

    public void remove(ColumnPos pos){
        if(this.occupiedArea.remove(pos)){
            if(pos.x() == maxX || pos.z() == maxZ || pos.x() == minX || pos.z() == minZ){
                calculateMaxXandZ();
            }
        }
    }

    public void setOccupiedArea(Set<ColumnPos> occupiedArea){
        this.occupiedArea.clear();
        this.occupiedArea.addAll(occupiedArea);
        calculateMaxXandZ();
    }

    public boolean boundingRectangleIntersect(OccupiedArea other){
        if(this == EMPTY || other == EMPTY || other == null) return true;
        return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ;
    }

    public boolean overlapWith(OccupiedArea other){
        if(!boundingRectangleIntersect(other)){
            return false;
        }
        for (ColumnPos pos : this.occupiedArea) {
            if(other.occupiedArea.contains(pos)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode(){
        return occupiedArea.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof OccupiedArea){
            return occupiedArea.equals(((OccupiedArea)obj).occupiedArea);
        }
        return false;
    }


    public CompoundTag toNBT(){
        CompoundTag nbt = new CompoundTag();
        ListTag list = new ListTag();
        for (ColumnPos pos : occupiedArea) {
            list.add(LongTag.valueOf(BlockPos.asLong(pos.x(), 0, pos.z())));
        }
        nbt.put("occupiedAreaList", list);
        list.clear();
        list.add(IntTag.valueOf(maxX));
        list.add(IntTag.valueOf(maxZ));
        list.add(IntTag.valueOf(minX));
        list.add(IntTag.valueOf(minZ));
        nbt.put("maxOccupiedCoordinates", list);
        return nbt;
    }

    public static OccupiedArea fromNBT(CompoundTag nbt){
        Set<ColumnPos> occupiedArea = new HashSet<>();
        ListTag list = nbt.getList("occupiedAreaList", Tag.TAG_LONG);
        if(list.isEmpty()){
            return EMPTY;
        }
        list.forEach(nbt1 -> {
            occupiedArea.add(new ColumnPos(BlockPos.getX(((LongTag) nbt1).getAsLong()), BlockPos.getZ(((LongTag) nbt1).getAsLong())));
        });
        list = nbt.getList("maxOccupiedCoordinates", Tag.TAG_INT);
        Iterator<Tag> iterator = list.iterator();
        int maxX = ((IntTag) (iterator.next()) ).getAsInt();
        int maxZ = ((IntTag) (iterator.next()) ).getAsInt();
        int minX = ((IntTag) (iterator.next()) ).getAsInt();
        int minZ = ((IntTag) (iterator.next()) ).getAsInt();
        return new OccupiedArea(occupiedArea, maxX, maxZ, minX, minZ);
    }
}
