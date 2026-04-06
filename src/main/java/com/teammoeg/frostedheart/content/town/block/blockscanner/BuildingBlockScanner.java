package com.teammoeg.frostedheart.content.town.block.blockscanner;

import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import static com.teammoeg.frostedheart.content.town.block.blockscanner.AbstractBlockScanner.DEFAULT_MAX_SCAN_BLOCKS;

/**
 * 大多数城镇建筑通用的BlockScanner，用以扫描建筑的结构。
 * 结构要求城镇放在门边，门连着一个封闭的区域。通常来说是一个密闭的房屋。
 * <p>
 * BuildingBlockScanner属于FloorBlockScanner和ConfinedSpaceScanner的组合使用，不继承AbstractBlockScanner。
 */
public class BuildingBlockScanner {
    @Getter
    public int area = 0;
    @Getter
    public int volume = 0;
    protected final BlockPos startPos;
    protected final Level world;
    @Getter
    public boolean isValid = true;
    @Getter
    public OccupiedVolume occupiedVolume = new OccupiedVolume();
    /**
     * FloorBlockScanner标记扫描过的空气方块，这样在扫描空气时就不需要再从世界获取了。
     */
    public LongSet airs = new LongOpenHashSet();

    public BuildingBlockScanner(Level world, BlockPos startPos) {
        this.startPos = startPos;
        this.world = world;
    }

    //下面两个方法实际上都是ConfinedSpaceScanner中可重写的方法。
    // 我在这个类的ConfinedSpaceScanner中直接调用下面的两个方法，这样子类想修改的话就不用再弄一个ConfinedSpaceScanner的子类了。
    /**
     * 通常可重写此方法以处理温度信息等
     * @param pos 空气方块的位置
     */
    protected void processBuildingAirBlock(BlockPos pos) {
    }
    /**
     * 通常可重写此方法以处理装饰物等方块。
     * @param pos 非空气方块的位置
     */
    protected void processBuildingNonAirBlock(BlockPos pos) {
    }

    //不是AbstractBlockScanner的scan方法，但使用上和返回值是类似的。
    public boolean scan(){
        BuildingFloorBlockScanner floorBlockScanner = new BuildingFloorBlockScanner(world, startPos, true, DEFAULT_MAX_SCAN_BLOCKS / 4);//地板的扫描要检测比看起来更多的方块，maxScanBlocks弄少一点
        if(!floorBlockScanner.scan()){
            this.isValid = false;
            this.occupiedVolume = OccupiedVolume.EMPTY;
            return false;
        }
        BuildingConfinedSpaceScanner confinedSpaceScanner = new BuildingConfinedSpaceScanner(world, startPos.above(), DEFAULT_MAX_SCAN_BLOCKS);
        if(!confinedSpaceScanner.scan()){
            this.isValid = false;
            this.occupiedVolume = OccupiedVolume.EMPTY;
            return false;
        }
        return true;
    }

    protected class BuildingFloorBlockScanner extends FloorBlockScanner {
        public BuildingFloorBlockScanner(Level world, BlockPos startPos, boolean canUseLadder, int maxScanBlocks) {
            super(world, startPos, canUseLadder, maxScanBlocks);
        }

        /**
         * 标记所有空气的的位置，这样后面扫描空气就能免去很多检测。
         */
        public boolean isFloorBlock(BlockPos pos) {
            if(isAirOrLadder(world, pos)){
                airs.add(pos.asLong());
            }
            return super.isFloorBlock(pos);
        }

        @Override
        protected void processBlock(BlockPos pos) {
            area++;
        }
    }

    protected class BuildingConfinedSpaceScanner extends ConfinedSpaceScanner {

        public BuildingConfinedSpaceScanner(Level world, BlockPos startPos, int maxScanBlocks) {
            super(world, startPos, maxScanBlocks);
        }

        @Override
        protected boolean isValidAir(BlockPos pos) {
            if(airs.contains(pos.asLong())){
                return true;
            }
            return super.isValidAir(pos);
        }

        /**
         * 通常可重写此方法以处理温度信息
         * @param pos 空气方块的位置
         */
        @Override
        protected void processAirBlock(BlockPos pos) {
            processBuildingAirBlock(pos);
            volume++;
            occupiedVolume.add(pos);
        }

        /**
         * 通常可重写此方法以处理装饰物等方块。
         * @param pos 非空气方块的位置
         */
        @Override
        protected void processNonAirBlock(BlockPos pos) {
            processBuildingNonAirBlock(pos);
        }
    }
}
