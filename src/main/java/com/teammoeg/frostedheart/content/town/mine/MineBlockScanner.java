package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.OccupiedArea;
import com.teammoeg.frostedheart.util.blockscanner.ConfinedSpaceScanner;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.block.PlantBlockHelper.isAir;

public class MineBlockScanner extends ConfinedSpaceScanner {
    private final int startX;
    private final int startY;
    private final int startZ;
    private int validStone = 0;
    private int light = 0;
    private double temperature = 0;
    private int volume = 0;//used to calculate temperature
    private final OccupiedArea occupiedArea = new OccupiedArea();
    public MineBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
        this.startX = startPos.getX();
        this.startY = startPos.getY();
        this.startZ = startPos.getZ();
    }

    public int getValidStone() {
        return validStone;
    }
    public int getLight(){
        return light;
    }
    public double getTemperature(){
        return temperature;
    }
    public OccupiedArea getOccupiedArea() {
        return occupiedArea;
    }


    public static boolean isStoneOrOre(World world, BlockPos pos){
        BlockState state = world.getBlockState(pos);
        return state.isIn(Tags.Blocks.ORES) || state.isIn(Tags.Blocks.STONE);
    }

    @Override
    protected boolean isValidAir(BlockPos pos){
        return Math.abs(pos.getZ()-startZ) < 6 && Math.abs(pos.getX()-startX) < 6 && Math.abs(pos.getY()-startY) < 5 && isAir(world.getBlockState(pos));
    }

    @Override
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos pos, Consumer<BlockPos> operation){
        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
        nextScanningBlocks.addAll(getBlocksAdjacent(pos, (pos1)->{
            if(scannedBlocks.contains(pos1) || scanningBlocks.contains(pos1)) return false;
            if(isValidAir(pos1)) return true;
            else {
                operation.accept(pos1);
                scannedBlocks.add(pos1);
                return false;
            }
        }));
        return nextScanningBlocks;
    }

    public boolean scan(){
        this.scan(512, (pos)->{
            this.volume++;
            this.temperature += ChunkHeatData.getTemperature(world, pos);
        }, (pos)->{
            if(isStoneOrOre(world, pos)){
                validStone++;
                occupiedArea.add(toColumnPos(pos));
            }
            light += world.getBlockState(pos).getLightValue(world, pos);
        }, PREDICATE_FALSE);
        if(validStone <= 0){
            this.isValid = false;
            return false;
        }
        light = light * 7 / validStone;//单个光源亮度约为14-15，乘7后约为100.
        temperature = temperature / volume;
        return this.isValid && validStone > 5;
    }
}
