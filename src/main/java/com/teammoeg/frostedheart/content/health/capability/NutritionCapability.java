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

import com.teammoeg.caupona.CPTags;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.chorda.math.CMath;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
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
        set(new ImmutableNutrition(CMath.toValidClampedValue(nbt.getFloat("fat"), 0, 100000) , CMath.toValidClampedValue(nbt.getFloat("carbohydrate"), 0, 100000), CMath.toValidClampedValue(nbt.getFloat("protein"), 0, 100000), CMath.toValidClampedValue(nbt.getFloat("vegetable"), 0, 100000)));
        	
    }

    public static final ImmutableNutrition DEFAULT_VALUE = new ImmutableNutrition(7000);
    private MutableNutrition nutrition = DEFAULT_VALUE.mutableCopy();
    //food level calculated within nutrition mechanic, resets every tick
    public transient int calculatedFoodLevel;
    public void addFat(Player player, float add) {
        this.nutrition.fat += add;
        callOnChange(player);
    }

    public void addCarbohydrate(Player player, float add) {
        this.nutrition.carbohydrate += add;
        callOnChange(player);
    }

    public void addProtein(Player player, float add) {
        this.nutrition.protein += add;
        callOnChange(player);
    }

    public void addVegetable(Player player, float add) {
        this.nutrition.vegetable += add;
        callOnChange(player);
    }

    public void set(Nutrition temp) {
        if (temp != this.nutrition)
            this.nutrition.set(temp);
    }

    public void setFat(float temp) {
        this.nutrition.fat = temp;
    }

    public void setCarbohydrate(float temp) {
        this.nutrition.carbohydrate = temp;
    }

    public void setProtein(float temp) {
        this.nutrition.protein = temp;
    }

    public void setVegetable(float temp) {
        this.nutrition.vegetable = temp;
    }

    public Nutrition get() {
        return nutrition;
    }
/*
    public static void syncToClient(ServerPlayer player) {
        getCapability(player).ifPresent(t -> FHNetwork.sendPlayer(player, new PlayerNutritionSyncPacket(t.get())));
    }*/

    public static void callOnChange(Player player) {
        /*if (!player.level().isClientSide()) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            syncToClient(serverPlayer);
        }*/
    }

    /**
     * 如一个食物营养值为0.1，0，0，0.2，饱食度是4，那么这个食物给玩家增加的基础营养值就是0.1*4,0,0,0.2*4
     * 再乘以营养增加比例，默认是40
     *
     * @param player 玩家
     * @param food   食物
     */
    public void eat(Player player, ItemStack food) {
        if (food.isEdible() || food.is(CPTags.Items.CONTAINER)) {
            Nutrition wRecipe = NutritionRecipe.getRecipeFromItem(player, food);
            if (wRecipe == null) return;
            FoodProperties fp = food.getFoodProperties(player);
            if (fp != null) {
                int nutrition = fp.getNutrition();
                if(nutrition>0) {
	                int filling=20-calculatedFoodLevel;
	                if(filling<nutrition) {//replace overfilled hunger to new food hunger
	                	consume(nutrition-filling);
	                }
	                calculatedFoodLevel=player.getFoodData().getFoodLevel();
	                this.nutrition.addScaled(wRecipe, (float) (nutrition * FHConfig.SERVER.nutritionGainRate.get()));
	                this.nutrition.ensureValid();
	                callOnChange(player);
                }
            }
        }
    }
    public void eat(Player player, ItemStack food,int hungerOverride) {
        Nutrition wRecipe = NutritionRecipe.getRecipeFromItem(player, food);
        if (wRecipe == null) return;
        if(hungerOverride>0) {
            FoodData fd=player.getFoodData();
            int filling=20-calculatedFoodLevel;
            if(filling<hungerOverride) {//replace overfilled hunger to new food hunger
            	consume(hungerOverride-filling);
            }
            calculatedFoodLevel=player.getFoodData().getFoodLevel();
            this.nutrition.addScaled(wRecipe, (float) (hungerOverride * FHConfig.SERVER.nutritionGainRate.get()));
            this.nutrition.ensureValid();
            callOnChange(player);
        }
    }
    public void consume(Player player) {
    	FoodData fd=player.getFoodData();
    	if(fd.getLastFoodLevel()>fd.getFoodLevel()) {
    		consume(fd.getLastFoodLevel()-fd.getFoodLevel());
	        callOnChange(player);
    	}
    }
    public void consume(int amount) {
    	if(amount<=0)return;
    	this.nutrition.addScaled(this.nutrition, (float) (FHConfig.SERVER.nutritionConsumptionRate.get()*(-amount)));
    	this.nutrition.ensureValid();
    }


    public void punishment(Player player) {
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }

        //TODO 营养值过高或过低的惩罚
        int count = 0;

        /*if (nutrition.fat < 2000) {
            count += 2;

        }*/
        if (nutrition.fat > 10000) {
            count++;
        }
        /*if (nutrition.carbohydrate < 2000) {
            count += 2;

        }
        if (nutrition.carbohydrate > 9000) {
            count++;
        }*/
        if (nutrition.protein < 2000) {
            count += 2;

        }
        if (nutrition.protein > 10000) {
            count++;
        }
        if (nutrition.vegetable < 2000) {
            count += 2;

            //player.addEffect(new MobEffectInstance(FHMobEffects.SCURVY.get(), 300, count - 1));
        }
        
        
        count /= 2;
        if (count > 0) {
            player.addEffect(new MobEffectInstance(FHMobEffects.ANEMIA.get(), 200, count-1));
        }


        
    }
    private float removeCenter(float percent) {
    	if(percent<0.3f)
    		return percent/0.3f*0.5f;
    	if(percent>0.7f)
    		return (percent-0.7f)/0.3f*0.5f+0.5f;
    	return 0.5f;
    }
    public void addAttributes(Player player) {
    	// 对生命值上限的修改
    	float v1=Mth.clampedLerp(-5, 5, removeCenter(nutrition.getCarbohydrate()/10000f));
    	float v2=Mth.clampedLerp(-5, 5, removeCenter(nutrition.getFat()/10000f));
    	float v3=Mth.clampedLerp(-5, 5, removeCenter(nutrition.getProtein()/10000f));
    	float v4=Mth.clampedLerp(-5, 5, removeCenter(nutrition.getVegetable()/10000f));
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.MAX_HEALTH);
        AttributeModifier modifier = new AttributeModifier(NutritionUUID, "nutrition", Math.round(v1+v2+v3+v4), AttributeModifier.Operation.ADDITION);
        if (instance.hasModifier(modifier))
            instance.removeModifier(modifier);
        instance.addPermanentModifier(modifier);
    }
    public static final UUID NutritionUUID = UUID.fromString("f3f5f6f7-8f9f-afbf-cfcf-dfdfefeff0f1");

    public static LazyOptional<NutritionCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_NUTRITION.getCapability(player);
    }

    @Nullable
    public static Nutrition getFoodNutrition(Player player, ItemStack food) {
        return NutritionRecipe.getRecipeFromItem(player, food);

    }
}
