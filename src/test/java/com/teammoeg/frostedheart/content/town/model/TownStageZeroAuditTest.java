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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.content.town.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownStageZeroAuditTest {
    @Test
    void readsCurrentFhGeneratorRecipesAndHuntingLoot() throws IOException {
        Path resources = Path.of("src/main/resources/data/frostedheart");
        assertEquals(1_600, TownStageZeroAudit.parseGeneratorRecipeProcessTicks(
                resources.resolve("recipes/generator/coal.json")));
        assertEquals(3_200, TownStageZeroAudit.parseGeneratorRecipeProcessTicks(
                resources.resolve("recipes/generator/coal_coke.json")));
        assertEquals(8, TownStageZeroAudit.parseHuntingLoot(
                resources.resolve("loot_tables/town/hunting.json")).size());
    }

    @Test
    void extractsOnlyRequestedBiomeMineObject() {
        String script = """
                event.custom(biomeMineResourceRecipe('test:other', {'minecraft:dirt': 99}))
                event.custom(biomeMineResourceRecipe('the_winter_rescue:fossil_deposits', {
                    'minecraft:coal': 8,
                    'minecraft:bone_block': 4,
                    'frostedheart:biomass': 2,
                    'minecraft:stone': 10
                }))
                """;
        Map<String, Double> weights = TownStageZeroAudit.parseBiomeMineWeights(
                        script, "the_winter_rescue:fossil_deposits").stream()
                .collect(Collectors.toMap(
                        TownStageZeroModel.WeightedResource::item,
                        TownStageZeroModel.WeightedResource::weight));

        assertEquals(Map.of(
                "minecraft:coal", 8.0,
                "minecraft:bone_block", 4.0,
                "frostedheart:biomass", 2.0,
                "minecraft:stone", 10.0), weights);
    }

    @Test
    void readsPercentResearchEffect(@TempDir Path temporaryDirectory) throws IOException {
        Path research = temporaryDirectory.resolve("research.json");
        Files.writeString(research, """
                {"effects":[
                  {"type":"stats","vars":"generator_effi","val":10.0,"percent":true},
                  {"type":"stats","vars":"unrelated","val":50.0,"percent":true}
                ]}
                """);

        assertEquals(0.1,
                TownStageZeroAudit.parseResearchStatBonus(research, "generator_effi"),
                1.0e-12);
    }
}
