package com.teammoeg.frostedheart.content.town.mine;

import com.google.common.util.concurrent.AtomicDouble;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.town.ChunkTownResourceCapability;
import com.teammoeg.frostedheart.content.town.TownResourceType;
import com.teammoeg.frostedheart.content.town.TownTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.math.ColumnPos;
import net.minecraftforge.common.util.Constants;

import java.util.*;
import java.util.stream.Stream;

public class MineTileEntity extends FHBaseTileEntity implements TownTileEntity, ITickableTileEntity,
        FHBlockInterfaces.IActiveState{
    private int avgLightLevel;
    private int validStoneOrOre;
    private double linkedBaseRating = 0;
    private Set<ColumnPos> occupiedArea;
    private byte isValid = -1;
    private Map<TownResourceType, Double> resources;
    private double temperature;
    private double rating;

    public MineTileEntity(){
        super(FHTileTypes.MINE.get());
    }

    public boolean isStructureValid(){
        MineBlockScanner scanner = new MineBlockScanner(world, this.getPos().up());
        if(scanner.scan()){
            this.avgLightLevel = scanner.light;
            this.validStoneOrOre = scanner.validStone;
            this.occupiedArea = scanner.occupiedArea;
            this.temperature = scanner.temperature;
            return validStoneOrOre > 0;
        }
        return false;
    }

    public void setLinkedBaseRating(double rating) {
        this.linkedBaseRating = rating;
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
    public TownWorkerType getWorker() {
        return TownWorkerType.MINE;
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
        if(this.isValid == 1){
            nbt.putDouble("linkedBaseRating", this.linkedBaseRating);
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
        this.isValid = data.getByte("isValid");
        if(this.isValid == 1){
            this.linkedBaseRating = data.getDouble("linkedBaseRating");
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

    @Override
    public Collection<ColumnPos> getOccupiedArea() {
        return this.occupiedArea;
    }

    public void refresh() {
        this.isValid = (byte) (this.isStructureValid() ? 1 : 0);
        if(this.isValid == 1) {
            if (this.resources == null || this.resources.isEmpty()) {
                assert this.world != null;
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
            this.computeRating();
        }
    }

    @Override
    public void tick() {

    }

    @Override
    public void readCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }

    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }
}
