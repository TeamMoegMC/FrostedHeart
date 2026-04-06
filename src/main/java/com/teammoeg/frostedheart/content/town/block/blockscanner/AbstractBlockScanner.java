/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.town.block.blockscanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 提供了一些扫描方块用的静态方法和模板方法
 * 使用模板方法模式实现方块的扫描逻辑。子类通过覆写钩子方法来定制扫描行为。
 *
 * <p>模板方法模式设计：</p>
 * <ul>
 *   <li>{@code scan()} - 主扫描方法，定义扫描流程，为final方法</li>
 *   <li>{@code nextScanningBlocks(BlockPos)} - 子类必须实现，定义如何获取下一批扫描方块</li>
 *   <li>{@code processBlock(BlockPos)} - 子类可覆写，定义如何处理每个扫描到的方块</li>
 *   <li>{@code shouldStopAt(BlockPos)} - 子类可覆写，定义是否提前停止扫描（不影响isValid）</li>
 * </ul>
 *
 * <p>可用于对形状未知，但以一定条件互相关联的方块，进行一些操作。</p>
 * <p>以一定条件互相关联，就是可以通过一个方块的位置，加上一些其它必要条件，获取到其余的应被扫描的方块位置。</p>
 * <p>例如：通过一个水方块，获取相邻的水方块。以此类推可以将一片连续的水域全部扫描。</p>
 * <p>可以进行的操作包括但不限于统计数量、统计温度</p>
 * <p>和扫描方块有关的其它方法也可以丢在这里</p>
 * <p>这里面的方法可能存在问题，发现的话请帮我改了谢谢茄子(</p>
 * <p><b>这里的部分方法由于高度限制只适用于主世界！</b></p>
 */
public abstract class AbstractBlockScanner {
    // 常量定义
    public static final int MAX_HEIGHT = 320;
    public static final int MIN_HEIGHT = -64;
    public static final int DEFAULT_MAX_SCAN_BLOCKS = 4096;
    public static final int MIN_ABOVE_HEIGHT = 2;

    @Getter
    protected LongSet scannedBlocks = new LongOpenHashSet();
    @Getter
    @Setter
    protected LongSet scanningBlocks = new LongOpenHashSet();
    protected LongSet scanningBlocksNew = new LongOpenHashSet();
    public final int maxScanBlocks;
    protected final BlockPos startPos;
    public final Level world;
    public static final Direction[] PLANE_DIRECTIONS= {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
    public static final Predicate<BlockPos> PREDICATE_TRUE = (useless)->true;
    public static final Predicate<BlockPos> PREDICATE_FALSE = (useless)->false;
    public static final Consumer<BlockPos> CONSUMER_NULL = (useless)->{};
    public boolean isValid = true;//it can be changed in methods, scan should stop when this is false

    public AbstractBlockScanner(Level world, BlockPos startPos, int maxScanBlocks){
        this.startPos = startPos;
        this.maxScanBlocks = maxScanBlocks;
        this.scanningBlocks = new LongOpenHashSet();
        this.scanningBlocks.add(startPos.asLong());
        //FHMain.LOGGER.debug("HouseScanner: scanningBlocks: " + scanningBlocks);
        this.world = world;
        this.scannedBlocks = new LongOpenHashSet();
    }

    /**
     * 默认构造函数，使用默认的最大扫描次数DEFAULT_MAX_SCAN_TIMES
     */
    public AbstractBlockScanner(Level world, BlockPos startPos){
        this(world, startPos, DEFAULT_MAX_SCAN_BLOCKS);
    }

    public AbstractBlockScanner(Level world, BlockPos startPos, LongSet scannedBlocks, int maxScanBlocks){
        this.startPos = startPos;
        this.maxScanBlocks = maxScanBlocks;
        this.scanningBlocks = new LongOpenHashSet();
        this.scanningBlocks.add(startPos.asLong());
        this.world = world;
        this.scannedBlocks = scannedBlocks;
    }

    public void addScannedBlock(BlockPos pos){
        this.scannedBlocks.add(pos.asLong());
    }

    /**
     * 添加扫描块（返回是否成功添加）
     */
    private boolean addScanningBlock(BlockPos pos) {
        long key = pos.asLong();
        if (scannedBlocks.contains(key)) return false;
        return scanningBlocks.add(key);
    }

    protected BlockState getBlockState(BlockPos pos) {
        return world.getBlockState(pos);
    }

    public boolean isValid(){
        return this.isValid;
    }


    public static int countBlocksAdjacent(Level world, BlockPos startPos, Block targetBlock){
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
            BlockPos adjacentPos = startPos.relative(direction);
            if(adjacentPos.getY() >= MIN_HEIGHT && adjacentPos.getY() <= MAX_HEIGHT){
                if(target.test(adjacentPos)){
                    num++;
                }
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
    public static HeightCheckingInfo countBlocksAbove(LevelReader level,Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        BlockPos scanningBlock;
        int num = 0;
        scanningBlock = startPos.above();
        int maxHeight=level.getHeight(Heightmap.Types.WORLD_SURFACE,startPos.getX(),startPos.getZ());
        while(scanningBlock.getY() < maxHeight){
            if(stopAt.test(scanningBlock)){
                return new HeightCheckingInfo(num, true);
            }
            if(target.test(scanningBlock)){
                num++;
            }
            scanningBlock = scanningBlock.above();
        }
        return new HeightCheckingInfo(num, false);
    }
    public static int countBlocksAbove(Predicate<BlockPos> target, BlockPos startPos){
        BlockPos scanningBlock;
        int num = 0;
        scanningBlock = startPos.above();
        while(scanningBlock.getY() < MAX_HEIGHT){
            if(target.test(scanningBlock)){
                num++;
            }
        }
        return num;
    }
    public static HeightCheckingInfo countBlocksAbove(LevelReader level,BlockPos startPos, Predicate<BlockPos> stopAt){
        return countBlocksAbove(level,(useless)->true, startPos, stopAt);
    }


    /**
     * @param startPos The block that you want to scan its Adjacent
     * @param target The block you are searching
     * @return All targetBlock adjacent to startPos
     */
    public static ArrayList<BlockPos> getBlocksAdjacent( BlockPos startPos, Predicate<BlockPos> target){
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for(Direction direction : Direction.values()){
            if(target.test(startPos.relative(direction))){
                blocks.add(startPos.relative(direction));
            }
        }
        return blocks;
    }

    /**
     * 找到与startPos相邻的一个门方块。若有多个门，则只返回其中一个的坐标。
     */
    public static BlockPos getDoorAdjacent(Level world, BlockPos startPos){
        for(Direction direction : Direction.values()){
            if(world.getBlockState(startPos.relative(direction)).is(BlockTags.DOORS)){
                return startPos.relative(direction);
            }
        }
        return null;
    }

    public static ArrayList<BlockPos> getBlocksAbove(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        BlockPos scanningBlock;
        scanningBlock = startPos.above();
        ArrayList<BlockPos> blocks = new ArrayList<>();
        while(scanningBlock.getY() <= MAX_HEIGHT){
            if(stopAt.test(scanningBlock)){
                return blocks;
            }
            if(target.test(scanningBlock)){
                blocks.add(scanningBlock);
            }
            scanningBlock = scanningBlock.above();
        }
        return blocks;
    }
    public static ArrayList<BlockPos> getBlocksAbove(Predicate<BlockPos> target, BlockPos startPos){
        return getBlocksAbove(target, startPos, (useless)->false);
    }
    public static ArrayList<BlockPos> getBlocksAbove(BlockPos startPos, Predicate<BlockPos> stopAt){
        return getBlocksAbove((useless)->true, startPos, stopAt);
    }


    public static ArrayList<BlockPos> getBlocksBelow(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        BlockPos scanningBlock;
        scanningBlock = startPos.below();
        ArrayList<BlockPos> blocks = new ArrayList<>();
        while(scanningBlock.getY() >= MIN_HEIGHT){
            if(stopAt.test(scanningBlock)){
                return blocks;
            }
            if(target.test(scanningBlock)){
                blocks.add(scanningBlock);
            }
            scanningBlock = scanningBlock.below();
        }
        return blocks;
    }
    public static ArrayList<BlockPos> getBlocksBelow(Predicate<BlockPos> target, BlockPos startPos){
        return getBlocksBelow(target, startPos, (useless)->false);
    }
    public static ArrayList<BlockPos> getBlocksBelow(BlockPos startPos, Predicate<BlockPos> stopAt){
        return getBlocksBelow((useless)->true, startPos, stopAt);
    }

    public static ArrayList<BlockPos> getBlocksAboveAndBelow(Predicate<BlockPos> target, BlockPos startPos, Predicate<BlockPos> stopAt){
        ArrayList<BlockPos> blocks = new ArrayList<>();
        blocks.addAll(getBlocksBelow(target, startPos, stopAt));
        blocks.addAll(getBlocksAbove(target, startPos, stopAt));
        return blocks;
    }
    public static ArrayList<BlockPos> getBlocksAboveAndBelow(Predicate<BlockPos> target, BlockPos startPos){
        return getBlocksAboveAndBelow(target, startPos, (useless)->false);
    }
    public static ArrayList<BlockPos> getBlocksAboveAndBelow(BlockPos startPos, Predicate<BlockPos> stopAt){
        return getBlocksAboveAndBelow((useless)->true, startPos, stopAt);
    }

    public static ArrayList<BlockPos> getBlocksAdjacent_plane(Predicate<BlockPos> target, BlockPos scanningBlock){
        ArrayList<BlockPos> targetBlocks = new ArrayList<>();
        for(Direction direction : PLANE_DIRECTIONS){
            if(target.test(scanningBlock.relative(direction))){
                targetBlocks.add(scanningBlock.relative(direction));
            }
        }
        return targetBlocks;
    }

    /**
     * @return  返回pos对应方块x,z方向相邻的4个方块，以及这4个方块向上一格和向下一格的8个方块
     */
    public static ArrayList<BlockPos> getPossibleFloor(BlockPos pos){
        ArrayList<BlockPos> blocks = new ArrayList<>();
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.above()));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.below()));
        return blocks;
    }

    public ArrayList<BlockPos> getPossibleFloorNearLadder(BlockPos pos){
        ArrayList<BlockPos> blocks = new ArrayList<>();
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.above()));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.below()));
        blocks.addAll(getBlocksAdjacent_plane((blockPos)->true, pos.below(2)));
        return blocks;
    }

    /**
     * Find valid floor block near the block
     * @param isValidFloor determine if a block is valid floor
     */
    public ArrayList<BlockPos> getFloorAdjacent(BlockPos pos, Predicate<BlockPos> isValidFloor){
        ArrayList<BlockPos> possibleBlocks = getPossibleFloor(pos);
        ArrayList<BlockPos> floorAdjacent = new ArrayList<>();
        for(BlockPos possibleBlock : possibleBlocks){
            long key = possibleBlock.asLong();
            if(scannedBlocks.contains(key) || scanningBlocks.contains(key)) continue;
            if(isValidFloor.test(possibleBlock)) floorAdjacent.add(possibleBlock);
        }
        return floorAdjacent;
    }

    /**
     * 默认本身为完整方块，且上方两格为均空气的方块为合法的地板。若有不同需求请用上面那个方法
     */
    public ArrayList<BlockPos> getFloorAdjacent(BlockPos pos){
        return getFloorAdjacent(pos, (BlockPos)-> world.getBlockState(pos).isCollisionShapeFullBlock(world, pos) && world.getBlockState(pos.above()).isAir() && world.getBlockState(pos.above(2)).isAir());
    }

    public boolean isOpenAir(BlockPos pos){
        return countBlocksAbove(world ,pos, blockPos -> !world.getBlockState(blockPos).isAir()).result();
    }

    /**
     * 判断一个方块是否是空气/无碰撞箱方块/梯子
     */
    public static boolean isAirOrLadder(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.CLIMBABLE) || state.getCollisionShape(world, pos).isEmpty();
    }

    /**
     * @return return the first position of the block that makes target returns true
     */
    @Nullable
    public static BlockPos getBlockBelow(Predicate<BlockPos> target, BlockPos startPos){
        BlockPos scanningBlock;
        scanningBlock = startPos.below();
        while(scanningBlock.getY() >= MIN_HEIGHT){
            if(target.test(scanningBlock)){
                return scanningBlock;
            }
            else {
                scanningBlock = scanningBlock.below();
            }
        }
        return null;
    }

    public static HashSet<Long> toLongSet(Collection<BlockPos> collection){
        HashSet<Long> longSet = new HashSet<>();
        for(BlockPos pos : collection){
            longSet.add(pos.asLong());
        }
        return longSet;
    }

    public static HashSet<BlockPos> toPosSet(Collection<Long> collection){
        HashSet<BlockPos> posSet = new HashSet<>();
        for(Long posLong : collection){
            posSet.add(BlockPos.of(posLong));
        }
        return posSet;
    }

    public static ColumnPos toColumnPos(BlockPos pos){
        return new ColumnPos(pos.getX(), pos.getZ());
    }


    /**
     * 获取接下来要被扫描的方块。这个方法应在子类中重写，而非直接使用。
     * @return 接下来要被扫描的方块。这个Set不应包含scannedBlocks中已记录的内容及scanningBlocks中的内容。
     */
    protected abstract HashSet<BlockPos> nextScanningBlocks(BlockPos startPos);

    /**
     * 处理每个扫描到的方块。子类可以覆写此方法来实现特定的处理逻辑。
     * 默认实现为空，子类可以根据需要覆写。
     * @param pos 当前正在处理的方块位置
     */
    protected void processBlock(BlockPos pos) {
        // 默认实现为空，子类可以覆写
    }

    /**
     * 判断是否应该在指定位置停止扫描。
     * 默认实现检查isValid标志，子类可以覆写以实现更精细的控制。
     *
     * <p>设计意图：</p>
     * <ul>
     *   <li>{@code isValid}：表示建筑结构整体是否合法，是最终的判断结果</li>
     *   <li>{@code shouldStopAt}：提供扫描中途的控制能力，可以在不影响结构合法性的前提下提前停止
     *     <br>例如：扫描到一定数量的方块后就停止，因为已经满足需求
     *     <br>例如：发现某个特征后不需要继续扫描，但结构本身是合法的</li>
     * </ul>
     *
     * @param pos 当前正在处理的方块位置
     * @return true表示应该停止扫描，false表示继续扫描
     */
    protected boolean shouldStopAt(BlockPos pos) {
        // 默认实现：检查isValid
        return !this.isValid;
    }

    /**
     * 主扫描方法（模板方法）。定义扫描的完整流程，子类通过覆写钩子方法来定制行为。
     *
     * <p>scan方法的工作模式：</p>
     * 初始状态：在类创建完成后，scanningBlocks里会有一个blockPos，即startPos
     * 工作时，将scanningBlocks以long的形式放入scannedBlock，创建scanningBlocksNew集合，遍历scanningBlocks中的所有blockPos，然后对blockPos进行以下操作：
     * 1 检查isValid，如果为false则停止扫描
     * 2 检查shouldStopAt，如果返回true则停止扫描
     * 3 对blockPos执行processBlock操作
     * 4 对blockPos执行nextScanningBlocks方法，获取下一轮扫描的方块，然后存入scanningBlocksNew中
     * 在一轮scanningBlocks扫描结束后，把scanningBlocks换成scanningBlocksNew
     *
     * @return 扫描成功与否。如果scan完成了（scanningBlock为空），就会返回true；如果scan中途中断（触发了shouldStopAt，或是scanTimes达到了上限maxScanBlocks），则会返回false
     */
    public boolean scan() {
        int scannedBlocksNum = 0;

        while(!scanningBlocks.isEmpty() && isValid){
            // 先检查扫描次数限制
            if(scannedBlocksNum > maxScanBlocks){
                return false;
            }

            // 直接将 scanningBlocks 转移到 scannedBlocks
            for (long pos : scanningBlocks) {
                scannedBlocks.add(pos);
                scannedBlocksNum++;
            }
            scanningBlocksNew.clear();

            LongIterator iterator = scanningBlocks.iterator();
            while (iterator.hasNext()) {
                long posLong = iterator.nextLong();
                BlockPos scanningBlock = BlockPos.of(posLong);

                // 优先检查 isValid
                if (!this.isValid) {
                    return false;
                }

                // shouldStopAt 检查
                if (shouldStopAt(scanningBlock)) {
                    return false;
                }

                // 处理当前方块
                processBlock(scanningBlock);

                // 获取下一批位置
                HashSet<BlockPos> next = nextScanningBlocks(scanningBlock);
                for (BlockPos nextPos : next) {
                    long nextKey = nextPos.asLong();
                    if (!scannedBlocks.contains(nextKey) && !scanningBlocksNew.contains(nextKey)) {
                        scanningBlocksNew.add(nextKey);
                    }
                }
            }

            // 提前检查：如果没有新的扫描目标，提前退出
            if (scanningBlocksNew.isEmpty()) {
                break;
            }

            // 交换集合，避免重复创建
            LongSet temp = scanningBlocks;
            scanningBlocks = scanningBlocksNew;
            scanningBlocksNew = temp;
        }
        return this.isValid;
    }

}
