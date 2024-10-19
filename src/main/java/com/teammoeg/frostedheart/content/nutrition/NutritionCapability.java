package com.teammoeg.frostedheart.content.nutrition;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.water.PlayerWaterLevelSyncPacket;
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
        compound.putFloat("fruit", this.fruit);
        compound.putFloat("grain", this.grain);
        compound.putFloat("protein", this.protein);
        compound.putFloat("vegetable", this.vegetable);
        compound.putFloat("sugar", this.sugar);
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        setFruit(nbt.getFloat("fruit"));
        setGrain(nbt.getFloat("grain"));
        setProtein(nbt.getFloat("protein"));
        setVegetable(nbt.getFloat("vegetable"));
        setSugar(nbt.getFloat("sugar"));
    }

    private float fruit = 1.0f;
    private float grain = 1.0f;
    private float protein = 1.0f;
    private float vegetable = 1.0f;
    private float sugar = 1.0f;

    public void addFruit(Player player, float add) {
        this.fruit = Math.min(this.fruit + add, 1.0f);
        syncToClientOnRestore(player);
    }

    public void addGrain(Player player, float add) {
        this.grain = Math.min(this.grain + add, 1.0f);
        syncToClientOnRestore(player);
    }

    public void addProtein(Player player, float add) {
        this.protein = Math.min(this.protein + add, 1.0f);
        syncToClientOnRestore(player);
    }

    public void addVegetable(Player player, float add) {
        this.vegetable = Math.min(this.vegetable + add, 1.0f);
        syncToClientOnRestore(player);
    }

    public void addSugar(Player player, float add) {
        this.sugar = Math.min(this.sugar + add, 20);
        syncToClientOnRestore(player);
    }

    public void setFruit(float temp) {
        this.fruit = temp;
    }

    public void setGrain(float temp) {
        this.grain = temp;
    }

    public void setProtein(float temp) {
        this.protein = temp;
    }

    public void setVegetable(float temp) {
        this.vegetable = temp;
    }

    public void setSugar(float temp) {
        this.sugar = temp;
    }

    public float getFruit() {
        return fruit;
    }

    public float getGrain() {
        return grain;
    }

    public float getProtein() {
        return protein;
    }

    public float getVegetable() {
        return vegetable;
    }

    public float getSugar() {
        return sugar;
    }

    public static void syncToClient(ServerPlayer player) {
        player.getCapability(FHCapabilities.PLAYER_WATER_LEVEL.capability()).ifPresent(t -> FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
    }

    public static void syncToClientOnRestore(Player player) {
        if (!player.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            //CriteriaTriggerRegistry.WATER_LEVEL_RESTORED_TRIGGER.trigger(serverPlayer);
            syncToClient(serverPlayer);
        }
    }
    public void eat(Player player, ItemStack food) {
        //TODO
        CompoundTag tag = food.getOrCreateTag();
        eat(player, tag.getFloat("fruit"), tag.getFloat("grain"), tag.getFloat("protein"), tag.getFloat("vegetable"), tag.getFloat("sugar"));
    }


    public void eat(Player player, float fruit, float grain, float protein, float vegetable, float sugar) {
        addFruit(player, fruit);
        addGrain(player, grain);
        addProtein(player, protein);
        addVegetable(player, vegetable);
        addSugar(player, sugar);
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
