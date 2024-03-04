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

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatConsumerEndpoint;
import com.teammoeg.frostedheart.content.town.TownTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.BlockScanner;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

import static com.teammoeg.frostedheart.content.town.house.HouseBlockScanner.*;

import java.util.*;

import static com.teammoeg.frostedheart.town.house.FloorBlockScanner.isHouseBlock;

/**
 * A house in the town.
 *
 * Functionality:
 * - Provide a place for residents to live
 * - (Optional) Consume heat to add temperature based on the heat level
 * - Consume resources to maintain the house
 * - Check if the house structure is valid
 * - Compute comfort rating based on the house structure
 */
public class HouseTileEntity extends FHBaseTileEntity implements TownTileEntity, ITickableTileEntity,
        FHBlockInterfaces.IActiveState {

    /** The temperature at which the house is comfortable. */
    private static final double COMFORTABLE_TEMP_HOUSE = 24;
    public static final int MAX_TEMP_HOUSE = 50;
    public static final int MIN_TEMP_HOUSE = 0;

    /** Work data, stored in town. */
    public int size = 0; // how many resident can live here
    public List<Resident> residents = new ArrayList<>();
    public int volume = 0;
    public int decoration = 0;
    public int area = 0;
    public double temperature = 0;
    public Map<String, Integer> decorations = new HashMap<>();
    public double rating = 0;
    public Set<AbstractMap.SimpleEntry<Integer, Integer>> occupiedArea;
    public double temperatureModifier = 0;

    /** Tile data, stored in tile entity. */
    HeatConsumerEndpoint endpoint = new HeatConsumerEndpoint(10,1);

    public HouseTileEntity() {
        super(FHTileTypes.HOUSE.get());
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.HOUSE;
    }

    /**
     * Check if work environment is valid.
     * <p>
     * For the house, this implies whether the house would accommodate the residents,
     * consume resources, and other.
     * <p>
     * Room structure should be valid.
     * Temperature should be within a reasonable range.
     */
    @Override
    public boolean isWorkValid() {
        return isStructureValid() && isTemperatureValid();
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT data = new CompoundNBT();
        ListNBT residentList = new ListNBT();
        for (Resident resident : residents) {
            residentList.add(resident.serialize());
        }
        data.put("residents", residentList);
        data.putInt("size", size);
        data.putDouble("temperature", temperature);
        data.putInt("volume", volume);
        data.putInt("decoration", decoration);
        data.putDouble("rating", rating);
        data.putDouble("temperatureModifier", temperatureModifier);
        return data;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        residents = new ArrayList<>();
        ListNBT residentList = data.getList("residents", 10);
        for (int i = 0; i < residentList.size(); i++) {
            residents.add(new Resident().deserialize(residentList.getCompound(i)));
        }
        size = data.getInt("size");
        temperature = data.getDouble("temperature");
        volume = data.getInt("volume");
        decoration = data.getInt("decoration");
        rating = data.getDouble("rating");
        temperatureModifier = data.getDouble("temperatureModifier");
    }

    /**
     * Determine whether the house structure is well-defined.
     * <p>
     * Check room insulation
     * Check minimum volume
     * Check within generator range (or just check steam connection instead?)
     * <p>
     *
     * @return whether the house structure is valid
     */
    public boolean isStructureValid() {
        BlockPos housePos = this.getPos();
        Set<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos) -> Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS));
        if (doorPosSet.isEmpty()) return false;
        for (BlockPos doorPos : doorPosSet) {
            BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
            for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
                //FHMain.LOGGER.debug("HouseScanner: creating new HouseBlockScanner");
                assert floorBelowDoor != null;
                BlockPos startPos = floorBelowDoor.offset(direction);//找到门下方块旁边的方块
                //FHMain.LOGGER.debug("HouseScanner: start pos 1" + startPos);
                if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                    if(!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos.down()) || isHouseBlock(world, startPos.up(2))){//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                        continue;
                    }
                    startPos = startPos.down();
                    //FHMain.LOGGER.debug("HouseScanner: start pos 2" + startPos);
                }
                HouseBlockScanner scanner = new HouseBlockScanner(this.world, startPos);
                if (scanner.scan()) {
                    //FHMain.LOGGER.debug("HouseScanner: scan successful");
                    this.volume = scanner.getVolume();
                    this.area = scanner.getArea();
                    this.decorations = scanner.getDecorations();
                    this.temperature = scanner.getTemperature();
                    this.occupiedArea = scanner.getOccupiedArea();
                    this.rating = computeRating();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine whether the house temperature is valid for work.
     *
     * If connected to heat network, this always returns true.
     *
     * @return whether the temperature is valid
     */
    public boolean isTemperatureValid() {
        double effective = temperature + temperatureModifier;
        return effective >= MIN_TEMP_HOUSE && effective <= MAX_TEMP_HOUSE;
    }

    public double getEffectiveTemperature() {
        return temperature + temperatureModifier;
    }

    /**
     * Get a comfort rating based on how the house is built.
     * <p>
     * This would affect the mood of the residents on the next day.
     *
     * @return a rating in range of zero to one
     */
    private double computeRating() {
        return (calculateDecorationRating(this.decorations, this.area)
                + calculateSpaceRating(this.volume, this.area)
                + calculateTemperatureRating(this.temperature + this.temperatureModifier)) / 3;
    }

    private static double calculateTemperatureRating(double temperature) {
        double tempDiff = Math.abs(COMFORTABLE_TEMP_HOUSE - temperature);
        return 0.017 + 1 / (1 + Math.exp(0.4 * (tempDiff - 10)));
    }

    private static double calculateDecorationRating(Map<?, Integer> decorations, int area) {
        double score = 0;
        for (Integer num : decorations.values()) {
            if (num + 0.32 > 0) { // Ensure the argument for log is positive
                score += Math.log(num + 0.32) * 1.75 + 0.9;
            } else {
                // Handle the case where num + 0.32 <= 0
                // For example, you could add a minimal score or skip adding to the score.
                score += 0; // Or some other handling logic
            }
        }
        return Math.min(1, score / (6 + area / 16.0f));
    }


    private static double calculateSpaceRating(int volume, int area) {
        double height = volume / (float) area;
        double score = area * (1.55 + Math.log(height - 1.6) * 0.6);
        return 1 - Math.exp(-0.024 * Math.pow(score, 1.11));
    }

    @Override
    public void tick() {
        if (!world.isRemote) {
            if (endpoint.tryDrainHeat(1)) {
                temperatureModifier = Math.max(endpoint.getTemperatureLevel() * 10, COMFORTABLE_TEMP_HOUSE);
                if (setActive(true)) {
                    markDirty();
                }
            } else {
                temperatureModifier = 0;
                if (setActive(false)) {
                    markDirty();
                }
            }
        } else if (getIsActive()) {
            ClientUtils.spawnSteamParticles(world, pos);
        }
    }

    @Override
    public void readCustomNBT(CompoundNBT compoundNBT, boolean isPacket) {
        endpoint.load(compoundNBT, isPacket);
    }

    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean isPacket) {
        endpoint.save(compoundNBT, isPacket);
    }

    LazyOptional<HeatConsumerEndpoint> endpointCap = LazyOptional.of(()-> endpoint);
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if(capability== FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
            return endpointCap.cast();
        }
        return super.getCapability(capability, facing);
    }
}