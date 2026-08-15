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

package com.teammoeg.frostedheart.content.town.buildings.mine;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MineBaseBlockEntity extends AbstractTownBuildingBlockEntity<MineBaseBuilding> implements MenuProvider {

    public MineBaseBlockEntity(BlockPos pos, BlockState state){
        super(FHBlockEntityTypes.MINE_BASE.get(),pos,state);
    }

    public boolean scanStructure(MineBaseBuilding building){
        BlockPos mineBasePos = this.getBlockPos();
        BlockPos doorPos = AbstractBlockScanner.getDoorAdjacent(level, mineBasePos);
        if (doorPos == null) return false;
        BlockPos floorBelowDoor = AbstractBlockScanner.getBlockBelow(Objects.requireNonNull(level), (pos)->!(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
        for (Direction direction : AbstractBlockScanner.PLANE_DIRECTIONS) {
            assert floorBelowDoor != null;
            BlockPos startPos = floorBelowDoor.relative(direction);//找到门下方块旁边的方块
            if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isBuildingBlock(level, startPos.above(2))) {//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                    continue;
                }
                startPos = startPos.below();
            }
            MineBaseBlockScanner scanner = new MineBaseBlockScanner(level, startPos);
            if(scanner.scan()){
                building.setArea(scanner.getArea());
                building.setVolume(scanner.getVolume());
                //this.rack = scanner.getRack();
                //this.chest = scanner.getChest();
                building.setOccupiedVolume(scanner.getOccupiedVolume());
                FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
                double effectiveFloorBlocks = TownMathFunctions.calculateSpaceRating(
                        scanner.getVolume(),
                        scanner.getArea(),
                        scoring.spaceAreaCoefficient.get(),
                        scoring.spaceHeightLogCoefficient.get(),
                        scoring.spaceHeightLogOffset.get(),
                        scoring.spaceResponseScale.get(),
                        scoring.spaceResponseExponent.get())
                        * scanner.getArea();
                int calculated = (int) (effectiveFloorBlocks
                        / FHConfig.SERVER.TOWN.MINING.floorBlocksPerWorkerSlot.get());
                building.setMaxResidents(Math.max(
                        FHConfig.SERVER.TOWN.MINING.minimumWorkerSlots.get(),
                        calculated
                ));
                return true;
            }
        }
        return false;
    }

/*
    @Override
    public CompoundTag getWorkData() {
        CompoundTag nbt = getBasicWorkData();
        if(this.isValid()) {
            ListTag list = new ListTag();
            for (BlockPos pos : this.linkedMines) {
                list.add(LongTag.valueOf(pos.asLong()));
            }
            nbt.put("linkedMines", list);
            nbt.putDouble("rating", this.rating);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundTag data) {
        setBasicWorkData(data);
    }*/


    @Override
    public @Nullable MineBaseBuilding getBuilding(AbstractTownBuilding abstractTownBuilding) {
        if(abstractTownBuilding instanceof MineBaseBuilding){
            return (MineBaseBuilding) abstractTownBuilding;
        }
        return null;
    }

    @Override
    public @NotNull MineBaseBuilding createBuilding() {
        return new MineBaseBuilding(this.getBlockPos());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new MineBaseMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.mine_base");
    }
}
