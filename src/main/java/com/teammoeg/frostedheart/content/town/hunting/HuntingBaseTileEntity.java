package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.FHBlockEntityTypes;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatConsumerEndpoint;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.house.HouseBlockScanner;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.FloorBlockScanner;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.util.*;

import java.util.AbstractMap.SimpleEntry;

public class HuntingBaseTileEntity extends AbstractTownWorkerTileEntity {
    private double rating = 0;
    private int volume;
    private int area;
    private int chestNum;
    private int bedNum;
    private int tanningRackNum;
    private double temperature;
    private Map<String, Integer> decorations;
    HeatConsumerEndpoint endpoint = new HeatConsumerEndpoint(99,10,1);
    LazyOptional<HeatConsumerEndpoint> endpointCap = LazyOptional.of(()-> endpoint);
    private double temperatureModifier = 0;
    private int maxResident;

    public HuntingBaseTileEntity(BlockPos pos,BlockState state) {
        super(FHBlockEntityTypes.HUNTING_BASE.get(),pos,state);
    }

    public double getRating(){
        return rating;
    }
    //get volume
    public int getVolume() {
        return volume;
    }
    //get area
    public int getArea() {
        return area;
    }
    //get chest num
    public int getChestNum() {
        return chestNum;
    }
    //get bed num
    public int getBedNum() {
        return bedNum;
    }
    //get tanning rack num
    public int getTanningRackNum() {
        return tanningRackNum;
    }
    //get temperature
    public double getTemperature() {
        return temperature;
    }
    //get max resident
    public int getMaxResident() {
        return maxResident;
    }


