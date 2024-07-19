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

package com.teammoeg.frostedheart.content.town.house;

import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.OccupiedArea;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.ConfinedSpaceScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.World;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//严格来讲这不是一个正常的BlockScanner，而是一个用于将FloorBlockScanner和ConfinedSpaceScanner结合起来的类
public class HouseBlockScanner extends BlockScanner {
    public static final int MAX_SCANNING_TIMES_VOLUME = 4096;
    public static final int MINIMUM_VOLUME = 6;
    public static final int MINIMUM_AREA = 3;
    public static final int MAX_SCANNING_TIMES_FLOOR = 512;
    protected int area = 0;
    protected int volume = 0;
    protected final Map<String/*block.getName()*/, Integer> decorations = new HashMap<>();
    protected double temperature = 0;//average temperature
    protected final OccupiedArea occupiedArea = new OccupiedArea();

    public int getArea() {
        return this.area;
    }

    public int getVolume() {
        return this.volume;
    }

    public Map<String, Integer> getDecorations() {
        return this.decorations;
    }

    public double getTemperature() {
        return this.temperature;
    }

    public OccupiedArea getOccupiedArea() {
        return this.occupiedArea;
    }

    public HouseBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
    }


    public static boolean isValidFloorOrLadder(World world, BlockPos pos) {
        // Determine whether the block satisfies type requirements
        if (!FloorBlockScanner.isFloorBlock(world, pos) && !world.getBlockState(pos).isIn(BlockTags.CLIMBABLE)) return false;
        AbstractMap.SimpleEntry<Integer, Boolean> information = countBlocksAbove(pos, (pos1)->FloorBlockScanner.isHouseBlock(world, pos1));
        // Determine whether the block has open air above it
        if (!information.getValue()) {
            return false;
        } else {
            // Determine whether the block has at least 2 blocks above it
            return information.getKey() >= 2;
        }
    }

    /**
     * Given a block scanned, add the block to the decorations map if it is a decoration block.
     *
     * @param pos the position of the block to check
     */
    protected void addDecoration(BlockPos pos) {
        BlockState blockState = getBlockState(pos);
        Block block = blockState.getBlock();
        if (blockState.isIn(FHTags.Blocks.DECORATIONS) || Objects.requireNonNull(block.getRegistryName()).getNamespace().equals("cfm")) {
            String name = block.toString();
            decorations.merge(name, 1, Integer::sum);
        }
    }


    /**
     * Run the house scanner.
     * 对本scanner进行一次scan扫描地板，然后新建一个blockScanner扫描空气。
     * @return whether the house is valid
     */
    public boolean scan() {//想了想还是叫scan更合适
        //第一次扫描，确定地板的位置，并判断是否有露天的地板
        FloorBlockScanner floorBlockScanner = new FloorBlockScanner(world, startPos);
        floorBlockScanner.scan(MAX_SCANNING_TIMES_FLOOR, (pos) -> {
            this.area++;
            this.occupiedArea.add(toColumnPos(pos));
            //FHMain.LOGGER.debug("HouseScanner: scanning floor pos " + pos);
        }, (pos) -> !this.isValid);
        //FHMain.LOGGER.debug("HouseScanner: first scan area: " + area);
        if (this.area < MINIMUM_AREA) this.isValid = false;
        if (!floorBlockScanner.isValid || !this.isValid) return false;
        //FHMain.LOGGER.debug("HouseScanner: first scan completed");

        //第二次扫描，判断房间是否密闭
        ConfinedSpaceScanner airScanner = new ConfinedSpaceScanner(world, startPos.up());
        airScanner.scan(MAX_SCANNING_TIMES_VOLUME, (pos) -> {//对每一个空气方块执行的操作：统计温度、统计体积、统计温度
                    this.temperature += ChunkHeatData.getTemperature(world, pos);
                    this.volume++;
                    this.occupiedArea.add(new ColumnPos(pos.getX(), pos.getZ()));
                    //FHMain.LOGGER.debug("scanning air pos:" + pos);
                }, this::addDecoration,
                (useless) -> !this.isValid);
        temperature /= volume;
        if (this.volume < MINIMUM_VOLUME) this.isValid = false;
        return this.isValid;
    }
    /*
    * 此方法尚存在缺陷，下面（横截面示意图）这种情况
    块块块块
    块块空空
    空空空空
    空空块块
    块块块块
    * 如果是玩家的话，不能从左边走到右边或者从右边走到左边（会顶头），但是blockScanner可以直接通过
    * 考虑到这种情况应该出现得很少，并且影响不大，暂且不去修它
    * */
}
