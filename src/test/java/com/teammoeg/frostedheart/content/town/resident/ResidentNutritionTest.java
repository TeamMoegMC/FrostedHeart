/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.resident;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResidentNutritionTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommentedConfig serverConfig = CommentedConfig.inMemory();
        FHConfig.SERVER_CONFIG.correct(serverConfig);
        FHConfig.SERVER_CONFIG.setConfig(serverConfig);
    }

    @Test
    void legacyResidentCodecStartsAtHealthyReserves() {
        Resident resident = Resident.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {"firstName":"Legacy","lastName":"Resident",
                 "uuid":[0,0,0,1]}
                """)).result().orElseThrow();

        assertEquals(ResidentNutrition.DEFAULT_VALUE, resident.getNutrition());
    }

    @Test
    void directNutrientAndFatProteinSupportComposeRecoveryMultiplier() {
        ResidentNutrition empty = new ResidentNutrition(0, 0, 0, 0);
        ResidentNutrition directOnly = new ResidentNutrition(0, 70, 0, 0);
        ResidentNutrition oneSupport = new ResidentNutrition(70, 70, 0, 0);
        ResidentNutrition complete = ResidentNutrition.DEFAULT_VALUE;

        assertEquals(0.5, empty.mentalRecoveryMultiplier(0.5), 1.0e-12);
        assertEquals(0.8, directOnly.mentalRecoveryMultiplier(0.5), 1.0e-12);
        assertEquals(0.9, oneSupport.mentalRecoveryMultiplier(0.5), 1.0e-12);
        assertEquals(1.0, complete.mentalRecoveryMultiplier(0.5), 1.0e-12);

        ResidentNutrition vegetableComplete = new ResidentNutrition(70, 0, 70, 70);
        assertEquals(1.0, vegetableComplete.healthRecoveryMultiplier(0.5), 1.0e-12);
    }

    @Test
    void growthHasDeficiencyFloorHealthyBaselineAndSurplusCap() {
        assertEquals(0.5, ResidentNutrition.growthMultiplier(0), 1.0e-12);
        assertEquals(1.0, ResidentNutrition.growthMultiplier(70), 1.0e-12);
        assertEquals(1.25, ResidentNutrition.growthMultiplier(100), 1.0e-12);
    }

    @Test
    void decayAndMealIntakeAreClampedPerChannel() {
        ResidentNutrition depleted = new ResidentNutrition(5, 10, 15, 20).decay(10);
        assertEquals(new ResidentNutrition(0, 0, 5, 10), depleted);

        ResidentNutrition restored = depleted.withMeal(
                new ResidentNutrition.NutritionIntake(140, 70, 35, -1),
                70, 10, 2);
        assertEquals(new ResidentNutrition(20, 10, 10, 10), restored);
    }

    @Test
    void configurableReserveRecoveryAndGrowthParametersDriveTheFormula() {
        ResidentNutrition.Parameters parameters = new ResidentNutrition.Parameters(
                80, 40, 1, 0, 0.25, 0.5);
        ResidentNutrition nutrition = new ResidentNutrition(80, 40, 0, 40);

        assertEquals(1.0, nutrition.mentalRecoveryMultiplier(0.2, parameters), 1.0e-12);
        assertEquals(0.25, ResidentNutrition.growthMultiplier(0, parameters), 1.0e-12);
        assertEquals(1.0, ResidentNutrition.growthMultiplier(40, parameters), 1.0e-12);
        assertEquals(1.5, ResidentNutrition.growthMultiplier(80, parameters), 1.0e-12);
        assertEquals(new ResidentNutrition(80, 80, 80, 80),
                new ResidentNutrition(79, 79, 79, 79).withMeal(
                        new ResidentNutrition.NutritionIntake(100, 100, 100, 100),
                        1, 10, 10, 80));
    }
}
