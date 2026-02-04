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

package com.teammoeg.frostedheart.content.town.buildings.hunting;

import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBlockScanner;
import com.teammoeg.frostedheart.content.town.blockscanner.ConfinedSpaceScanner;
import com.teammoeg.frostedheart.content.town.blockscanner.FloorBlockScanner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

import java.util.Objects;

public class HuntingBaseBlockScanner extends HouseBlockScanner {
    private int bedNum = 0;
    private int chestNum = 0;
    private int tanningRackNum = 0;


    public HuntingBaseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }

    public int getBedNum() {
        return bedNum;
    }

    public int getChestNum() {
        return chestNum;
    }

    public int getTanningRackNum() {
        return tanningRackNum;
    }

    protected void addSpecialBlock(BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        addDecoration(pos);
        if(blockState.is(BlockTags.BEDS)) bedNum++;
        if(blockState.is(Tags.Blocks.CHESTS)) chestNum++;
        if(Objects.requireNonNull(CRegistryHelper.getRegistryName(blockState.getBlock())).getPath().equals("tanning_rack") || blockState.getBlock() instanceof CommandBlock) tanningRackNum++;
    }

    @Override
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
        ConfinedSpaceScanner airScanner = new ConfinedSpaceScanner(world, startPos.above());
        airScanner.scan(MAX_SCANNING_TIMES_VOLUME, (pos) -> {//对每一个空气方块执行的操作：统计温度、统计体积、统计温度
                    this.temperature += WorldTemperature.block(world, pos);
                    this.volume++;
                    this.occupiedArea.add(new ColumnPos(pos.getX(), pos.getZ()));
                    //FHMain.LOGGER.debug("scanning air pos:" + pos);
                }, this::addSpecialBlock,
                (useless) -> !this.isValid);
        temperature /= volume;
        if (this.volume < MINIMUM_VOLUME) this.isValid = false;
        return this.isValid;
    }
}
