package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.town.TownTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.util.BlockScanner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

import static com.teammoeg.frostedheart.util.BlockScanner.FloorBlockScanner.isHouseBlock;

public class WarehouseTileEntity extends FHBaseTileEntity implements TownTileEntity, ITickableTileEntity, FHBlockInterfaces.IActiveState {
    public int volume;//有效体积
    public int area;//占地面积
    public double capacity;//最大容量

    public WarehouseTileEntity() {
        super(FHTileTypes.WAREHOUSE.get());
    }

    public boolean isStructureValid(){
        BlockPos warehousePos = this.getPos();
        BlockPos doorPos = BlockScanner.getDoorAdjacent(world, warehousePos);
        if (doorPos == null) return false;
        BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(world).getBlockState(pos).isIn(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
        for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
            assert floorBelowDoor != null;
            BlockPos startPos = floorBelowDoor.offset(direction);//找到门下方块旁边的方块
            if (!BlockScanner.FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                if (!BlockScanner.FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos.down()) || isHouseBlock(world, startPos.up(2))) {//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                    continue;
                }
                startPos = startPos.down();
            }
            WarehouseBlockScanner scanner = new WarehouseBlockScanner(world, startPos);
            if(scanner.scan()){
                this.area = scanner.getArea();
                this.volume = scanner.getVolume();
                this.capacity = area*Math.pow((volume*0.02/area), 0.9)*37;
                return true;
            }
        }
        return false;
    }


    @Override
    public void readCustomNBT(CompoundNBT compoundNBT, boolean b) {
        //todo
    }

    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean b) {
        //todo
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.WAREHOUSE;
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
    public void tick() {

    }
}
