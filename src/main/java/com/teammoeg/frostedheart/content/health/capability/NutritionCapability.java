/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.health.capability;

import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.health.event.GatherFoodNutritionEvent;
import com.teammoeg.frostedheart.content.health.network.PlayerNutritionSyncPacket;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.UUID;

public class NutritionCapability implements NBTSerializable {
	@Override
    public void save(CompoundTag compound, boolean isPacket) {
        compound.putFloat("fat", nutrition.fat);
        compound.putFloat("carbohydrate", nutrition.carbohydrate);
        compound.putFloat("protein", nutrition.protein);
        compound.putFloat("vegetable", nutrition.vegetable);
    }

    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        set(new ImmutableNutrition(nbt.getFloat("fat"),nbt.getFloat("carbohydrate"),nbt.getFloat("protein"),nbt.getFloat("vegetable")));
    }
    public static final ImmutableNutrition DEFAULT_VALUE=new ImmutableNutrition(5000);
    private MutableNutrition nutrition = DEFAULT_VALUE.mutableCopy();


    public void addFat(Player player, float add) {
        this.nutrition.fat+=add;
        syncToClientOnRestore(player);
    }

    public void addCarbohydrate(Player player, float add) {
        this.nutrition.carbohydrate+=add;
        syncToClientOnRestore(player);
    }

    public void addProtein(Player player, float add) {
        this.nutrition.protein+=add;
        syncToClientOnRestore(player);
    }

    public void addVegetable(Player player, float add) {
        this.nutrition.vegetable+=add;
        syncToClientOnRestore(player);
    }

    public void set(Nutrition temp) {
    	if(temp!=this.nutrition)
    		this.nutrition.set(temp);
    }
    public void setFat(float temp) {
        this.nutrition.fat=temp;
    }

    public void setCarbohydrate(float temp) {
        this.nutrition.carbohydrate=temp;
    }

    public void setProtein(float temp) {
        this.nutrition.protein=temp;
    }

    public void setVegetable(float temp) {
        this.nutrition.vegetable=temp;
    }

    public Nutrition get() {
        return nutrition;
    }

    public static void syncToClient(ServerPlayer player) {
        getCapability(player).ifPresent(t -> FHNetwork.sendPlayer(player, new PlayerNutritionSyncPacket(t.get())));
    }

    public static void syncToClientOnRestore(Player player) {
        if (!player.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            syncToClient(serverPlayer);
        }
    }

    /**
     * 如一个食物营养值为0.1，0，0，0.2，饱食度是4，那么这个食物给玩家增加的基础营养值就是0.1*4,0,0,0.2*4
     * 再乘以营养增加比例，默认是40
     * @param player 玩家
     * @param food 食物
     */
    public void eat(Player player, ItemStack food) {
        if(!food.isEdible()) return;
        Level level = player.level();
        Nutrition wRecipe = NutritionRecipe.getRecipeFromItem(player, food);
        if(wRecipe == null) return;
        int nutrition = food.getFoodProperties(player).getNutrition();
        Nutrition recipeNutrition = wRecipe;
        this.nutrition.addScaled(recipeNutrition, nutrition*FHConfig.SERVER.nutritionGainRate.get());
        syncToClientOnRestore(player);
    }

    public void consume(Player player) {
        float radio = - 0.1f * FHConfig.SERVER.nutritionConsumptionRate.get();

        this.nutrition.addScaled(this.nutrition, radio / nutrition.getNutritionValue());
        syncToClientOnRestore(player);
    }


//    public void award(Player player) {
//
//    }

    public void punishment(Player player) {
        //TODO 营养值过高或过低的惩罚
        int count = 0;

        if(nutrition.fat<3000){
            count++;

        }
        if(nutrition.fat>8000){
            count++;

        }
        if(nutrition.carbohydrate<3000){
            count++;

        }
        if(nutrition.carbohydrate>8000){
            count++;

        }
        if(nutrition.protein<3000){
            count++;

        }
        if(nutrition.protein>8000){
            count++;

        }
        if(nutrition.vegetable<3000){
            count++;

        }
        if(nutrition.vegetable>8000){
            count++;

        }
        int a = count/2;
        if(count>0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, count - 1));
        }
        if(a>0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, a));
        }

        // 对生命值上限的修改
        int v = (int) (20 - nutrition.getNutritionValue() / 1000);
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.MAX_HEALTH);
        AttributeModifier modifier = new AttributeModifier(NutritionUUID, "nutrition", -v, AttributeModifier.Operation.ADDITION);
        if(instance.hasModifier(modifier))
            instance.removeModifier(modifier);
        instance.addPermanentModifier(modifier);
    }

    public static final UUID NutritionUUID = UUID.fromString("f3f5f6f7-8f9f-afbf-cfcf-dfdfefeff0f1");

    public static LazyOptional<NutritionCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_NUTRITION.getCapability(player);
    }

    @Nullable
    public static Nutrition getFoodNutrition(Player player,ItemStack food) {
        return NutritionRecipe.getRecipeFromItem(player, food);
        
    }
}
