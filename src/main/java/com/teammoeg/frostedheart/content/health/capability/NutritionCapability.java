/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.health.capability;

import com.teammoeg.caupona.CPTags;
import com.teammoeg.chorda.io.NBTSerializable;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionResolver;
import com.teammoeg.frostedheart.content.health.nutrition.PlayerNutritionState;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Player capability that persists and applies the four-channel nutrition state.
 *
 * <p>The capability owns serialization and game integration only. Numeric transitions are
 * delegated to immutable {@link PlayerNutritionState} instances, while food facts always come
 * from {@link FoodNutritionResolver}. Stored version {@code 2} uses direct {@code 0..100}
 * percentages; older unversioned values are migrated once during loading.</p>
 */
public class NutritionCapability implements NBTSerializable {
    private static final int STORAGE_VERSION = 2;

    public static final PlayerNutritionState DEFAULT_VALUE = PlayerNutritionState.DEFAULT;

    private PlayerNutritionState nutrition = DEFAULT_VALUE;

    /** Food level captured before consumption, refreshed at the start of each player tick. */
    public transient int calculatedFoodLevel;

    @Override
    public void save(CompoundTag compound, boolean isPacket) {
        compound.putInt("version", STORAGE_VERSION);
        compound.putFloat("fat", nutrition.fat());
        compound.putFloat("carbohydrate", nutrition.carbohydrate());
        compound.putFloat("protein", nutrition.protein());
        compound.putFloat("vegetable", nutrition.vegetable());
    }

    /**
     * Loads the current percentage format or migrates legacy {@code 0..10000} values by
     * multiplying them by {@code 0.01}. A tag with no nutrition channels uses the default
     * {@code 70} state instead of treating missing values as zero.
     */
    @Override
    public void load(CompoundTag nbt, boolean isPacket) {
        if (!nbt.contains("fat") && !nbt.contains("carbohydrate")
                && !nbt.contains("protein") && !nbt.contains("vegetable")) {
            set(DEFAULT_VALUE);
            return;
        }
        float scale = nbt.getInt("version") >= STORAGE_VERSION ? 1.0f : 0.01f;
        set(new PlayerNutritionState(
                nbt.getFloat("fat") * scale,
                nbt.getFloat("carbohydrate") * scale,
                nbt.getFloat("protein") * scale,
                nbt.getFloat("vegetable") * scale));
    }

    /**
     * @return the current immutable player state snapshot
     */
    public PlayerNutritionState get() {
        return nutrition;
    }

    public void set(PlayerNutritionState state) {
        nutrition = state == null ? DEFAULT_VALUE : state;
    }

    public void addFat(float amount) {
        nutrition = nutrition.addFat(amount);
    }

    public void addCarbohydrate(float amount) {
        nutrition = nutrition.addCarbohydrate(amount);
    }

    public void addProtein(float amount) {
        nutrition = nutrition.addProtein(amount);
    }

    public void addVegetable(float amount) {
        nutrition = nutrition.addVegetable(amount);
    }

    public void setFat(float value) {
        nutrition = nutrition.withFat(value);
    }

    public void setCarbohydrate(float value) {
        nutrition = nutrition.withCarbohydrate(value);
    }

    public void setProtein(float value) {
        nutrition = nutrition.withProtein(value);
    }

    public void setVegetable(float value) {
        nutrition = nutrition.withVegetable(value);
    }

    /**
     * Resolves and applies nutrition from a normally consumed food stack.
     *
     * <p>Only vanilla hunger the item can actually restore produces nutrition; saturation and
     * hunger overflow are ignored. Non-food Caupona containers remain eligible for dynamic
     * resolution.</p>
     *
     * @param player consuming player and resolver context
     * @param food actual consumed stack
     */
    public void eat(Player player, ItemStack food) {
        if (!food.isEdible() && !food.is(CPTags.Items.CONTAINER)) return;
        FoodNutritionProfile profile = FoodNutritionResolver.resolve(player.level(), food);
        if (profile.isZero()) return;
        FoodProperties properties = food.getFoodProperties(player);
        if (properties == null || properties.getNutrition() <= 0) return;
        nutrition = nutrition.afterEating(
                profile, properties.getNutrition(), calculatedFoodLevel,
                FHConfig.SERVER.NUTRITION.nutritionGainRate.get());
        calculatedFoodLevel = player.getFoodData().getFoodLevel();
    }

