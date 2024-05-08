package com.teammoeg.frostedheart.content.town;

import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static net.minecraftforge.common.util.Constants.NBT.TAG_INT;
import static net.minecraftforge.common.util.Constants.NBT.TAG_LONG;

public class OccupiedArea {
    private final Set<ColumnPos> occupiedArea;
    private int maxX, maxZ, minX, minZ;
    public static final OccupiedArea EMPTY = new OccupiedArea(null);

    public OccupiedArea(Set<ColumnPos> occupiedArea) {
        this.occupiedArea = occupiedArea;
        if(occupiedArea == null || occupiedArea.isEmpty()) return;
        for (ColumnPos pos : occupiedArea) {
            if (pos.x > maxX) maxX = pos.x;
            if (pos.x < minX) minX = pos.x;
            if (pos.z > maxZ) maxZ = pos.z;
            if (pos.z < minZ) minZ = pos.z;
        }
        this.calculateMaxXandZ();
    }
    public OccupiedArea(){
        this.occupiedArea = new HashSet<>();
    }

    /**
     * the max coordinates you input might be not true.
     */
    @Deprecated
    private OccupiedArea(Set<ColumnPos> occupiedArea, int maxX, int maxZ, int minX, int minZ){
        this.occupiedArea = occupiedArea;
    }

    public int getMaxX() {
        return maxX;
    }
    public int getMaxZ() {
        return maxZ;
    }
    public int getMinX() {
        return minX;
    }
    public int getMinZ() {
        return minZ;
    }
    public OccupiedArea getEmpty(){
        return EMPTY;
    }

    /**
     * calculate maxX and maxZ.
     * 计算结果会保存在maxX和maxZ字段中
     */
    public void calculateMaxXandZ(){
        if(occupiedArea.isEmpty()) return;
        Iterator<ColumnPos> iterator = occupiedArea.iterator();
        ColumnPos pos = iterator.next();
        maxX = minX = pos.x;
        maxZ = minZ = pos.z;
        while (iterator.hasNext()) {
            pos = iterator.next();
            updateMaxXandZ(pos);
        }
    }

    public void updateMaxXandZ(ColumnPos pos){
        if (pos.x > maxX) maxX = pos.x;
        if (pos.z > maxZ) maxZ = pos.z;
        if (pos.x < minX) minX = pos.x;
        if (pos.z < minZ) minZ = pos.z;
    }

    public void add(ColumnPos pos){
        if(this.occupiedArea.add(pos)){
            updateMaxXandZ(pos);
        }
    }

    public void remove(ColumnPos pos){
        if(this.occupiedArea.remove(pos)){
            if(pos.x == maxX || pos.z == maxZ || pos.x == minX || pos.z == minZ){
                calculateMaxXandZ();
            }
        }
    }

    public void setOccupiedArea(Set<ColumnPos> occupiedArea){
        this.occupiedArea.clear();
        this.occupiedArea.addAll(occupiedArea);
        calculateMaxXandZ();
    }

    /**
     * DO NOT MODIFY THIS SET!
     * If you need to change occupied area, use setOccupiedArea/add/remove instead.
     * @return occupied area
     */
    public Set<ColumnPos> getOccupiedArea() {
        //return copyCollection(occupiedArea);
        return occupiedArea;
    }

    public boolean doRectanglesIntersect(OccupiedArea other){
        if(other == EMPTY) return true;
        return this.maxX >= other.minX && this.minX <= other.maxX && this.maxZ >= other.minZ && this.minZ <= other.maxZ;
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


    public CompoundNBT toNBT(){
        CompoundNBT nbt = new CompoundNBT();
        ListNBT list = new ListNBT();
        for (ColumnPos pos : occupiedArea) {
            list.add(LongNBT.valueOf(BlockPos.pack(pos.x, 0, pos.z)));
        }
        nbt.put("occupiedAreaList", list);
        list.clear();
        list.add(IntNBT.valueOf(maxX));
        list.add(IntNBT.valueOf(maxZ));
        list.add(IntNBT.valueOf(minX));
        list.add(IntNBT.valueOf(minZ));
        nbt.put("maxOccupiedCoordinates", list);
        return nbt;
    }

    public static OccupiedArea fromNBT(CompoundNBT nbt){
        Set<ColumnPos> occupiedArea = new HashSet<>();
        ListNBT list = nbt.getList("occupiedAreaList", TAG_LONG);
        list.forEach(nbt1 -> {
            occupiedArea.add(new ColumnPos(BlockPos.unpackX(((LongNBT) nbt1).getLong()), BlockPos.unpackZ(((LongNBT) nbt1).getLong())));
        });
        list = nbt.getList("maxOccupiedCoordinates", TAG_INT);
        Iterator<INBT> iterator = list.iterator();
        int maxX = ((IntNBT) (iterator.next()) ).getInt();
        int maxZ = ((IntNBT) (iterator.next()) ).getInt();
        int minX = ((IntNBT) (iterator.next()) ).getInt();
        int minZ = ((IntNBT) (iterator.next()) ).getInt();
        return new OccupiedArea(occupiedArea, maxX, maxZ, minX, minZ);
    }
}
