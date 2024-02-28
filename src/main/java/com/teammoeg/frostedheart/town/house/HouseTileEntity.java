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

package com.teammoeg.frostedheart.town.house;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.town.ITownBlockTE;
import com.teammoeg.frostedheart.town.TownWorkerType;
import com.teammoeg.frostedheart.town.resident.Resident;
import com.teammoeg.frostedheart.util.BlockScanner;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.block.PlantBlockHelper.isAir;

/**
 * A house in the town.
 */
public class HouseTileEntity extends TileEntity implements ITownBlockTE {

    public static final double COMFORTABLE_TEMP = 24;
    //public static final int OPTIMAL_VOLUME = 100;
    //public static final int BEST_DECORATION = 100;

    public int size; // how many resident can live here
    public int maxResidents;
    public List<Resident> residents;
    public double temperature;
    public int volume;
    public int decoration;
    public int area;
    private Map<String, Integer> decorations;
    public Set<AbstractMap.SimpleEntry<Integer, Integer>> occupiedArea;
    //public double score_space;
    //public double score_temp;
    //public double score_deco;
    public HouseTileEntity() {
        super(FHTileTypes.HOUSE.get());
    }

    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Get the rating based on house status
     * this isn't now a very crude numerical experiment.
     *
     * @return a rating in range of zero to one
     */
    public double getRating() {
        //this.score_space = calculateSpaceRating(volume,area);
        //score_deco = calculateDecorationRating(decorations, area);
        //score_temp = calculateTemperatureRating(temperature);
        return (calculateDecorationRating(this.decorations, this.area) + calculateSpaceRating(this.volume, this.area) + calculateTemperatureRating(this.temperature)) / 3;
    }

    public static double calculateTemperatureRating(double temperature){
        double tempDiff = Math.abs(COMFORTABLE_TEMP-temperature);
        return 0.017+1/(1+Math.exp(0.4*(tempDiff-10)));
    }

    public static double calculateDecorationRating(Map<?, Integer> decorations, int area){
        double score = 0;
        for(Integer num : decorations.values()){
            score += Math.log(num+0.32)*1.75+0.9;
            //log(x+0.72)*1.5+0.5, sqrt(x*0.8)*1.5
        }
        return Math.min(1, score/(6+area/16.0f));
    }

    public static double calculateSpaceRating(int volume, int area){
        double height = volume/(float)area;
        double score = area * (1.55 + Math.log(height-1.6)*0.6);
        return 1-Math.exp(-0.024*Math.pow(score, 1.11));
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT data = new CompoundNBT();
        // TODO: Serialize resident
        data.putInt("size", size);
        data.putDouble("temperature", temperature);
        data.putInt("volume", volume);
        data.putInt("decoration", decoration);
        return data;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.HOUSE;
    }

