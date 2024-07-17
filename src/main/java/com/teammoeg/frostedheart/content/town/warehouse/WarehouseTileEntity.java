package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerState;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

import static com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner.isHouseBlock;

public class WarehouseTileEntity extends AbstractTownWorkerTileEntity{
    private int volume;//有效体积
    private int area;//占地面积
    private double capacity;//最大容量
    private boolean addedToSchedulerQueue = false;

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
            if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                if (!FloorBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(world), startPos.down()) || isHouseBlock(world, startPos.up(2))) {//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                    continue;
                }
                startPos = startPos.down();
            }
            WarehouseBlockScanner scanner = new WarehouseBlockScanner(world, startPos);
            if(scanner.scan()){
                this.area = scanner.getArea();
                this.volume = scanner.getVolume();
                this.capacity = area*Math.pow((volume*0.02/area), 0.9)*37;
                this.occupiedArea = scanner.getOccupiedArea();
                return true;
            }
        }
        return false;
    }

    public void refresh(){
        if(this.isOccupiedAreaOverlapped()){
            this.isStructureValid();
        }
        else {
            this.setWorkerState(this.isStructureValid()?TownWorkerState.VALID: TownWorkerState.NOT_VALID);
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.WAREHOUSE;
    }


    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT nbt = getBasicWorkData();
        if(this.isValid()){
            nbt.putInt("area", this.area);
            nbt.putInt("volume", this.volume);
            nbt.putDouble("capacity", this.capacity);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        this.setBasicWorkData(data);
        if(this.isValid()){
            this.area = data.getInt("area");
            this.volume = data.getInt("volume");
            this.capacity = data.getDouble("capacity");
        }
    }

    public int getVolume(){
        return this.isWorkValid()?this.volume:0;
    }
    public int getArea(){
        return this.isWorkValid()?this.area:0;
    }
    public double getCapacity(){
        return this.isWorkValid()?this.capacity:0;
    }

    @Override
    public void tick() {
        if(!this.addedToSchedulerQueue){
            SchedulerQueue.add(this);
            this.addedToSchedulerQueue = true;
        }
    }

}