    /**
     * Resolves and applies nutrition using a caller-supplied vanilla hunger value.
     *
     * <p>This overload is for consumption paths whose hunger is not exposed through the stack's
     * normal {@link FoodProperties}. The same missing-hunger cap and configured gain rate still
     * apply.</p>
     *
     * @param player consuming player and resolver context
     * @param food actual consumed stack
     * @param hungerOverride vanilla hunger supplied by the integration
     */
    public void eat(Player player, ItemStack food, int hungerOverride) {
        FoodNutritionProfile profile = FoodNutritionResolver.resolve(player.level(), food);
        if (profile.isZero() || hungerOverride <= 0) return;
        nutrition = nutrition.afterEating(
                profile, hungerOverride, calculatedFoodLevel,
                FHConfig.SERVER.NUTRITION.nutritionGainRate.get());
        calculatedFoodLevel = player.getFoodData().getFoodLevel();
    }

    /**
     * Detects vanilla hunger lost since the previous food-data update and consumes nutrition for
     * that loss.
     *
     * @param player player whose food data should be inspected
     */
    public void consume(Player player) {
        FoodData food = player.getFoodData();
        if (food.getLastFoodLevel() > food.getFoodLevel()) {
            consume(food.getLastFoodLevel() - food.getFoodLevel());
        }
    }

    /**
     * Removes the configured fixed amount from every nutrition channel.
     *
     * @param amount whole vanilla hunger points lost
     */
    public void consume(int amount) {
        if (amount <= 0) return;
        nutrition = nutrition.afterHungerLoss(
                amount, FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.get());
    }

    /**
     * Applies the existing anemia consequence from the current percentage state.
     *
     * @param player player to evaluate; peaceful difficulty suppresses the effect
     */
    public void punishment(Player player) {
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) return;

        int count = 0;
        if (nutrition.fat() > 100) count++;
        if (nutrition.protein() < 20) count += 2;
        if (nutrition.protein() > 100) count++;
        if (nutrition.vegetable() < 20) count += 2;

        count /= 2;
        if (count > 0) {
            player.addEffect(new MobEffectInstance(FHMobEffects.ANEMIA.get(), 200, count - 1));
        }
    }

    private float removeCenter(float percent) {
        if (percent < 0.3f) return percent / 0.3f * 0.5f;
        if (percent > 0.7f) return (percent - 0.7f) / 0.3f * 0.5f + 0.5f;
        return 0.5f;
    }

    /**
     * Replaces the player's persistent nutrition-based maximum-health modifier.
     *
     * @param player player whose max-health attribute should be recalculated
     */
    public void addAttributes(Player player) {
        float v1 = Mth.clampedLerp(-5, 5, removeCenter(nutrition.carbohydrate() / 100f));
        float v2 = Mth.clampedLerp(-5, 5, removeCenter(nutrition.fat() / 100f));
        float v3 = Mth.clampedLerp(-5, 5, removeCenter(nutrition.protein() / 100f));
        float v4 = Mth.clampedLerp(-5, 5, removeCenter(nutrition.vegetable() / 100f));
        AttributeInstance instance = player.getAttributes().getInstance(Attributes.MAX_HEALTH);
        AttributeModifier modifier = new AttributeModifier(
                NutritionUUID, "nutrition", Math.round(v1 + v2 + v3 + v4),
                AttributeModifier.Operation.ADDITION);
        if (instance.hasModifier(modifier)) instance.removeModifier(modifier);
        instance.addPermanentModifier(modifier);
    }

    public static final UUID NutritionUUID =
            UUID.fromString("f3f5f6f7-8f9f-afbf-cfcf-dfdfefeff0f1");

    public static LazyOptional<NutritionCapability> getCapability(@Nullable Player player) {
        return FHCapabilities.PLAYER_NUTRITION.getCapability(player);
    }
}
