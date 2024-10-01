package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerState;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongArrayNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner.isHouseBlock;
import static java.lang.Math.exp;
import static net.minecraftforge.common.util.Constants.NBT.TAG_LONG;

public class MineBaseTileEntity extends AbstractTownWorkerTileEntity {
    public Set<BlockPos> linkedMines = new HashSet<>();
    private int volume;
    private int area;
    private int rack;
    private int chest;
    private double temperature;
    private double rating;

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
            if(scanner.scan()){
                this.area = scanner.getArea();
                this.volume = scanner.getVolume();
                this.rack = scanner.getRack();
                this.chest = scanner.getChest();
                this.linkedMines = scanner.getLinkedMines();
                this.occupiedArea = scanner.getOccupiedArea();
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
    public TownWorkerType getWorkerType() {
        return TownWorkerType.MINE_BASE;
    }


    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT nbt = getBasicWorkData();
        if(this.isValid()) {
            ListNBT list = new ListNBT();
            for (BlockPos pos : this.linkedMines) {
                list.add(LongNBT.valueOf(pos.toLong()));
            }
            nbt.put("linkedMines", list);
            nbt.putDouble("rating", this.rating);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        setBasicWorkData(data);
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


    //由于需要寻找Mine，MineBase可能需要扫描更远的位置，因此设置需要加载的区块更远。
    @Override
    public void refresh_safe(){
        if(world != null && world.isAreaLoaded(pos,63)){
            this.refresh();
        }
    }

    public void refresh() {
        if(this.isOccupiedAreaOverlapped()){
            this.isStructureValid();
            return;
        }
        this.workerState = isStructureValid() ? TownWorkerState.VALID : TownWorkerState.NOT_VALID;
    }

    @Override
    public boolean isStillValid() {
        return this.isWorkValid();
    }
}
