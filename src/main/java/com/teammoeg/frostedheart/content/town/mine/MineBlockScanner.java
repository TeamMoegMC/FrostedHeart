package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.util.blockscanner.ConfinedSpaceScanner;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.HashSet;
import java.util.function.Consumer;

import static net.minecraft.block.PlantBlockHelper.isAir;

public class MineBlockScanner extends ConfinedSpaceScanner {
    private final int startX;
    private final int startY;
    private final int startZ;
    public int validStone = 0;
    public int light = 0;
    public MineBlockScanner(World world, BlockPos startPos) {
        super(world, startPos);
        this.startX = startPos.getX();
        this.startY = startPos.getY();
        this.startZ = startPos.getZ();
    }

    public static boolean isStoneOrOre(World world, BlockPos pos){
        BlockState state = world.getBlockState(pos);
        return state.isIn(Tags.Blocks.ORES) || state.isIn(Tags.Blocks.STONE);
    }

    @Override
    protected boolean isValidAir(BlockPos pos){
        return Math.abs(pos.getZ()-startZ) < 6 && Math.abs(pos.getX()-startX) < 6 && Math.abs(pos.getY()-startY) < 4 && isAir(world.getBlockState(pos));
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
        this.scan(512, CONSUMER_NULL, (pos)->{
            if(isStoneOrOre(world, pos)){
                validStone++;
            }
            light += world.getBlockState(pos).getLightValue(world, pos);
        }, PREDICATE_FALSE);
        if(validStone <= 0){
            this.isValid = false;
            return false;
        }
        light = light * 7 / validStone;
        return this.isValid;
    }



}
