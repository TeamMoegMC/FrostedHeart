package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.town.TownTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseBlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner.isHouseBlock;

public class MineBaseTileEntity extends FHBaseTileEntity implements TownTileEntity, ITickableTileEntity,
        FHBlockInterfaces.IActiveState{
    public Set<BlockPos> linkedMines = new HashSet<>();
    public int volume;
    public int area;
    public int rack;
    public int chest;
    public Set<ColumnPos> occupiedArea;

    public MineBaseTileEntity(){
        super(FHTileTypes.MINE_BASE.get());
    }

    public boolean isStructureValid(){
        BlockPos mineBasePos = this.getPos();
        BlockPos doorPos = BlockScanner.getDoorAdjacent(world, mineBasePos);
        if (doorPos == null) return false;
        BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
        for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
            assert floorBelowDoor != null;
            BlockPos startPos = floorBelowDoor.offset(direction);//找到门下方块旁边的方块
            if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos.down()) || isHouseBlock(world, startPos.up(2))) {//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                    continue;
                }
                startPos = startPos.down();
            }
            MineBaseBlockScanner scanner = new MineBaseBlockScanner(world, startPos);
            FHMain.LOGGER.info("New scanner created; Start pos: " + startPos);
            if(scanner.scan()){
                FHMain.LOGGER.info("scan successful");
                this.area = scanner.area;
                this.volume = scanner.volume;
                this.rack = scanner.rack;
                this.chest = scanner.chest;
                this.linkedMines = scanner.linkedMines;
                this.occupiedArea = scanner.occupiedArea;
                return true;
            }
        }
        return false;
    }

    @Override
    public void readCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }

    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorker() {
        return null;
    }

    @Override
    public boolean isWorkValid() {
        return this.isStructureValid();
    }

    @Override
    public CompoundNBT getWorkData() {
        return null;
    }

    @Override
    public void setWorkData(CompoundNBT data) {

    }

    @Override
    public Collection<ColumnPos> getOccupiedArea() {
        return this.occupiedArea;
    }

    @Override
    public void tick() {

    }
}
