package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.*;

import static net.minecraft.block.PlantBlockHelper.isAir;

/**
 * 提供了一些扫描方块用的静态方法
 * 可用于对形状未知，但以一定条件互相关联的方块，进行一些操作。若要如此做，最好写一个子类，并覆写nextScanningBlocks和scan方法，以实现更多功能。
 * 以一定条件互相关联，就是可以通过一个方块的位置，加上一些其它必要条件，获取到其余的应被扫描的方块位置。例如：通过一个水方块，获取相邻的水方块。以此类推可以将一片连续的水域全部扫描。
 * 可以进行的操作包括但不限于统计数量、统计温度
 * 和扫描方块有关的其它方法也可以丢在这里
 * 这里面的方法可能存在问题，发现的话请帮我改了谢谢茄子(
 */
public class BlockScanner {
    protected Set<Long> scannedBlocks;
    protected Set<BlockPos> scanningBlocks;
    protected final BlockPos startPos;
    public final World world;
    public static final Direction[] PLANE_DIRECTIONS= {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
    public boolean isValid = true;//it can be changed in methods, scan should stop when this is false

    public BlockScanner (World world, BlockPos startPos){
        this.startPos = startPos;
        scanningBlocks = new HashSet<>();
        scanningBlocks.add(startPos);
        //FHMain.LOGGER.debug("HouseScanner: scanningBlocks: " + scanningBlocks);
        this.world = world;
        scannedBlocks = new HashSet<>();
    }

    public BlockScanner (World world, BlockPos startPos, Set<Long> scannedBlocks){
        this.startPos = startPos;
        assert scanningBlocks != null;
        scanningBlocks.add(startPos);
        this.world = world;
        this.scannedBlocks = scannedBlocks;
    }

    public Set<Long> getScannedBlocks(){
        return this.scannedBlocks;
    }

    public void addScannedBlock(BlockPos pos){
        this.scannedBlocks.add(pos.toLong());
    }

    public Set<BlockPos> getScanningBlocks(){
        return this.scanningBlocks;
    }

    public void setScanningBlocks(Set<BlockPos> scanningBlocks){
        this.scanningBlocks = scanningBlocks;
    }

    protected BlockState getBlockState(BlockPos pos) {
        return world.getBlockState(pos);
    }

    public boolean isValid(){
        return this.isValid;
    }


    public static int countBlocksAdjacent(World world, BlockPos startPos, Block targetBlock){
        return countBlocksAdjacent(startPos, (pos)->world.getBlockState(pos).getBlock() == targetBlock);
    }

    /**
     * @param startPos The block that you want to scan its Adjacent
     * @param target The block you are searching
     * @return The number of targetBlock adjacent to startPos
     */
    public static int countBlocksAdjacent( BlockPos startPos, Predicate<BlockPos> target){
        int num = 0;
        for(Direction direction : Direction.values()){
            if(target.test(startPos.offset(direction))){
                num++;
            }
        }
        return num;
    }

    /**
     * @param target The function that determines weather a block is the target block.
     * @param startPos Scan blocks above this block.
     * @param stopAt When the scanning block makes this true, stop scanning.
     * @return The number of target blocks above scanningBlock, and weather the scanning block makes stopAt returned true
     */
    public static AbstractMap.SimpleEntry<Integer, Boolean> countBlocksAbove(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        BlockPos scanningBlock;
        int num = 0;
        scanningBlock = startPos.up();
        while(scanningBlock.getY() < 255){
            if(stopAt.test(scanningBlock)){
                return new AbstractMap.SimpleEntry<>(num, true);
            }
            if(target.test(scanningBlock)){
                num++;
            }
            scanningBlock = scanningBlock.up();
        }
        return new AbstractMap.SimpleEntry<>(num, false);
    }
    public static int countBlocksAbove(Predicate<BlockPos> target, BlockPos startPos){
        BlockPos scanningBlock;
        int num = 0;
        scanningBlock = startPos.up();
        while(scanningBlock.getY() < 255){
            if(target.test(scanningBlock)){
                num++;
            }
        }
        return num;
    }
    public static AbstractMap.SimpleEntry<Integer, Boolean> countBlocksAbove( BlockPos startPos, Predicate<BlockPos> stopAt){
        return countBlocksAbove((useless)->true, startPos, stopAt);
    }


    /**
     * @param startPos The block that you want to scan its Adjacent
     * @param target The block you are searching
     * @return All targetBlock adjacent to startPos
     */
    public static HashSet<BlockPos> getBlocksAdjacent( BlockPos startPos, Predicate<BlockPos> target){
        HashSet<BlockPos> blocks = new HashSet<>();
        for(Direction direction : Direction.values()){
            if(target.test(startPos.offset(direction))){
                blocks.add(startPos.offset(direction));
            }
        }
        return blocks;
    }

    /**
     * 找到与startPos相邻的一个门方块。若有多个门，则只返回其中一个的坐标。
     */
    public static BlockPos getDoorAdjacent(World world, BlockPos startPos){
        for(Direction direction : Direction.values()){
            if(world.getBlockState(startPos.offset(direction)).isIn(BlockTags.DOORS)){
                return startPos.offset(direction);
            }
        }
        return null;
    }

    public static HashSet<BlockPos> getBlocksAbove(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        BlockPos scanningBlock;
        scanningBlock = startPos.up();
        HashSet<BlockPos> blocks = new HashSet<>();
        while(scanningBlock.getY() < 256){
            if(stopAt.test(scanningBlock)){
                return blocks;
            }
            if(target.test(scanningBlock)){
                blocks.add(scanningBlock);
            }
            scanningBlock = scanningBlock.up();
        }
        return blocks;
    }
    public static HashSet<BlockPos> getBlocksAbove(Predicate<BlockPos> target, BlockPos startPos){
        return getBlocksAbove(target, startPos, (useless)->false);
    }
    public static HashSet<BlockPos> getBlocksAbove(BlockPos startPos, Predicate<BlockPos> stopAt){
        return getBlocksAbove((useless)->true, startPos, stopAt);
    }


    public static HashSet<BlockPos> getBlocksBelow(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        BlockPos scanningBlock;
        scanningBlock = startPos.down();
        HashSet<BlockPos> blocks = new HashSet<>();
        while(scanningBlock.getY() > 0){
            if(stopAt.test(scanningBlock)){
                return blocks;
            }
            if(target.test(scanningBlock)){
                blocks.add(scanningBlock);
            }
            scanningBlock = scanningBlock.down();
        }
        return blocks;
    }
    public static HashSet<BlockPos> getBlocksBelow(Predicate<BlockPos> target, BlockPos startPos){
        return getBlocksBelow(target, startPos, (useless)->false);
    }
    public static HashSet<BlockPos> getBlocksBelow(BlockPos startPos, Predicate<BlockPos> stopAt){
        return getBlocksBelow((useless)->true, startPos, stopAt);
    }

    public static HashSet<BlockPos> getBlocksAboveAndBelow(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        HashSet<BlockPos> blocks = new HashSet<>();
        blocks.addAll(getBlocksBelow(target, startPos, stopAt));
        blocks.addAll(getBlocksAbove(target, startPos, stopAt));
        return blocks;
    }
    public static HashSet<BlockPos> getBlocksAboveAndBelow(Predicate<BlockPos> target, BlockPos startPos){
        return getBlocksAboveAndBelow(target, startPos, (useless)->false);
    }
    public static HashSet<BlockPos> getBlocksAboveAndBelow(BlockPos startPos, Predicate<BlockPos> stopAt){
        return getBlocksAboveAndBelow((useless)->true, startPos, stopAt);
    }

    public static HashSet<BlockPos> getBlocksAdjacent_plane(Predicate<BlockPos> target, BlockPos scanningBlock){
        HashSet<BlockPos> targetBlocks = new HashSet<>();
        for(Direction direction : PLANE_DIRECTIONS){
            if(target.test(scanningBlock.offset(direction))){
                targetBlocks.add(scanningBlock.offset(direction));
            }
        }
        return targetBlocks;
    }

    /**
     * @return  返回pos对应方块x,z方向相邻的4个方块，以及这4个方块向上一格和向下一格的8个方块
     */
    public static HashSet<BlockPos> getPossibleFloor(IWorld world, BlockPos pos){
        HashSet<BlockPos> blocks = new HashSet<>();
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.up()));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.down()));
        return blocks;
    }
    public HashSet<BlockPos> getPossibleFloor(BlockPos pos){
        return getPossibleFloor(world, pos);
    }

    public HashSet<BlockPos> getPossibleFloorNearLadder(BlockPos pos){
        HashSet<BlockPos> blocks = new HashSet<>();
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.up()));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.down()));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.down(2)));
        return blocks;
    }

    /**
     * Find valid floor block near the block
     * @param isValidFloor determine if a block is valid floor
     */
    public HashSet<BlockPos> getFloorAdjacent(BlockPos pos, Predicate<BlockPos> isValidFloor){
        HashSet<BlockPos> possibleBlocks = getPossibleFloor(pos);
        for(BlockPos possibleBlock : possibleBlocks){
            if(scannedBlocks.contains(pos.toLong()) || scanningBlocks.contains(pos)) possibleBlocks.remove(pos);
            if(!isValidFloor.test(pos)) possibleBlocks.remove(pos);
        }
        return possibleBlocks;
    }

    /**
     * 默认本身为完整方块，且上方两格为均空气的方块为合法的地板。若有不同需求请用上面那个方法
     */
    public HashSet<BlockPos> getFloorAdjacent(BlockPos pos){
        return getFloorAdjacent(pos, (BlockPos)-> world.getBlockState(pos).isNormalCube(world, pos) && isAir(world.getBlockState(pos.up())) && isAir(world.getBlockState(pos.up(2))));
    }

    public boolean isOpenAir(BlockPos pos){
        return countBlocksAbove(pos, blockPos -> !isAir(world.getBlockState(blockPos))).getValue();
    }

    /**
     * 判断一个方块是否是空气/无碰撞箱方块/梯子
     */
    public static boolean isAirOrLadder(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return isAir(state) || state.isIn(BlockTags.CLIMBABLE) || state.getCollisionShape(world, pos).isEmpty();
    }

    /**
     * @return return the first position of the block that makes target returns true
     */
    public static BlockPos getBlockBelow(Predicate<BlockPos> target, BlockPos startPos){
        BlockPos scanningBlock;
        scanningBlock = startPos.down();
        while(scanningBlock.getY() > 0){
            if(target.test(scanningBlock)){
                return scanningBlock;
            }
            else scanningBlock = scanningBlock.down();
        }
        return null;
    }

    public static HashSet<Long> toLongSet(Collection<BlockPos> collection){
        HashSet<Long> longSet = new HashSet<>();
        for(BlockPos pos : collection){
            longSet.add(pos.toLong());
        }
        return longSet;
    }

    public static HashSet<BlockPos> toPosSet(Collection<Long> collection){
        HashSet<BlockPos> posSet = new HashSet<>();
        for(Long posLong : collection){
            posSet.add(BlockPos.fromLong(posLong));
        }
        return posSet;
    }


    /**
     * 获取接下来要被扫描的方块。这个方法应在子类中重写，而非直接使用。
     * @return 接下来要被扫描的方块。这个Set不应包含scannedBlocks中已记录的内容及scanningBlocks中的内容。
     */
    protected HashSet<BlockPos> nextScanningBlocks(BlockPos startPos){
        return new HashSet<>();
    }

    /**
     * 对每个scanningBlocks的方块执行operation操作，并按照nextScanningBlocks方法的规则获取新的scanningBlocks。
     * scan方法的工作模式：
     * 初始状态：在类创建完成后，scanningBlocks里会有一个blockPos，即startPos
     * 工作时，将scanningBlocks以long的形式放入scannedBlock，创建scanningBlocksNew集合，遍历scanningBlocks中的所有blockPos，然后对blockPos进行以下操作：
     * 1 如果满足stopAt，则直接退出扫描并返回false
     * 2 对blockPos执行operation操作
     * 3 对blockPos执行nextScanningBlocks方法，获取下一轮扫描的方块（这个方法应自己写，写的时候应注意排除scannedBlocks以及避免重复），然后存入scanningBlocksNew中
     * 在一轮scanningBlocks扫描结束后，把scanningBlocks换成scanningBlocksNew
     * @param operation 对于每个扫描的方块，都会执行这里的操作。由于输入了BlockScanner，也可以对BlockScanner类及其子类里面的变量进行操作。
     * @param nextScanningBlocks 用于获取接下来要被扫描的方块。
     * @param stopAt 如果扫描的方块满足了stopAt的条件，则会停止扫描并返回false
     * @return 扫描成功与否。如果scan完成了（scanningBlock为空），就会返回true；如果scan中途中断（触发了stopAt，或是scanTimes达到了上限2048），则会返回false
     */
    public boolean scan(int maxScanningTimes,
                        Consumer<BlockPos> operation,
                        Function<BlockPos, HashSet<BlockPos>> nextScanningBlocks,
                        Predicate<BlockPos> stopAt){
        int scanTimes = 0;
        while(!scanningBlocks.isEmpty() && isValid){
            if(scanTimes > maxScanningTimes){
                return false;
            }
            scannedBlocks.addAll(toLongSet(scanningBlocks));
            HashSet<BlockPos> scanningBlocksNew = new HashSet<>();
            for(BlockPos scanningBlock : scanningBlocks){
                if(stopAt.test(scanningBlock) || !this.isValid) return false;
                operation.accept(scanningBlock);
                scanningBlocksNew.addAll(nextScanningBlocks.apply(scanningBlock));
            }
            scanningBlocks = scanningBlocksNew;
            scanTimes++;
        }
        return this.isValid;
    }

    public boolean scan(int maxScanningTimes,
                        Consumer<BlockPos> operation,
                        Predicate<BlockPos> stopAt){
        return scan(maxScanningTimes, operation, this::nextScanningBlocks, stopAt);
    }


    /**
     * scan valid floor
     */
    public static class FloorBlockScanner extends BlockScanner{
        public final boolean canUseLadder;

        public FloorBlockScanner(World world, BlockPos startPos) {
            super(world, startPos);
            this.canUseLadder = true;
        }

        public FloorBlockScanner(World world, BlockPos startPos, boolean canUseLadder) {
            super(world, startPos);
            this.canUseLadder = canUseLadder;
        }

        protected boolean isFloorBlock(BlockPos pos) {
            BlockState blockState = getBlockState(pos);
            return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
        }

        public static boolean isFloorBlock(World world, BlockPos pos) {
            BlockState blockState = world.getBlockState(pos);
            return (blockState.isNormalCube(world, pos) || blockState.isIn(BlockTags.STAIRS) || blockState.isIn(BlockTags.SLABS));
        }

        public static boolean isWallBlock(World world, BlockPos pos) {
            BlockState blockState = world.getBlockState(pos);
            return (blockState.isNormalCube(world, pos) || blockState.isIn(FHTags.Blocks.WALL_BLOCKS) || blockState.isIn(BlockTags.DOORS) || blockState.isIn(BlockTags.WALLS) || blockState.isIn(Tags.Blocks.GLASS_PANES) || blockState.isIn(Tags.Blocks.FENCE_GATES) || blockState.isIn(Tags.Blocks.FENCES));
        }
        boolean isWallBlock(BlockPos pos) {
            return isWallBlock(this.world, pos);
        }

        boolean isHouseBlock(BlockPos pos) {
            return isFloorBlock(pos) || isWallBlock(pos);
        }

        public static boolean isHouseBlock(World world, BlockPos pos){
            return isFloorBlock(world, pos) || isWallBlock(world, pos);
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
         * Determine whether a block is a valid floor block.
         * A valid floor block is a block that is a normal cube, a stair, or a slab.
         * A valid floor block must have at least 2 blocks above it.
         * A valid floor block must not have any open air above it.
         * 【Override it if you need】
         * @param pos the position of the block
         * @return whether the block is a valid floor block
         */
        public boolean isValidFloor(BlockPos pos) {
            // Determine whether the block satisfies type requirements
            if (!isFloorBlock(pos)) return false;
            AbstractMap.SimpleEntry<Integer, Boolean> information = countBlocksAbove(pos, this::isHouseBlock);
            // Determine whether the block has open air above it
            if (!information.getValue()) {
                this.isValid = false;
                //FHMain.LOGGER.debug("HouseScanner: found block open air!");
                return false;
            } else {
                // Determine whether the block has at least 2 blocks above it
                return information.getKey() >= 2;
            }
        }

        protected boolean isValidLadder(BlockPos pos){
            return world.getBlockState(pos).isIn(BlockTags.CLIMBABLE) && isAirOrLadder(world, pos.up()) && isAirOrLadder(world, pos.up(2));
        }

        /**
         * Given a floor block, find all possible floor blocks that are adjacent to it.
         *
         * @param startPos the position of the floor block
         * @return a set of possible floor blocks
         */
        protected HashSet<BlockPos> nextScanningBlocks(BlockPos startPos) {
            HashSet<BlockPos> possibleFloors = getPossibleFloor(startPos);
            if(canUseLadder) {
                HashSet<BlockPos> possibleFloorsNearLadder = new HashSet<>();
                if (getBlockState(startPos.up()).isIn(BlockTags.CLIMBABLE) || getBlockState(startPos.up(2)).isIn(BlockTags.CLIMBABLE)) {
                    for (BlockPos ladder : getBlocksAboveAndBelow(startPos.up(), (pos) -> !(getBlockState(pos).isIn(BlockTags.CLIMBABLE)))) {
                        if (isValidLadder(ladder))
                            possibleFloorsNearLadder.addAll(getPossibleFloor(ladder));
                    }
                }
                for (BlockPos blockPos : possibleFloors) {
                    if (getBlockState(blockPos).isIn(BlockTags.CLIMBABLE) || getBlockState(blockPos.up()).isIn(BlockTags.CLIMBABLE)) {
                        for (BlockPos ladder : getBlocksAboveAndBelow(blockPos, (pos) -> !(getBlockState(pos).isIn(BlockTags.CLIMBABLE)))) {
                            if (isValidLadder(ladder))
                                possibleFloorsNearLadder.addAll(getPossibleFloorNearLadder(ladder));
                        }
                    }
                }
                possibleFloors.addAll(possibleFloorsNearLadder);
            }
            HashSet<BlockPos> nextScanningBlocks = new HashSet<>();
            for (BlockPos possibleBlock : possibleFloors) {
                if (scannedBlocks.contains(possibleBlock.toLong())) {
                    continue;
                }
                if (!isValidFloor(possibleBlock)) {
                    scannedBlocks.add(possibleBlock.toLong());
                    continue;
                }
                nextScanningBlocks.add(possibleBlock);
            }
            return nextScanningBlocks;
        }

        //暂时不需要覆写scan，BlockScanner的scan够用了
    }
    /**
     * scan air
     */
    public static class ConfinedSpaceScanner extends BlockScanner {

        public ConfinedSpaceScanner(World world, BlockPos startPos){
            super(world, startPos);
        }

        /**
         * @param pos scanning block
         * @param operation 对于满足nextScanningBlocks的位置条件（此处是相邻的方块），但是方块种类不满足的，执行此操作。在此处，就是对围住密闭空间的方块执行此操作
         */
        private HashSet<BlockPos> nextScanningBlocks(BlockPos pos, Consumer<BlockPos> operation){//接下来是找到下一批需要扫描的方块的内容
            HashSet<BlockPos> nextScanningBlocks = new HashSet<>();//这个HashSet暂存下一批的ScanningBlock
            for(Direction direction : Direction.values()){
                BlockPos pos1 = pos.offset(direction);// pos1: 用于存储与pos相邻的方块
                if (this.getScannedBlocks().contains(pos1.toLong())) continue;
                if (!BlockScanner.isAirOrLadder(world, pos1)) {
                    operation.accept(pos1);
                    continue;
                }
                nextScanningBlocks.add(pos1);
                AbstractMap.SimpleEntry<HashSet<BlockPos>, Boolean> airsAbove = getAirsAbove(pos1);
                if(!airsAbove.getValue()){
                    this.isValid = false;
                    return null;
                }
                else nextScanningBlocks.addAll(airsAbove.getKey());
            }
            return nextScanningBlocks;
        }

        //基本上和getBlocksAbove是相同的，为了减少lambda的使用单列一个方法
        private AbstractMap.SimpleEntry<HashSet<BlockPos>, Boolean> getAirsAbove(BlockPos startPos){
            BlockPos scanningBlock;
            scanningBlock = startPos.up();
            HashSet<BlockPos> blocks = new HashSet<>();
            while(scanningBlock.getY() < 256){
                if( scannedBlocks.contains(scanningBlock.toLong()) || !isAirOrLadder(world, scanningBlock) ){
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
}
