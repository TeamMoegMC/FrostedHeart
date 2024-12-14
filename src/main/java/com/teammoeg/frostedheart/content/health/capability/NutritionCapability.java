package com.teammoeg.frostedheart.content.health.capability;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.health.network.PlayerNutritionSyncPacket;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class NutritionCapability implements NBTSerializable {

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        CompoundTag compound = new CompoundTag();
        compound.putFloat("fat", this.fat);
        compound.putFloat("carbohydrate", this.carbohydrate);
        compound.putFloat("protein", this.protein);
        compound.putFloat("vegetable", this.vegetable);
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        setFat(nbt.getFloat("fat"));
        setCarbohydrate(nbt.getFloat("carbohydrate"));
        setProtein(nbt.getFloat("protein"));
        setVegetable(nbt.getFloat("vegetable"));
    }

    private float fat = 0.0f;
    private float carbohydrate = 0.0f;
    private float protein = 0.0f;
    private float vegetable = 0.0f;

    public void addFat(Player player, float add) {
        this.fat += add;
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

    public void setFat(float temp) {
        this.fat = temp;
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

    public float getFat() {
        return fat;
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
        return fat + carbohydrate + protein + vegetable;
    }

    public float getFatPercentage() {
        return fat / getNutritionValue();
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

    public float getFatValue() {
        return fat / (getNutritionValue() / 4);
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
        Level level = player.level();
        NutritionRecipe wRecipe = NutritionRecipe.getRecipeFromItem(level, food);
        if (wRecipe != null) {
            NutritionCapability.getCapability(player).ifPresent(data -> {
                data.addCarbohydrate(player, wRecipe.carbohydrate);
                data.addProtein(player, wRecipe.protein);
                data.addVegetable(player, wRecipe.vegetable);
                data.addFat(player, wRecipe.fat);
            });
        }
        if(player instanceof ServerPlayer serverPlayer){
            NutritionCapability.getCapability(serverPlayer).ifPresent(data -> {
                FHNetwork.sendPlayer(serverPlayer, new PlayerNutritionSyncPacket(data.fat, data.carbohydrate, data.protein, data.vegetable));
            });
        }
    }


    public void eat(Player player, float fat, float carbohydrate, float protein, float vegetable) {
        addFat(player, fat);
        addCarbohydrate(player, carbohydrate);
        addProtein(player, protein);
        addVegetable(player, vegetable);
        if(player instanceof ServerPlayer serverPlayer)
            FHNetwork.sendPlayer(serverPlayer, new PlayerNutritionSyncPacket(this.fat, this.carbohydrate, this.protein, this.vegetable));
    }

    public void tick(Player player) {
        //TODO
        float consume = 0.1f;

        addFat(player, -getFatPercentage() * consume);
        addCarbohydrate(player, -getCarbohydratePercentage() * consume);
        addProtein(player, -getProteinPercentage() * consume);
        addVegetable(player, -getVegetablePercentage() * consume);
        if(player instanceof ServerPlayer serverPlayer)
            FHNetwork.sendPlayer(serverPlayer, new PlayerNutritionSyncPacket(this.fat, this.carbohydrate, this.protein, this.vegetable));
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