    public boolean isStructureValid() {
        BlockPos housePos = this.getBlockPos();
        List<BlockPos> doorPosSet = BlockScanner.getBlocksAdjacent(housePos, (pos) -> Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS));
        if (doorPosSet.isEmpty()) return false;
        for (BlockPos doorPos : doorPosSet) {
            BlockPos floorBelowDoor = BlockScanner.getBlockBelow((pos)->!(Objects.requireNonNull(level).getBlockState(pos).is(BlockTags.DOORS)), doorPos);//找到门下面垫的的那个方块
            for (Direction direction : BlockScanner.PLANE_DIRECTIONS) {
                assert floorBelowDoor != null;
                BlockPos startPos = floorBelowDoor.relative(direction);//找到门下方块旁边的方块
                if (!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos)) {//如果门下方块旁边的方块不是合法的地板，找一下它下面的方块
                    if(!HouseBlockScanner.isValidFloorOrLadder(Objects.requireNonNull(level), startPos.below()) || FloorBlockScanner.isHouseBlock(level, startPos.above(2))){//如果它下面的方块也不是合法地板（或者梯子），或者门的上半部分堵了方块，就不找了。我们默认村民不能从两格以上的高度跳下来，也不能从一格高的空间爬过去
                        continue;
                    }
                    startPos = startPos.below();
                }
                HuntingBaseBlockScanner scanner = new HuntingBaseBlockScanner(this.level, startPos);
                if (scanner.scan()) {
                    this.volume = scanner.getVolume();
                    this.area = scanner.getArea();
                    this.decorations = scanner.getDecorations();
                    this.temperature = scanner.getTemperature();
                    this.occupiedArea = scanner.getOccupiedArea();
                    this.chestNum = scanner.getChestNum();
                    this.bedNum = scanner.getBedNum();
                    this.tanningRackNum = scanner.getTanningRackNum();
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isTemperatureValid(){
        double effective = temperature + temperatureModifier;
        return effective >= HouseTileEntity.MIN_TEMP_HOUSE && effective <= HouseTileEntity.MAX_TEMP_HOUSE;
    }

    public double getTemperatureModifier() {
        return isWorkValid() ? this.temperatureModifier : 0;
    }

    public double getEffectiveTemperature() {
        return temperature + temperatureModifier;
    }

    private double computeRating() {
        if(this.isValid()){
            return (HouseTileEntity.calculateSpaceRating(this.volume, this.area) * (2 + HouseTileEntity.calculateDecorationRating(this.decorations, this.area))
                    + 2 * HouseTileEntity.calculateTemperatureRating(this.temperature + this.temperatureModifier) +
                    (1-Math.exp(-this.maxResident - chestNum)) ) / 6;
        }
        else return 0;
    }

    private int calculateMaxResidents(){
        if(this.isValid()){
            return Math.min((int)(HouseTileEntity.calculateSpaceRating(this.volume, this.area) / 16 * this.area), Math.min(this.tanningRackNum, this.bedNum));
        }
        else return 0;
    }

    @Override
    public void tick() {
        assert level != null;
        if (!level.isClientSide) {
            if (endpoint.tryDrainHeat(1)) {
                temperatureModifier = Math.max(endpoint.getTemperatureLevel() * 10, HouseTileEntity.COMFORTABLE_TEMP_HOUSE);
                if (setActive(true)) {
                    setChanged();
                }
            } else {
                temperatureModifier = 0;
                if (setActive(false)) {
                    setChanged();
                }
            }
        } else if (getIsActive()) {
            ClientUtils.spawnSteamParticles(level, worldPosition);
        }
        this.addToSchedulerQueue();
    }

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if(capability== FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
            return endpointCap.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void refresh() {
        if (this.isOccupiedAreaOverlapped()) {
            this.isStructureValid();
            this.isTemperatureValid();
        } else {
            this.workerState = this.isStructureValid() && this.isTemperatureValid() ? TownWorkerState.VALID : TownWorkerState.NOT_VALID;
            this.rating = this.computeRating();
            this.maxResident = this.calculateMaxResidents();
        }
    }


    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.HUNTING_BASE;
    }

    @Override
    public CompoundTag getWorkData() {
        CompoundTag nbt = getBasicWorkData();
        if(this.isValid()){
            nbt.putDouble("rating",this.rating);
            nbt.putInt("maxResident",this.maxResident);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundTag data) {
        this.setBasicWorkData(data);
    }

    public static class HuntingBaseWorker implements TownWorker {
        @Override
        public boolean work(Town town, CompoundTag workData) {
            if(town instanceof TeamTown){//the town must be team town because it needs to get all camps in the town.
                TreeSet<SimpleEntry<TownWorkerData,Double>> camps = new TreeSet<>(Comparator.comparingDouble(AbstractMap.SimpleEntry::getValue));//Double: rating
                ArrayList<TownWorkerData> campsUnchecked = new ArrayList<>();
                TeamTown teamTown = (TeamTown) town;
                teamTown.getTownBlocks().values().forEach(
                        (TownWorkerData data)->{
                            if(data.getType()==TownWorkerType.HUNTING_CAMP){
                                campsUnchecked.add(data);
                            }
                        });
                if(campsUnchecked.size() > 0){
                    double baseRating = workData.getDouble("rating");
                    for(TownWorkerData data : campsUnchecked){
                        SimpleEntry<TownWorkerData,Double> dataPair = new SimpleEntry<>(data, data.getWorkData().getDouble("rating") * baseRating * 2);//double: 考虑到与之距离过近的camp之后新计算的rating    *2:下面遍历的时候会遍历到它自己
                        for(TownWorkerData data2 : campsUnchecked){
                            if(data2.getPos().distSqr(data.getPos()) < 128) dataPair.setValue( dataPair.getValue() * (0.5 + 0.5 * (data2.getPos().distSqr(data.getPos())) / 128));
                        }
                        camps.add(dataPair);
                    }
                }
                int residentsLeft = workData.getInt("maxResident");
                if(camps.size() > 0 && residentsLeft > 0){
                    Iterator<SimpleEntry<TownWorkerData, Double>> iterator = camps.iterator();
                    while(iterator.hasNext() && residentsLeft > 0){
                        double add = iterator.next().getValue();
                        double realAdd = town.add(TownResourceType.RAW_FOOD, add, true);
                        if(add - realAdd > 0.001) return false;
                        residentsLeft--;
                    }
                }
            }
            return false;
        }
    }
}
