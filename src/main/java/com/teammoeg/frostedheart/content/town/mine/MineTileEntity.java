package com.teammoeg.frostedheart.content.town.mine;

import com.google.common.util.concurrent.AtomicDouble;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.MathUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MineTileEntity extends AbstractTownWorkerTileEntity{
    private int avgLightLevel;
    private int validStoneOrOre;
    private Map<TownResourceType, Double> resources;
    private double temperature;
    private double rating;

    public MineTileEntity(){
        super(FHTileTypes.MINE.get());
    }

    public boolean isStructureValid(){
        MineBlockScanner scanner = new MineBlockScanner(level, this.getBlockPos().above());
        if(scanner.scan()){
            this.avgLightLevel = scanner.getLight();
            this.validStoneOrOre = scanner.getValidStone();
            this.occupiedArea = scanner.getOccupiedArea();
            this.temperature = scanner.getTemperature();
            return validStoneOrOre > 0;
        }
        return false;
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
        this.rating = (lightRating * 0.3 + stoneRating * 0.3 + temperatureRating * 0.4) /* * (1 + 4 * this.linkedBaseRating)*/;
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
    public CompoundTag getWorkData() {
        CompoundTag nbt = getBasicWorkData();
        if(this.isValid()){
            nbt.putDouble("rating", this.rating);
            ListTag list = new ListTag();
            this.resources.forEach((type, amount) -> {
                CompoundTag nbt_1 = new CompoundTag();
                nbt_1.putString("type", type.getKey());
                nbt_1.putDouble("amount", amount);
                list.add(nbt_1);
            });
            nbt.put("resources", list);
        }
        return nbt;
    }

    @Override
    public void setWorkData(CompoundTag data) {
        this.setBasicWorkData(data);
    }


    public void refresh() {
        if(this.isOccupiedAreaOverlapped()){
            this.isStructureValid();
            return;
        }
        this.workerState = isStructureValid() ? TownWorkerState.VALID : TownWorkerState.NOT_VALID;
        if(this.isValid()) {
            assert this.level != null;
            if (this.resources == null || this.resources.isEmpty()) {
                ChunkTownResourceCapability capability = FHCapabilities.CHUNK_TOWN_RESOURCE.getCapability(this.level.getChunk(worldPosition)).orElseGet(ChunkTownResourceCapability::new);
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
            this.computeRating();
        }
    }

    public static void setLinkedBase(TownWorkerData mineData, BlockPos pos){
        mineData.setDataFromTown("linkedBasePos", LongTag.valueOf(pos.asLong()));
    }



    @Override
    public void readCustomNBT(CompoundTag compoundNBT, boolean b) {

    }

    @Override
    public void writeCustomNBT(CompoundTag compoundNBT, boolean b) {

    }

    public static class MineWorker implements TownWorker{
        @Override
        public boolean work(Town town, CompoundTag workData) {
            if(town instanceof TownWithResident){
                TeamTown teamTown = (TeamTown) town;
                CompoundTag dataTE = workData.getCompound("tileEntity");
                double rating = dataTE.getDouble("rating");
                ListTag list = dataTE.getList("resources", Constants.NBT.TAG_COMPOUND);
                EnumMap<TownResourceType, Double> resources = new EnumMap<>(TownResourceType.class);
                list.forEach(nbt -> {
                    CompoundTag nbt_1 = (CompoundTag) nbt;
                    String key = nbt_1.getString("type");
                    double amount = nbt_1.getDouble("amount");
                    resources.put(TownResourceType.from(key), amount);
                });
                List<Resident> residents = workData.getCompound("town").getList("residents", Constants.NBT.TAG_STRING)
                        .stream()
                        .map(nbt -> UUID.fromString(nbt.getString()))
                        .map(teamTown::getResident)
                        .map(optional -> optional.orElse(null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                for(Resident resident : residents){
                    double add = rating * resident.getWorkScore(TownWorkerType.MINE);
                    double randomDouble = MathUtils.RANDOM.nextDouble();
                    double counter = 0;
                    for(Map.Entry<TownResourceType, Double> entry : resources.entrySet()){
                        counter += entry.getValue();
                        if(counter >= randomDouble){
                            double actualAdd = town.add(entry.getKey(), add, false);
                            if(add != actualAdd) return false;
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

}
