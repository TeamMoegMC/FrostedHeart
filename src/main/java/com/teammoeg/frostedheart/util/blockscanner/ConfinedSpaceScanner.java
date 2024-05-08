package com.teammoeg.frostedheart.util.blockscanner;


import java.util.AbstractMap;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * scan air
 * 可用于判断一个空间是否密闭。
 * 如果需要用某些特定的方块包围密闭空间，可以在子类中覆写isValidAir方法
 */
public class ConfinedSpaceScanner extends BlockScanner {

    public ConfinedSpaceScanner(World world, BlockPos startPos){
        super(world, startPos);
    }

    /**
     * 在子类中覆写此方法以修改空气的判定条件。如果你想扫描什么别的方块也可以用这个类并覆写此方法
     */
    protected boolean isValidAir(BlockPos pos){
        return isAirOrLadder(world, pos);
    }

    /**
     * @param pos scanning block
     * @param operation 对于满足nextScanningBlocks的位置条件（此处是相邻的方块），但是方块种类不满足的，执行此操作。在此处，就是对围住密闭空间的方块执行此操作
     */
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos pos, Consumer<BlockPos> operation){//接下来是找到下一批需要扫描的方块的内容
        HashSet<BlockPos> nextScanningBlocks = new HashSet<>();//这个HashSet暂存下一批的ScanningBlock
        for(Direction direction : Direction.values()){
            BlockPos pos1 = pos.offset(direction);// pos1: 用于存储与pos相邻的方块
            if (this.getScannedBlocks().contains(pos1)) continue;
            if (!isValidAir(pos1)) {
                operation.accept(pos1);
                scannedBlocks.add(pos1);
                continue;
            }
            nextScanningBlocks.add(pos1);
            AbstractMap.SimpleEntry<HashSet<BlockPos>, Boolean> airsAbove = getAirsAbove(pos1);
            if(!airsAbove.getValue()){
                this.isValid = false;
                return nextScanningBlocks;
            }
            else nextScanningBlocks.addAll(airsAbove.getKey());
        }
        return nextScanningBlocks;
    }

    @Override
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos pos){
        return nextScanningBlocks(pos, CONSUMER_NULL);
    }

    //基本上和getBlocksAbove是相同的，为了减少lambda的使用单列一个方法
    private AbstractMap.SimpleEntry<HashSet<BlockPos>, Boolean> getAirsAbove(BlockPos startPos){
        BlockPos scanningBlock;
        scanningBlock = startPos.up();
        HashSet<BlockPos> blocks = new HashSet<>();
        while(scanningBlock.getY() < 256){
            if( scannedBlocks.contains(scanningBlock) || !isValidAir(scanningBlock) ){
                return new AbstractMap.SimpleEntry<>(blocks, true);
            }
            blocks.add(scanningBlock);
            scanningBlock = scanningBlock.up();
        }
        return new AbstractMap.SimpleEntry<>(blocks, false);
    }

    /**
     * 专用于判断密闭空间的扫描方法。
     * @param maxScanningTimes 最大扫描次数
     * @param operation1 对所有空气方块，都会进行operation1
     * @param operation2 对所有非空气方块，都会进行operation2
     * @param stopAt 如果扫描的方块满足了stopAt的条件，则会停止扫描并返回false
     * @return 扫描成功与否
     */
    public boolean scan(int maxScanningTimes, Consumer<BlockPos> operation1, Consumer<BlockPos> operation2, Predicate<BlockPos> stopAt){
        return this.scan(maxScanningTimes, operation1, (pos)-> {return nextScanningBlocks(pos, operation2);}, stopAt);
    }
}
