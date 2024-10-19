package com.teammoeg.frostedheart.content.nutrition;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.water.PlayerWaterLevelSyncPacket;
import com.teammoeg.frostedheart.util.io.NBTSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class NutritionCapability implements NBTSerializable {

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        CompoundTag compound = new CompoundTag();
        compound.putFloat("fruit", this.fruit);
        compound.putFloat("grain", this.grain);
        compound.putFloat("protein", this.portein);
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
    private float portein = 1.0f;
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
        this.portein = Math.min(this.portein + add, 1.0f);
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
        this.portein = temp;
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
        return portein;
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

    public void award(Player player) {
        FoodData foodData = player.getFoodData();

    }

    public void punishment(Player player) {


    }


    protected static void mobEffectPunishment(Player player, int level) {
        int weAmp = FHConfig.SERVER.weaknessEffectAmplifier.get();
        int slAmp = FHConfig.SERVER.weaknessEffectAmplifier.get();
        MobEffectInstance weaknessEffect = player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance movementSlowDownEffect = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (weAmp > -1 && (weaknessEffect == null || weaknessEffect.getDuration() <= 100))
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, weAmp + level, false, false));
        if (slAmp > -1 && movementSlowDownEffect == null || movementSlowDownEffect.getDuration() <= 100)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 400, slAmp, false, false));

    }

    public static LazyOptional<NutritionCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_NUTRITION.getCapability(player);
    }
}
