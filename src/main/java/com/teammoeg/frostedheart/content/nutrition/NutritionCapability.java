package com.teammoeg.frostedheart.content.nutrition;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class NutritionCapability implements NBTSerializable {

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        CompoundTag compound = new CompoundTag();
        compound.putFloat("vitamin", this.vitamin);
        compound.putFloat("carbohydrate", this.carbohydrate);
        compound.putFloat("protein", this.protein);
        compound.putFloat("vegetable", this.vegetable);
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        setVitamin(nbt.getFloat("vitamin"));
        setCarbohydrate(nbt.getFloat("carbohydrate"));
        setProtein(nbt.getFloat("protein"));
        setVegetable(nbt.getFloat("vegetable"));
    }

    private float vitamin = 10000.0f;
    private float carbohydrate = 10000.0f;
    private float protein = 10000.0f;
    private float vegetable = 10000.0f;

    public void addVitamin(Player player, float add) {
        this.vitamin += add;
        syncToClientOnRestore(player);
    }

    public void addCarbohydrate(Player player, float add) {
        this.carbohydrate += add;
        syncToClientOnRestore(player);
    }

    public void addProtein(Player player, float add) {
        this.protein += add;
        syncToClientOnRestore(player);
    }

    public void addVegetable(Player player, float add) {
        this.vegetable += add;
        syncToClientOnRestore(player);
    }

    public void setVitamin(float temp) {
        this.vitamin = temp;
    }

    public void setCarbohydrate(float temp) {
        this.carbohydrate = temp;
    }

    public void setProtein(float temp) {
        this.protein = temp;
    }

    public void setVegetable(float temp) {
        this.vegetable = temp;
    }

    public float getVitamin() {
        return vitamin;
    }

    public float getCarbohydrate() {
        return carbohydrate;
    }

    public float getProtein() {
        return protein;
    }

    public float getVegetable() {
        return vegetable;
    }

    public float getNutritionValue() {
        return vitamin + carbohydrate + protein + vegetable;
    }

    public float getVitaminPercentage() {
        return vitamin / getNutritionValue();
    }

    public float getCarbohydratePercentage() {
        return carbohydrate / getNutritionValue();
    }

    public float getProteinPercentage() {
        return protein / getNutritionValue();
    }

    public float getVegetablePercentage() {
        return vegetable / getNutritionValue();
    }

    public float getVitaminValue() {
        return vitamin / (getNutritionValue() / 4);
    }

    public float getCarbohydrateValue() {
        return carbohydrate / (getNutritionValue() / 4);
    }

    public float getProteinValue() {
        return protein / (getNutritionValue() / 4);
    }

    public float getVegetableValue() {
        return vegetable / (getNutritionValue() / 4);
    }


    public static void syncToClient(ServerPlayer player) {
        player.getCapability(FHCapabilities.PLAYER_WATER_LEVEL.capability()).ifPresent(t -> FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
    }

    public static void syncToClientOnRestore(Player player) {
        if (!player.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            syncToClient(serverPlayer);
        }
    }

    public void eat(Player player, ItemStack food) {
        //TODO
        CompoundTag tag = food.getOrCreateTag();
        eat(player, tag.getFloat("vitamin"), tag.getFloat("carbohydrate"), tag.getFloat("protein"), tag.getFloat("vegetable"));
    }


    public void eat(Player player, float vitamin, float carbohydrate, float protein, float vegetable) {
        addVitamin(player, vitamin);
        addCarbohydrate(player, carbohydrate);
        addProtein(player, protein);
        addVegetable(player, vegetable);
    }

    public void tick(Player player) {
        //TODO
        float consume = 0.1f;

        addVitamin(player, -getVitaminPercentage() * consume);
        addCarbohydrate(player, -getCarbohydratePercentage() * consume);
        addProtein(player, -getProteinPercentage() * consume);
        addVegetable(player, -getVegetablePercentage() * consume);
    }


    public void award(Player player) {
        //TODO
    }

    public void punishment(Player player) {
        //TODO
    }

    public static LazyOptional<NutritionCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_NUTRITION.getCapability(player);
    }
}
