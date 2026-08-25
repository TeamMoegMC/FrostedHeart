package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchResultCodecTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void decodesAndRoundTripsAllFiveNamedBranches() {
        assertRoundTrip("""
                {"type":"finding","id":"test:rocks","views":["test:rock_map"]}
                """, ResearchResult.Finding.class);
        assertRoundTrip("""
                {"type":"design","id":"test:copper_pick","recipes":["test:copper_pick"]}
                """, ResearchResult.Design.class);
        assertRoundTrip("""
                {"type":"construction","id":"test:blast_furnace","multiblocks":["immersiveengineering:blast_furnace"]}
                """, ResearchResult.Construction.class);
        assertRoundTrip("""
                {"type":"procedure","id":"test:calculator","usable_blocks":["test:calculator"]}
                """, ResearchResult.Procedure.class);
        assertRoundTrip("""
                {"type":"prototype","id":"test:efficiency","profile":"test:efficiency"}
                """, ResearchResult.Prototype.class);
    }

    @Test
    void constructionAndProcedureRejectEachOthersFields() {
        assertTrue(parse("""
                {"type":"construction","id":"test:bad","multiblocks":["test:machine"],"usable_blocks":["test:machine"]}
                """).error().isPresent());
        assertTrue(parse("""
                {"type":"procedure","id":"test:bad","usable_blocks":["test:machine"],"multiblocks":["test:machine"]}
                """).error().isPresent());
    }

    @Test
    void topicAndProfileShellRoundTripWithoutInterpretingFutureFields() {
        JsonElement topicJson = JsonParser.parseString("""
                {"format":3,"presentation":{},"results":[
                  {"type":"finding","id":"test:finding","views":[]}
                ],"rewards":[{"item":"minecraft:paper","count":2}],"future_field":{"kept_by_full_loader":true}}
                """);
        ResearchTopicDefinition topic = ResearchTopicDefinition.CODEC.parse(JsonOps.INSTANCE, topicJson)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertEquals(3, topic.format());
        assertEquals(1, topic.results().size());
        assertEquals(2, topic.rewards().get(0).count());

        PrototypeProfileDefinition profile = new PrototypeProfileDefinition(1, 7);
        JsonElement encoded = PrototypeProfileDefinition.CODEC.encodeStart(JsonOps.INSTANCE, profile)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertEquals(profile, PrototypeProfileDefinition.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); }));
    }

    private static void assertRoundTrip(String json, Class<? extends ResearchResult> expectedType) {
        ResearchResult decoded = parse(json).getOrThrow(false, message -> { throw new AssertionError(message); });
        assertInstanceOf(expectedType, decoded);
        JsonElement encoded = ResearchResult.CODEC.encodeStart(JsonOps.INSTANCE, decoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        ResearchResult roundTrip = ResearchResult.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertEquals(decoded, roundTrip);
    }

    private static com.mojang.serialization.DataResult<ResearchResult> parse(String json) {
        return ResearchResult.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
}
