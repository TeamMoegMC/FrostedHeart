package com.teammoeg.frostedheart.content.water.capability;

import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.water.network.PlayerWaterLevelSyncPacket;
import com.teammoeg.chorda.util.io.NBTSerializable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class WaterLevelCapability implements NBTSerializable {

    @Override
    public void save(CompoundTag nbt, boolean isPacket) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("PlayerWaterLevel", this.getWaterLevel());
        compound.putInt("PlayerWaterSaturationLevel", this.getWaterSaturationLevel());
        compound.putFloat("PlayerWaterExhaustionLevel", this.getWaterExhaustionLevel());
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        this.setWaterLevel(nbt.getInt("PlayerWaterLevel"));
        this.setWaterSaturationLevel(nbt.getInt("PlayerWaterSaturationLevel"));
        this.setWaterExhaustionLevel(nbt.getFloat("PlayerWaterExhaustionLevel"));
    }

    private int waterLevel = 20;
    private int waterSaturationLevel = 6;
    private float waterExhaustionLevel = 0;

    public void addWaterLevel(Player player, int add) {
        this.waterLevel = Math.min(this.waterLevel + add, 20);
        syncToClientOnRestore(player);
    }

    public void addWaterSaturationLevel(Player player, int add) {
        this.waterSaturationLevel = Math.min(this.waterSaturationLevel + add, 20);
        syncToClientOnRestore(player);
    }

    protected void addExhaustion(float add) {
        reduceLevel((int) ((this.waterExhaustionLevel + add) / 4.0f));
        this.waterExhaustionLevel = (this.waterExhaustionLevel + add) % 4.0f;
    }

    public void addExhaustion(Player player, float add) {
        //float moisturizingRate = WaterLevelUtil.getMoisturizingRate(player);
        float moisturizingRate =1;
        float finalValue = (float) ((double) add * FHConfig.SERVER.waterReducingRate.get()) * moisturizingRate;
        MobEffectInstance effect = player.getEffect(FHMobEffects.THIRST.get());
        if (effect != null) {
            addExhaustion(finalValue * (4 + effect.getAmplifier()) / 2);
        } else addExhaustion(finalValue);

        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    public void setWaterLevel(int temp) {
        this.waterLevel = temp;
    }

    public void setWaterExhaustionLevel(float waterExhaustionLevel) {
        this.waterExhaustionLevel = waterExhaustionLevel;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public void setWaterSaturationLevel(int waterSaturationLevel) {
        this.waterSaturationLevel = waterSaturationLevel;
    }

    public int getWaterSaturationLevel() {
        return waterSaturationLevel;
    }

    public float getWaterExhaustionLevel() {
        return waterExhaustionLevel;
    }

    public void reduceLevel(int reduce) {
        if (this.waterSaturationLevel - reduce >= 0) {
            waterSaturationLevel -= reduce;
        } else {
            if (waterLevel - (reduce - waterSaturationLevel) >= 0) {
                waterLevel -= reduce;
                waterSaturationLevel = 0;
            } else {
                waterLevel = 0;
                waterSaturationLevel = 0;
            }
        }
    }

    public void restoreWater(Player player, int restore) {
        this.waterLevel = Math.min(waterLevel + restore, 20);
        if (this.waterLevel == 20) this.waterSaturationLevel = Math.min(waterLevel + restore, 20);
        syncToClientOnRestore(player);
    }

    public static void syncToClient(ServerPlayer player) {
        WaterLevelCapability.getCapability(player).ifPresent(t -> FHNetwork.send(PacketDistributor.PLAYER.with(() -> player), new PlayerWaterLevelSyncPacket(t.getWaterLevel(), t.getWaterSaturationLevel(), t.getWaterExhaustionLevel())));
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
        if (player.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION) && getWaterLevel() > 17 && player.getFoodData().getFoodLevel() > 10 && player.getHealth() < player.getMaxHealth()) {
            player.heal(1);
            switch (player.level().getDifficulty()) {
                case PEACEFUL:
                    break;
                case EASY:
                    addExhaustion(5.0f);
                    foodData.addExhaustion(0.6F);
                    break;
                case NORMAL:
                    addExhaustion(6.0f);
                    foodData.addExhaustion(0.8F);
                    break;
                case HARD:
                    addExhaustion(7.0f);
                    foodData.addExhaustion(1.0F);
                    break;
            }
        }
    }

    public void punishment(Player player) {

        if (getWaterLevel() <= 6) {
            switch (player.level().getDifficulty()) {
                case PEACEFUL:
                    break;
                case EASY:
                    mobEffectPunishment(player, 0);
                    break;
                default:
                    mobEffectPunishment(player, 1);
                    break;
            }
        }
        int i = 0;
        if (player.level().getDifficulty() != Difficulty.HARD) i = 1;
        if (getWaterLevel() == 0 && player.getHealth() > i) {
            if (!player.level().isClientSide()) {
                player.hurt(new DamageSource(Holder.direct(new DamageType("byThirst", 1.0F))), 1.0f);
            } else {
                //player.level().playSound(player, player, SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        //TODO:当水分为0时死亡
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

    public static LazyOptional<WaterLevelCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_WATER_LEVEL.getCapability(player);
    }
}
