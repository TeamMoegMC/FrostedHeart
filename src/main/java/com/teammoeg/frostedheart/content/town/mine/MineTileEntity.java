package com.teammoeg.frostedheart.content.town.mine;

import com.google.common.util.concurrent.AtomicDouble;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.scheduler.SchedulerQueue;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.util.*;
import java.util.stream.Stream;

public class MineTileEntity extends AbstractTownWorkerTileEntity{
    private int avgLightLevel;
    private int validStoneOrOre;
    private boolean hasLinkedBase = false;
    private BlockPos linkedBasePos;
    private double linkedBaseRating = 0;
    private Map<TownResourceType, Double> resources;
    private double temperature;
    private double rating;
    private boolean addedToSchedulerQueue = false;

    public MineTileEntity(){
        super(FHTileTypes.MINE.get());
    }

    public boolean isStructureValid(){
        MineBlockScanner scanner = new MineBlockScanner(world, this.getPos().up());
        if(scanner.scan()){
            this.avgLightLevel = scanner.getLight();
            this.validStoneOrOre = scanner.getValidStone();
            this.occupiedArea = scanner.getOccupiedArea();
            this.temperature = scanner.getTemperature();
            return validStoneOrOre > 0;
        }
        return false;
    }

    public void setLinkedBase(BlockPos basePos) {
        assert world != null;
        if(world.getTileEntity(basePos) instanceof MineBaseTileEntity){
            this.setLinkedBase(basePos,  ((MineBaseTileEntity) Objects.requireNonNull(world.getTileEntity(basePos))).getRating());
        }
    }
    public void setLinkedBase(BlockPos basePos, double baseRating){
        this.linkedBasePos = basePos;
        this.hasLinkedBase = true;
        this.linkedBaseRating = baseRating;
    }

    public double getRating() {
        if(this.isWorkValid()) return this.rating;
        else return 0;
    }
    public int getAvgLightLevel() {
        if(this.isWorkValid()) return this.avgLightLevel;
        else return 0;
    }
    public int getValidStoneOrOre() {
        if(this.isWorkValid()) return this.validStoneOrOre;
        else return 0;
    }

    public void computeRating(){
        double lightRating = 1 - Math.exp(-this.avgLightLevel);
        double stoneRating = Math.min(this.validStoneOrOre / 255.0F, 1);
        double temperatureRating = HouseTileEntity.calculateTemperatureRating(this.temperature);
        this.rating = (lightRating * 0.3 + stoneRating * 0.3 + temperatureRating * 0.4) * (1 + 4 * this.linkedBaseRating);
    }


    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.MINE;
    }

    @Override
    public CompoundNBT getWorkData() {
        CompoundNBT nbt = getBasicWorkData();
        nbt.putBoolean("hasLinkedBase", this.hasLinkedBase);
        if(this.hasLinkedBase){
            nbt.putLong("linkedBasePos", this.linkedBasePos.toLong());
            nbt.putDouble("linkedBaseRating", this.linkedBaseRating);
        }
        if(this.isValid()){
            nbt.putDouble("temperature", this.temperature);
            nbt.putDouble("rating", this.rating);
            ListNBT list = new ListNBT();
            this.resources.forEach((type, amount) -> {
                CompoundNBT nbt_1 = new CompoundNBT();
                nbt_1.putString("type", type.getKey());
                nbt_1.putDouble("amount", amount);
                list.add(nbt_1);
            });
            nbt.put("resources", list);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        this.setBasicWorkData(data);
        this.hasLinkedBase = data.getBoolean("hasLinkedBase");
        if(this.hasLinkedBase){
            this.linkedBasePos = BlockPos.fromLong(data.getLong("linkedBasePos"));
            this.linkedBaseRating = data.getDouble("linkedBaseRating");
        }

        if(this.isValid()){
            this.rating = data.getDouble("rating");
            ListNBT list = data.getList("resources", Constants.NBT.TAG_COMPOUND);
            this.resources = new EnumMap<>(TownResourceType.class);
            list.forEach(nbt -> {
                CompoundNBT nbt_1 = (CompoundNBT) nbt;
                String key = nbt_1.getString("type");
                double amount = nbt_1.getDouble("amount");
                this.resources.put(TownResourceType.from(key), amount);
            });
        }
    }


    public void refresh() {
        if(this.isOccupiedAreaOverlapped()){
            this.isStructureValid();
            return;
        }
        this.workerState = isStructureValid() ? TownWorkerState.VALID : TownWorkerState.NOT_VALID;
        if(this.isValid()) {
            assert this.world != null;
            if (this.resources == null || this.resources.isEmpty()) {
                ChunkTownResourceCapability capability = FHCapabilities.CHUNK_TOWN_RESOURCE.getCapability(this.world.getChunk(pos)).orElseGet(ChunkTownResourceCapability::new);
                AtomicDouble totalResources = new AtomicDouble(0);
                this.resources = new HashMap<>();
                Stream.of(ChunkTownResourceCapability.ChunkTownResourceType.values())
                        .filter(type -> capability.getOrGenerateAbundance(type) > 0)//移除丰度为0的
                        .map(type -> {//获取资源的相对含量
                            int abundance = capability.getOrGenerateAbundance(type);
                            totalResources.addAndGet(abundance);
                            return new AbstractMap.SimpleEntry<>(type, (double) capability.getOrGenerateAbundance(type));
                        }).forEach(pair -> {//将相对含量存入map
                            resources.put(pair.getKey().getType(), pair.getValue() / totalResources.get());
                        });
            }
            if(this.hasLinkedBase){
                if(world.getTileEntity(this.linkedBasePos) instanceof MineBaseTileEntity){
                    this.linkedBaseRating = ((MineBaseTileEntity) Objects.requireNonNull(world.getTileEntity(this.linkedBasePos))).getRating();
                } else this.hasLinkedBase = false;
            }
            this.computeRating();
        }
    }

    @Override
    public void tick() {
        if(!this.addedToSchedulerQueue){
            SchedulerQueue.add(this);
            this.addedToSchedulerQueue = true;
        }

    }

    @Override
    public void readCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }

    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }
}
