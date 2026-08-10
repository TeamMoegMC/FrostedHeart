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

package com.teammoeg.frostedheart.content.town.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidentFoodLevelTagTest {
    @Test
    void levelsAreDisjointAndCookedMeatOutranksRawMeat() throws IOException {
        Map<String, Integer> levels = loadLevels();

        assertHigher(levels, "minecraft:cooked_beef", "minecraft:beef");
        assertHigher(levels, "minecraft:cooked_chicken", "minecraft:chicken");
        assertHigher(levels, "minecraft:cooked_mutton", "minecraft:mutton");
        assertHigher(levels, "minecraft:cooked_porkchop", "minecraft:porkchop");
        assertHigher(levels, "minecraft:cooked_rabbit", "minecraft:rabbit");
        assertHigher(levels, "minecraft:cooked_salmon", "minecraft:salmon");
        assertHigher(levels, "frostedheart:cooked_fox_meat", "frostedheart:fox_meat");
        assertHigher(levels, "frostedheart:cooked_polar_bear_meat", "frostedheart:polar_bear_meat");
        assertHigher(levels, "frostedheart:cooked_squid_tentacles", "frostedheart:squid_tentacles");
        assertHigher(levels, "frostedheart:cooked_whale_meat", "frostedheart:raw_whale_meat");
        assertHigher(levels, "frostedheart:cooked_wolf_meat", "frostedheart:wolf_meat");
    }

    private static Map<String, Integer> loadLevels() throws IOException {
        Map<String, Integer> levels = new HashMap<>();
        ClassLoader loader = ResidentFoodLevelTagTest.class.getClassLoader();
        for (int level = 0; level <= 4; level++) {
            String path = "data/frostedheart/tags/items/town_resource_resident_food_level_"
                    + level + ".json";
            try (InputStream stream = loader.getResourceAsStream(path)) {
                assertNotNull(stream, "Missing resident food level tag: " + path);
                JsonObject root = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement value : root.getAsJsonArray("values")) {
                    String item = value.getAsString();
                    Integer previous = levels.put(item, level);
                    assertNull(previous, item + " appears in both level " + previous + " and " + level);
                }
            }
        }
        return levels;
    }

    private static void assertHigher(Map<String, Integer> levels, String preferred, String fallback) {
        assertNotNull(levels.get(preferred), "Missing preferred food: " + preferred);
        assertNotNull(levels.get(fallback), "Missing fallback food: " + fallback);
        assertTrue(levels.get(preferred) > levels.get(fallback),
                preferred + " must outrank " + fallback);
    }
}
