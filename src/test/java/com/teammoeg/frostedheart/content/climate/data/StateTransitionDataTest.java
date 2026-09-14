/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StateTransitionDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void independentDirectionsAndConditionsRoundTrip() {
        var data = decode("""
                {"block":{"Name":"minecraft:stone"}, "capacity_j_per_k":100,
                 "enthalpy_offset_j":1200,
                 "heating":{"target":{"Name":"minecraft:glass"}, "temperature_c":40,
                            "world_conditions":{"min_y_exclusive":-55,"fluid_boundary_tag":"minecraft:water"}},
                 "cooling":{"target":{"Name":"minecraft:dirt"}, "temperature_c":-10}}
                """);
        assertEquals(Blocks.GLASS.defaultBlockState(), data.heating().target());
        assertEquals(Blocks.DIRT.defaultBlockState(), data.cooling().target());
        assertEquals(-55, data.heating().worldConditions().minYExclusive());
        assertEquals(StateTransitionData.WorldConditions.NONE, data.cooling().worldConditions());
        assertEquals(data, StateTransitionData.CODEC.parse(JsonOps.INSTANCE,
                StateTransitionData.CODEC.encodeStart(JsonOps.INSTANCE, data).result().orElseThrow()).result().orElseThrow());
    }

    @Test
    void omittedDirectionsHaveNoInferredInverse() {
        var data = decode("""
                {"block":{"Name":"minecraft:stone"},
                 "cooling":{"target":{"Name":"minecraft:dirt"}, "temperature_c":-10}}
                """);
        assertNull(data.heating());
        assertTrue(data.hasTransitions());
        assertEquals(38000, data.cooling().latentHeatJ());
        var ordinary = decode("""
                {"block":{"Name":"minecraft:stone"}}
                """);
        assertFalse(ordinary.hasTransitions());
        assertTrue(Double.isNaN(ordinary.capacityJPerK()));
    }

    private static StateTransitionData decode(String json) {
        return StateTransitionData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElseThrow();
    }
}
