package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void phaseTwoWorkflowFieldsDecodeWithStableReferences() {
        JsonElement topicJson = JsonParser.parseString("""
                {"format":3,"legacy":{"mode":"coexist"},
                 "idea_sources":[{"provider":"frostedheart:field_evidence","idea":"test:idea","required_tags":["forge:stone"]}],
                 "inspiration":{"provider":"frostedheart:drawing_desk","idea":"test:idea","paper_level":0},
                 "protocols":[{"id":"test:compare","resolver":"frostedheart:manual_field_comparison","outcomes":["match","no_match","insufficient"]}],
                 "resolution":{"resolver":"frostedheart:field_comparison_resolution","idea":"test:idea","results":["test:finding"]},
                 "results":[{"type":"finding","id":"test:finding","views":["frostedheart:geology_archive"]}]}
                """);
        ResearchTopicDefinition topic = ResearchTopicDefinition.CODEC.parse(JsonOps.INSTANCE, topicJson)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertEquals(ResearchTopicDefinition.Legacy.Mode.COEXIST, topic.legacy().mode());
        assertEquals(new net.minecraft.resources.ResourceLocation("test", "idea"), topic.ideaSources().get(0).idea());
        assertEquals(List.of("match", "no_match", "insufficient"), topic.protocols().get(0).outcomes());
        assertEquals(List.of(new net.minecraft.resources.ResourceLocation("test", "finding")),
                topic.resolution().orElseThrow().results());
    }

    @Test
    void bundledGeologyTopicReferencesBundledStableRecipes() {
        ResearchTopicDefinition topic = ResearchTopicDefinition.CODEC.parse(JsonOps.INSTANCE, readResource(
                        "/data/the_winter_rescue/frostedresearch/topics/geology_understanding.json"))
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        ResearchResult.Design design = topic.results().stream()
                .filter(ResearchResult.Design.class::isInstance)
                .map(ResearchResult.Design.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of(new net.minecraft.resources.ResourceLocation(
                "the_winter_rescue", "research/copper_pro_pick")), design.recipes());
        assertRecipeResult(
                "/data/the_winter_rescue/recipes/research/copper_pro_pick.json",
                "minecraft:crafting_shaped",
                "frostedheart:copper_pro_pick");
        assertRecipeResult(
                "/data/the_winter_rescue/recipes/research/research_notebook.json",
                "minecraft:crafting_shapeless",
                "frostedresearch:research_notebook");
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

    private static void assertRecipeResult(String path, String type, String item) {
        var recipe = readResource(path).getAsJsonObject();
        assertEquals(type, recipe.get("type").getAsString());
        assertEquals(item, recipe.getAsJsonObject("result").get("item").getAsString());
    }

    private static JsonElement readResource(String path) {
        InputStream stream = ResearchResultCodecTest.class.getResourceAsStream(path);
        assertNotNull(stream, () -> "missing test resource " + path);
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException exception) {
            throw new AssertionError("failed to read " + path, exception);
        }
    }
}