    /**
     * Check room insulation
     * Check minimum volume
     * Check within generator range (or just check steam connection instead?)
     *
     * @param housePos the core block
     * @return whether this room is well-defined
     */
    private boolean isRoomValid(BlockPos housePos) {
        Set<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos)-> Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS));
        if(doorPosSet.isEmpty()) return false;
        for(BlockPos doorPos : doorPosSet) {
            for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
                //FHMain.LOGGER.debug("HouseScanner: creating new HouseBlockScanner");
                BlockPos startPos = doorPos.offset(direction);
                //FHMain.LOGGER.debug("HouseScanner: start pos 1" + startPos);
                if (!HouseBlockScanner.isFloorBlock(Objects.requireNonNull(world), startPos)) {
                    startPos = BlockScanner.getBlockBelow((pos) -> HouseBlockScanner.isFloorBlock(world, pos), startPos);
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
                    //FHMain.LOGGER.debug("HouseScanner:\n volume: " + volume + " area: " + area + " decorations: ");
                    //for(String s : decorations.keySet()) FHMain.LOGGER.debug("HouseScanner.decoration: " + s);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if work environment is valid
     */
    @Override
    public boolean isWorkValid() {
        BlockPos pos = this.getPos();
        boolean roomValid = isRoomValid(pos);
        boolean tempConstraint = temperature >= 0 && temperature <= 50;
        return roomValid && tempConstraint;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        size = data.getInt("size");
        temperature = data.getDouble("temperature");
        volume = data.getInt("volume");
        decoration = data.getInt("decoration");
    }
}


class HouseBlockScanner extends BlockScanner{
    private int area = 0;
    private int volume = 0;
    private final Map<String/*block.getName()*/, Integer> decorations = new HashMap<>();
    private double temperature = 0;//average temperature
    private final HashSet<AbstractMap.SimpleEntry<Integer, Integer>> occupiedArea = new HashSet<>();

    public int getArea(){return this.area;}
    public int getVolume(){return this.volume;}
    public Map<String, Integer> getDecorations(){return this.decorations;}
    public double getTemperature(){return this.temperature;}
    public HashSet<AbstractMap.SimpleEntry<Integer, Integer>> getOccupiedArea(){return this.occupiedArea;}

    HouseBlockScanner(World world, BlockPos startPos){
        super(world, startPos);
    }

    boolean isFloorBlock(BlockPos pos){
        BlockState blockState = getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    static boolean isFloorBlock(World world, BlockPos pos){
        BlockState blockState = world.getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
    }

    public static boolean isAirOrLadder(World world, BlockPos pos){
        BlockState state = world.getBlockState(pos);
        return isAir(state) || state.isIn(BlockTags.CLIMBABLE) || state.getShape(world, pos).isEmpty();
    }


    boolean isWallBlock(BlockPos pos){
        BlockState blockState = getBlockState(pos);
        return (blockState.isNormalCube(world, pos) || blockState.isIn(FHTags.Blocks.WALL_BLOCKS) || blockState.isIn(BlockTags.DOORS) || blockState.isIn(BlockTags.WALLS) || blockState.isIn(Tags.Blocks.GLASS_PANES) || blockState.isIn(Tags.Blocks.FENCE_GATES) || blockState.isIn(Tags.Blocks.FENCES));
    }

    boolean isHouseBlock(BlockPos pos){
        return isFloorBlock(pos) || isWallBlock(pos);
    }

    BlockState getBlockState(BlockPos pos){
        return world.getBlockState(pos);
    }

    /**
     * Determine whether a block is a valid floor block.
     *
     * A valid floor block is a block that is a normal cube, a stair, or a slab.
     *
     * A valid floor block must have at least 2 blocks above it.
     *
     * A valid floor block must not have any open air above it.
     *
     * @param pos the position of the block
     * @return whether the block is a valid floor block
     */
    boolean isValidFloor(BlockPos pos){
        // Determine whether the block satisfies type requirements
        if (!isFloorBlock(pos)) return false;
        AbstractMap.SimpleEntry<Integer, Boolean> information = countBlocksAbove(pos, this::isHouseBlock);
        // Determine whether the block has open air above it
        if (!information.getValue()){
            this.isValid = false;
            //FHMain.LOGGER.debug("HouseScanner: found block open air!");
            return false;
        }  else {
            // Determine whether the block has at least 2 blocks above it
            return information.getKey() >= 2;
        }
    }

    void addDecoration(BlockPos pos){
        BlockState blockState = getBlockState(pos);
        Block block = blockState.getBlock();
        if( blockState.isIn(FHTags.Blocks.DECORATIONS) || Objects.requireNonNull(block.getRegistryName()).getNamespace().equals("cfm")) {
            String name = block.toString();
            //FHMain.LOGGER.debug("HouseScanner: adding a decoration");
            if(decorations.get(name) == null || decorations.get(name) == 0){
                decorations.put(name, 1);
            }else{
                decorations.put(name, decorations.get(name) + 1);
            }
        }
    }

    public HashSet<BlockPos> nextScanningBlocks(BlockPos startPos){
        HashSet<BlockPos> possibleFloors = getPossibleFloor(startPos);
        HashSet<BlockPos> possibleFloorsNearLadder = new HashSet<>();
        if(getBlockState(startPos.up()).isIn(BlockTags.CLIMBABLE) || getBlockState(startPos.up(2)).isIn(BlockTags.CLIMBABLE)){
            for(BlockPos ladder : getBlocksAboveAndBelow(startPos.up(), (pos)->!(getBlockState(pos).isIn(BlockTags.CLIMBABLE)) )){
                if(!getBlockState(ladder.up()).isNormalCube(world, ladder.up())) possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
            }
        }
        for(BlockPos blockPos : possibleFloors){
            if(getBlockState(blockPos).isIn(BlockTags.CLIMBABLE) || getBlockState(blockPos.up()).isIn(BlockTags.CLIMBABLE)){
                for(BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos)->!(getBlockState(pos).isIn(BlockTags.CLIMBABLE)) )){
                    if(!getBlockState(ladder.up()).isNormalCube(world, ladder.up())) possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
                }
            }
        }
        HashSet<Long> longSet = toLongSet(possibleFloors);
        longSet.addAll(toLongSet(possibleFloorsNearLadder));
        possibleFloors = toPosSet(longSet);

        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
        for(BlockPos possibleBlock : possibleFloors){
            if(scannedBlocks.contains(possibleBlock.toLong())) {
                continue;
            }
            if(!isValidFloor(possibleBlock)) {
                scannedBlocks.add(possibleBlock.toLong());
                continue;
            }
            nextScanningBlocks.add(possibleBlock);
        }
        return nextScanningBlocks;
    }

    //吗的，lambda玩脱了，现在我也看不懂我写的什么玩意了
    //要写复杂一点的逻辑最好还是新建个类
    boolean scan(){
        //第一次扫描，确定地板的位置，并判断是否有露天的地板
        this.scan(512, (pos)->{
            this.area++;
            this.occupiedArea.add(new AbstractMap.SimpleEntry<>(pos.getX(), pos.getZ()));
            //FHMain.LOGGER.debug("HouseScanner: scanning floor pos " + pos);
        }, this::nextScanningBlocks, (pos)->!this.isValid);
        //FHMain.LOGGER.debug("HouseScanner: first scan area: " + area);
        if(this.area < 3) this.isValid = false;
        if(!this.isValid) return false;
        //FHMain.LOGGER.debug("HouseScanner: first scan completed");

        //第二次扫描，判断房间是否密闭
        BlockScanner airScanner = new BlockScanner(world, startPos.up());
        airScanner.scan(4096, (pos)->{//对每一个空气方块执行的操作：统计温度、统计体积、统计温度
            this.temperature += ChunkHeatData.getTemperature(world, pos);
            this.volume ++;
        }, (pos)->{//接下来是找到下一批需要扫描的方块的内容
            HashSet<BlockPos> blocks = new HashSet<>();//这个HashSet暂存下一批的ScanningBlock
            blocks.addAll(BlockScanner.getBlocksAdjacent/*先找到与本次扫描方块相邻的空气方块*/(pos, (pos1)->{
                if(airScanner.getScannedBlocks().contains(pos1.toLong())) return false;
                if(!isAirOrLadder(world, pos1)){
                    addDecoration(pos1);
                    return false;
                }
                if(!occupiedArea.contains(new AbstractMap.SimpleEntry<>(pos1.getX(), pos1.getZ()))){/*判断这个空气方块是否在合法地板上方。如果不在则进行更多的操作*/
                    AtomicBoolean isOpenAir = new AtomicBoolean(true);//这里要判断这个空气方块上方是否存在遮挡，但是BlockScanner类里面的getBlocksAbove不返回是否碰到stopAt，因此在这里实现。
                    blocks.addAll(getBlocksAbove(pos1, (pos2)->{//这个lambda是getBlocksAbove的停止条件
                        if(airScanner.getScannedBlocks().contains(pos2.toLong())) {
                            isOpenAir.set(false);
                            return true;//这个方块已经扫描过，那么它上方的方块应该也已经扫描过，那么就不在继续扫描
                        }
                        if(isAirOrLadder(world, pos2)){
                            return false;//如果是空气则继续
                        }else{
                            isOpenAir.set(false);
                            return true;//碰到非空气的方块的话，就把isOpenAir设置成false。由于isOpenAir初始值为true，如果这里没触发条件把它改成false的话，它就会是true
                        }
                    }));
                    if(isOpenAir.get()){
                        this.isValid = false;//如果这个空气是露天的话，认为这个房子不是合法的
                        airScanner.isValid = false;
                        return false;
                    }
                }
                return isAirOrLadder(world, pos1);
            }));
                    return deDuplication(blocks);
                },//nextScanningBlocks end
                (useless)->!this.isValid && !airScanner.isValid);
        temperature /= volume;
        if(this.volume < 6) this.isValid = false;

        return this.isValid;
    }
}

/*
            for(BlockPos scanningBlock : scanningBlocks){
                AbstractMap.SimpleEntry<Integer, Boolean> floorBlockInformation = countBlocksAbove(possibleBlock, (pos)->{
                    boolean stopAt = this.isHouseBlock(pos);
                    addDecoration(pos);
                    if(!stopAt) {
                        temperature+= ChunkHeatData.getTemperature(world, pos);
                    }
                    return stopAt;//判断方块是否是天花板，在此过程中顺便帮忙计算一下平均温度和装饰物
                });
                scanningBlocksNew.addAll(nextScanningBlocks.apply(this, scanningBlock));
                scannedBlocks.add(scanningBlock.toLong());
            }
 */