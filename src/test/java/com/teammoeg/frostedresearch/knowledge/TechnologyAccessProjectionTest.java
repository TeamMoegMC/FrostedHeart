package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnologyAccessProjectionTest {
    @Test
    void unmanagedTargetsStayOpenAndManagedTargetsNeedASource() {
        ResourceLocation managed = id("managed");
        TechnologyAccessProjection projection = TechnologyAccessProjection.create(
                Set.of(managed), Set.of(), Set.of(), Map.of(), Map.of(), Map.of());
        assertFalse(projection.recipe(managed).allowed());
        assertTrue(projection.recipe(managed).managed());
        assertTrue(projection.recipe(id("ordinary")).allowed());
        assertFalse(projection.recipe(id("ordinary")).managed());
    }

    @Test
    void resultKindsCannotLeakAcrossTechnologyChannels() {
        ResourceLocation recipe = id("recipe");
        ResourceLocation multiblock = id("multiblock");
        ResourceLocation block = id("block");
        AccessSource.ResultSource design = source(ResearchResult.ResultType.DESIGN, "design");
        AccessSource.ResultSource construction = source(ResearchResult.ResultType.CONSTRUCTION, "construction");
        AccessSource.ResultSource procedure = source(ResearchResult.ResultType.PROCEDURE, "procedure");
        TechnologyAccessProjection projection = TechnologyAccessProjection.create(
                Set.of(recipe), Set.of(multiblock), Set.of(block),
                Map.of(recipe, List.of(design)), Map.of(multiblock, List.of(construction)),
                Map.of(block, List.of(procedure)));
        assertEquals(List.of(design), projection.recipe(recipe).sources());
        assertEquals(List.of(construction), projection.multiblock(multiblock).sources());
        assertEquals(List.of(procedure), projection.block(block).sources());
        assertTrue(projection.recipe(recipe).allowed());
        assertTrue(projection.multiblock(multiblock).allowed());
        assertTrue(projection.block(block).allowed());
    }

    @Test
    void overlappingNewAndLegacySourcesRoundTripTogether() {
        ResourceLocation target = id("shared");
        List<AccessSource> sources = List.of(source(ResearchResult.ResultType.DESIGN, "design"),
                new AccessSource.LegacySource("legacy_research", "effect_nonce"));
        TechnologyAccessProjection projection = TechnologyAccessProjection.create(
                Set.of(target), Set.of(), Set.of(), Map.of(target, sources), Map.of(), Map.of());
        var encoded = TechnologyAccessProjection.CODEC.encodeStart(NbtOps.INSTANCE, projection)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        TechnologyAccessProjection decoded = TechnologyAccessProjection.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertEquals(sources, decoded.recipe(target).sources());
    }

    private static AccessSource.ResultSource source(ResearchResult.ResultType type, String id) {
        return new AccessSource.ResultSource(new ResourceLocation("test", "topic"), type,
                new ResourceLocation("test", id));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
