package com.teammoeg.frostedheart.content.town.mine;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.town.OccupiedArea;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import se.mickelus.tetra.blocks.rack.RackBlock;

//矿场基地需要有铁轨连通到矿场，因此不做任何的密封性要求，有个顶就行。
public class MineBaseBlockScanner extends FloorBlockScanner {
    public MineBaseBlockScanner(Level world, BlockPos startPos) {
        super(world, startPos);
    }
    private final HashSet<BlockPos> rails = new HashSet<>();
    private int area = 0;
    private int volume;
    private int chest = 0;
    private int rack = 0;
    private double temperature = 0;
    private int counter_for_temperature = 0;//used to calculate average temperature.
    private final Set<BlockPos> linkedMines = new HashSet<>();
    private final OccupiedArea occupiedArea = new OccupiedArea();

    @Override
    public boolean isValidFloor(BlockPos pos){
        BlockState state = world.getBlockState(pos);
        if(scanningBlocksNew.contains(pos)) return false;
        if(state.is(BlockTags.RAILS)){
            rails.add(pos);
            return false;
        }
        if(!isFloorBlock(pos)){
            return false;
        }
        AbstractMap.SimpleEntry<Integer, Boolean> floorInformation = countBlocksAbove(pos,(pos1)->{
            if(isFloorBlock(pos1)) return true;
            if(world.getBlockState(pos1).is(Tags.Blocks.CHESTS)){
                chest++;
                return false;
            }
            if(world.getBlockState(pos1).getBlock().equals(RackBlock.instance)){
                rack++;
                return false;
            }
            if(NetherVines.isValidGrowthState(world.getBlockState(pos1))){
                temperature += WorldTemperature.block(world, pos1);
                counter_for_temperature++;
                return false;
            }
            return false;
        });
        if(floorInformation.getValue() && floorInformation.getKey() >= 2){
            volume += floorInformation.getKey();
            return true;
        } else return false;
    }

    public double getTemperature(){
        return temperature;
    }
    public int getArea() {
        return area;
    }
    public int getVolume() {
        return volume;
    }
    public int getChest() {
        return chest;
    }
    public int getRack() {
        return rack;
    }
    public Set<BlockPos> getLinkedMines() {
        return linkedMines;
    }
    public OccupiedArea getOccupiedArea() {
        return occupiedArea;
    }

    public boolean scan(){
        this.scan(256, (blockPos) -> {
            area++;
            //FHMain.LOGGER.info("Scanning pos: " + blockPos);
            occupiedArea.add(toColumnPos(blockPos));
            }, BlockScanner.PREDICATE_FALSE);
        temperature /= counter_for_temperature;
        if(!this.rails.isEmpty()){
            RailScanner railScanner = new RailScanner();
            railScanner.scan(512, CONSUMER_NULL, PREDICATE_FALSE);
        }
        return this.area > 4 && this.volume > 8 && this.isValid;
    }

    class RailScanner extends FloorBlockScanner{
        public RailScanner(){
            super(MineBaseBlockScanner.this.world, MineBaseBlockScanner.this.rails.iterator().next());
            this.scanningBlocks = MineBaseBlockScanner.this.rails;
        }

        @Override
        public boolean isValidFloor(BlockPos pos){
            if(world.getBlockState(pos).is(BlockTags.RAILS)){
                return NetherVines.isValidGrowthState(world.getBlockState(pos.above()));
            } else if(world.getBlockState(pos).getBlock().equals(FHBlocks.MINE.get())){
                linkedMines.add(pos);
            }
            return false;
        }
    }
}
