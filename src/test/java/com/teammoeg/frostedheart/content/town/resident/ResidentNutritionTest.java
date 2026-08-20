/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.resident;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void legacyResidentIgnoresRemovedNutritionDevelopmentState() {
        String json = """
                {"firstName":"Legacy","lastName":"Resident",
                 "uuid":[0,0,0,7],
                 "strength":92,"intelligence":44,
                 "nutrition":{"fat":35,"carbohydrate":70,"protein":0,"vegetable":100},
                 "nutritionDevelopment":{"obsolete":true}}
                """;
        Resident resident = Resident.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .result().orElseThrow();

        assertEquals(92.0, resident.getStrength(), 1.0e-12);
        assertEquals(44.0, resident.getIntelligence(), 1.0e-12);
        assertEquals(new ResidentNutrition(35, 70, 0, 100), resident.getNutrition());
        assertFalse(resident.getNutritionSnapshot().hasData());
    }

    @Test
    void flatNutritionSnapshotRoundTripsThroughResidentCodec() {
        Resident resident = new Resident("Round", "Trip",
                java.util.UUID.fromString("cb7a2645-d75e-4df7-823b-c9bc98f45231"));
        resident.completeNutritionMeal(new ResidentNutrition(20, 40, 60, 80));
        resident.recordNutritionRecovery(0.5, 0.25, 0.75, 0.5);
        resident.recordNutritionAttributes(
                new ResidentActivity(1.0, 0.25),
                new ResidentAttributeChange(51, 1, 1.2, 0.1, 0.1, 1),
                new ResidentAttributeChange(50.2, 0.475, 0.3, 0.1, 0, 0.2));
        var encoded = Resident.CODEC.encodeStart(JsonOps.INSTANCE, resident)
                .result().orElseThrow();
        Resident decoded = Resident.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result().orElseThrow();

        assertEquals(resident.getNutritionSnapshot(), decoded.getNutritionSnapshot());
        assertTrue(decoded.getNutritionSnapshot().hasData());
    }

    @Test
    void rawNbtIgnoresRemovedNutritionDevelopmentField() {
        Resident resident = new Resident("Legacy", "Nbt",
                java.util.UUID.fromString("d768481b-55c8-49c8-804d-ec6f02ed6a49"));
        resident.setStrength(83.0);
        resident.setIntelligence(62.0);
        resident.setNutrition(new ResidentNutrition(10, 20, 30, 40));
        CompoundTag saved = resident.serialize();
        saved.remove("nutritionSnapshot");
        saved.put("nutritionDevelopment", new CompoundTag());

        Resident decoded = new Resident(saved);

        assertEquals(83.0, decoded.getStrength(), 1.0e-12);
        assertEquals(62.0, decoded.getIntelligence(), 1.0e-12);
        assertEquals(new ResidentNutrition(10, 20, 30, 40), decoded.getNutrition());
        assertFalse(decoded.getNutritionSnapshot().hasData());
    }

    @Test
    void decayAndMealIntakeAreClampedPerChannel() {
        ResidentNutrition depleted = new ResidentNutrition(5, 10, 15, 20).decay(10);
        assertEquals(new ResidentNutrition(0, 0, 5, 10), depleted);

        ResidentNutrition restored = depleted.withMeal(
                new ResidentNutrition.NutritionIntake(140, 70, 35, -1),
                70, 10, 2);
        assertEquals(new ResidentNutrition(20, 10, 10, 10), restored);
        assertEquals(new ResidentNutrition(80, 80, 80, 80),
                new ResidentNutrition(79, 79, 79, 79).withMeal(
                        new ResidentNutrition.NutritionIntake(100, 100, 100, 100),
                        1, 10, 10, 80));
    }

    @Test
    void validationFoodExamplesUseHungerWeightedPercentagePoints() {
        ResidentNutrition afterBeef = new ResidentNutrition(50, 50, 50, 50).decay(1)
                .withMeal(new ResidentNutrition.NutritionIntake(0, 0, 8 * 60, 0),
                        200, 2, 2);
        assertEquals(53.0, afterBeef.protein(), 1.0e-12);

        ResidentNutrition afterPotato = new ResidentNutrition(50, 50, 50, 50).decay(1)
                .withMeal(new ResidentNutrition.NutritionIntake(0, 5 * 40, 0, 5 * 20),
                        200, 2, 2);
        assertEquals(51.0, afterPotato.carbohydrate(), 1.0e-12);
        assertEquals(50.0, afterPotato.vegetable(), 1.0e-12);
    }
}
