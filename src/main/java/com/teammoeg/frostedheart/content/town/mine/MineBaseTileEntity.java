package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.base.scheduler.ScheduledTaskTileEntity;
import com.teammoeg.frostedheart.base.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.content.town.TownTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.teammoeg.frostedheart.content.town.house.HouseTileEntity.*;
import static com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner.isHouseBlock;
import static java.lang.Math.exp;
import static net.minecraftforge.common.util.Constants.NBT.TAG_LONG;

public class MineBaseTileEntity extends FHBaseTileEntity implements TownTileEntity, ITickableTileEntity,
        FHBlockInterfaces.IActiveState, ScheduledTaskTileEntity {
    public Set<BlockPos> linkedMines = new HashSet<>();
    private int volume;
    private int area;
    private int rack;
    private int chest;
    public Set<ColumnPos> occupiedArea = new HashSet<>();
    private double temperature;
    private double rating;
    private byte isValid = -1;//-1:not init;0:false;1:true
    private boolean addedToSchedulerQueue = false;

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
                this.temperature = scanner.getTemperature();
                return true;
            }
        }
        return false;
    }

    public double computeRating() {
        double rackRating = 1 - exp(-this.rack);
        double chestRating = 1 - exp(-this.chest * 0.4);
        double spaceRating = HouseTileEntity.calculateSpaceRating(this.volume, this.area);
        double temperatureRating = HouseTileEntity.calculateTemperatureRating(this.temperature);
        return this.rating = spaceRating*0.15 + temperatureRating*0.15 + chestRating*0.35 + rackRating*0.35;
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
        if(this.isValid == -1) this.refresh();
        return this.isValid == 1;
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT nbt = new CompoundNBT();
        nbt.putByte("isValid", this.isValid);
        if(this.isValid == 1) {
            nbt.putInt("volume", this.volume);
            nbt.putInt("area", this.area);
            nbt.putInt("rack", this.rack);
            nbt.putInt("chest", this.chest);
            ListNBT list = new ListNBT();
            for (BlockPos pos : this.linkedMines) {
                list.add(LongNBT.valueOf(pos.toLong()));
            }
            nbt.put("linkedMines", list);
            nbt.putDouble("temperature", this.temperature);
            nbt.putDouble("rating", this.rating);
            nbt.put(TAG_NAME_OCCUPIED_AREA, serializeOccupiedArea(this.occupiedArea));
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        this.isValid = data.getByte("isValid");
        if(isValid == 1) {
            this.volume = data.getInt("volume");
            this.area = data.getInt("area");
            this.rack = data.getInt("rack");
            this.chest = data.getInt("chest");
            this.linkedMines.clear();
            ListNBT list = data.getList("linkedMines", TAG_LONG);
            list.forEach(nbt->{
                this.linkedMines.add( BlockPos.fromLong( ((LongNBT)nbt).getLong() ));
            });
            this.occupiedArea = deserializeOccupiedArea(data);
        }
    }

    @Override
    public Collection<ColumnPos> getOccupiedArea() {
        this.isWorkValid();
        return this.occupiedArea;
    }
    public double getRating(){
        if(isWorkValid()) {
            if (this.rating == 0) return this.computeRating();
            return this.rating;
        }
        return 0;
    }
    public int getVolume(){
        return this.isWorkValid()?this.volume:0;
    }
    public Set<BlockPos> getLinkedMines() {
        return this.linkedMines;
    }
    public int getArea() {
        return this.isWorkValid() ? this.area : 0;
    }
    public int getRack() {
        return this.isWorkValid() ? this.rack : 0;
    }
    public int getChest() {
        return this.isWorkValid() ? this.chest : 0;
    }
    public double getTemperature() {
        return this.isWorkValid() ? this.temperature : 0;
    }

    public void setLinkedBaseToAllLinkedMines(){
        if(this.isWorkValid()) {
            for(BlockPos minePos:this.linkedMines){
                assert world != null;
                MineTileEntity mineTileEntity = (MineTileEntity) world.getTileEntity(minePos);
                if(mineTileEntity!=null)
                    mineTileEntity.setLinkedBase(this.getPos(),this.getRating());
            }
        }
    }


    public void refresh() {
        this.isValid = (byte)(this.isStructureValid()?1:0);
    }

    @Override
    public void tick() {
        if(!this.addedToSchedulerQueue){
            SchedulerQueue.add(this);
            this.addedToSchedulerQueue = true;
        }
    }

    // ScheduledTaskTileEntity
    @Override
    public void executeTask() {
        this.refresh();
        this.setLinkedBaseToAllLinkedMines();
    }
    @Override
    public boolean isStillValid() {
        return this.isWorkValid();
    }
}
