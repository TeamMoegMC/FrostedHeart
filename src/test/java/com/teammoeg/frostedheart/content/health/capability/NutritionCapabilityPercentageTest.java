/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.health.capability;

import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import com.teammoeg.frostedheart.content.health.nutrition.PlayerNutritionState;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NutritionCapabilityPercentageTest {
    @Test
    void missingStateUsesSeventyPercentDefaults() {
        NutritionCapability capability = new NutritionCapability();
        capability.load(new CompoundTag(), false);

        assertEquals(PlayerNutritionState.DEFAULT, capability.get());
    }

    @Test
    void oldNbtScaleMigratesOnceAndNewNbtRemainsDirectPercent() {
        CompoundTag legacy = new CompoundTag();
        legacy.putFloat("fat", 1_000);
        legacy.putFloat("carbohydrate", 2_000);
        legacy.putFloat("protein", 6_000);
        legacy.putFloat("vegetable", 12_000);
        NutritionCapability capability = new NutritionCapability();
        capability.load(legacy, false);

        assertEquals(10.0f, capability.get().fat(), 1.0e-6f);
        assertEquals(20.0f, capability.get().carbohydrate(), 1.0e-6f);
        assertEquals(60.0f, capability.get().protein(), 1.0e-6f);
        assertEquals(100.0f, capability.get().vegetable(), 1.0e-6f);

        CompoundTag saved = new CompoundTag();
        capability.save(saved, false);
        NutritionCapability reloaded = new NutritionCapability();
        reloaded.load(saved, false);
        assertEquals(60.0f, reloaded.get().protein(), 1.0e-6f);
        assertEquals(2, saved.getInt("version"));
    }

    @Test
    void beefGainUsesOnlyEffectiveHunger() {
        FoodNutritionProfile beef = new FoodNutritionProfile(0, 0, 60, 0);
        PlayerNutritionState empty = PlayerNutritionState.uniform(0);

        assertEquals(4.8f, empty.afterEating(beef, 8, 12, 1).protein(), 1.0e-6f);
        assertEquals(1.2f, empty.afterEating(beef, 8, 18, 1).protein(), 1.0e-6f);
        assertEquals(0.0f, empty.afterEating(beef, 8, 20, 1).protein(), 1.0e-6f);
    }

    @Test
    void stateTransitionsClampWritesAndApplyUniformHungerLoss() {
        PlayerNutritionState state = new PlayerNutritionState(-1, 101, Float.NaN, 50)
                .afterHungerLoss(4, 0.25)
                .addProtein(200);

        assertEquals(0.0f, state.fat(), 1.0e-6f);
        assertEquals(99.0f, state.carbohydrate(), 1.0e-6f);
        assertEquals(100.0f, state.protein(), 1.0e-6f);
        assertEquals(49.0f, state.vegetable(), 1.0e-6f);
    }
}
