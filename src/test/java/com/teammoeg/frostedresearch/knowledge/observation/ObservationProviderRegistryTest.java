package com.teammoeg.frostedresearch.knowledge.observation;

import com.teammoeg.frostedheart.content.utility.oredetect.OreProspectingModel;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationProviderRegistryTest {
    private static final ResourceLocation DIMENSION = id("dimension");
    private static final ResourceLocation OAK_DOOR = new ResourceLocation("minecraft", "oak_door");

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void genericProviderRecordsAnyBlockIdentityStateContextAndSource() {
        UUID observer = UUID.randomUUID();
        ObservationContext context = context(new BlockPos(7, 64, -3), observer, false);

        KnowledgeRecord record = ObservationProviderRegistry.observeBlock(null, context);

        assertEquals(KnowledgeRecord.Type.BLOCK, record.type());
        assertEquals(KnowledgeRecord.BLOCK_KIND, record.kindId());
        assertEquals(DIMENSION, record.dimension());
        assertEquals(context.position(), record.position());
        assertEquals(OAK_DOOR, record.subject());
        assertEquals("false", record.stateProperties().get("open"));
        assertEquals(42, record.firstObserved());
        assertEquals(42, record.lastObserved());
        assertEquals(Set.of(observer), record.observers());
        assertEquals(Set.of(ObservationProviderRegistry.NOTEBOOK_CHANNEL), record.channels());
        assertEquals("dawn", record.contextFacts().get("time_period"));
        assertTrue(record.publicFacets().contains(KnowledgeRecord.BLOCK_OBSERVATION_FACET));
        assertTrue(record.sealedFacts().isEmpty());
    }

    @Test
    void selectedContextFieldsAndEntityFallbackRemainTopicNeutral() {
        UUID observer = UUID.randomUUID();
        UUID entity = UUID.randomUUID();
        ObservationContext context = new ObservationContext(ObservationContext.TargetType.ENTITY,
                DIMENSION, new BlockPos(2, 65, 4), new ResourceLocation("minecraft", "sheep"),
                Blocks.AIR.defaultBlockState(), 13000, 13000, observer,
                ObservationProviderRegistry.NOTEBOOK_CHANNEL, new ResourceLocation("minecraft", "snowy_plains"),
                ObservationContext.Weather.CLEAR,
                Set.of(ObservationContext.Field.TIME, ObservationContext.Field.BIOME), Map.of());

        KnowledgeRecord record = ObservationProviderRegistry.observeEntity(context, entity);

        assertEquals(KnowledgeRecord.Type.ENTITY, record.type());
        assertEquals("dusk", record.contextFacts().get("time_period"));
        assertEquals("minecraft:snowy_plains", record.contextFacts().get("biome"));
        assertFalse(record.contextFacts().containsKey("position"));
        assertEquals(entity.toString(), record.contextFacts().get("entity_uuid"));
    }

    @Test
    void genericDedupDistinguishesPositionAndVisibleState() {
        UUID observer = UUID.randomUUID();
        KnowledgeRecord first = ObservationProviderRegistry.observeBlock(null,
                context(new BlockPos(7, 64, -3), observer, false));
        KnowledgeRecord same = ObservationProviderRegistry.observeBlock(null,
                context(new BlockPos(7, 64, -3), observer, false));
        KnowledgeRecord changedState = ObservationProviderRegistry.observeBlock(null,
                context(new BlockPos(7, 64, -3), observer, true));
        KnowledgeRecord changedPosition = ObservationProviderRegistry.observeBlock(null,
                context(new BlockPos(8, 64, -3), observer, false));

        assertEquals(first.semanticKey(), same.semanticKey());
        assertNotEquals(first.semanticKey(), changedState.semanticKey());
        assertNotEquals(first.semanticKey(), changedPosition.semanticKey());
    }

    @Test
    void providerCellPolicyCanMergeARegionWithoutChangingGenericPolicy() {
        ObservationDeduplication policy = ObservationDeduplication.cell("sample", 16, 16, 16);
        Map<String, String> state = Map.of();
        String first = policy.semanticKey(KnowledgeRecord.ROCK_SAMPLE_KIND,
                context(new BlockPos(1, 2, 3), UUID.randomUUID(), false), state);
        String sameCell = policy.semanticKey(KnowledgeRecord.ROCK_SAMPLE_KIND,
                context(new BlockPos(15, 15, 15), UUID.randomUUID(), true), state);
        String otherCell = policy.semanticKey(KnowledgeRecord.ROCK_SAMPLE_KIND,
                context(new BlockPos(16, 15, 15), UUID.randomUUID(), false), state);

        assertEquals(first, sameCell);
        assertNotEquals(first, otherCell);
    }

    @Test
    void codecKeepsPublicObservationDataAndExplicitRedactionDropsOnlySealedFacts() {
        ResourceLocation mineral = new ResourceLocation("minecraft", "copper_ore");
        KnowledgeRecord original = KnowledgeRecord.create(KnowledgeRecord.Type.ROCK_SAMPLE,
                KnowledgeRecord.ROCK_SAMPLE_KIND, DIMENSION, BlockPos.ZERO,
                new ResourceLocation("minecraft", "stone"), Map.of("variant", "plain"),
                Map.of("biome", "minecraft:snowy_plains", "weather", "clear"), 9, UUID.randomUUID(),
                Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET, KnowledgeRecord.ROCK_SAMPLE_FACET),
                Set.of(ObservationProviderRegistry.NOTEBOOK_CHANNEL),
                Optional.of(new OreProspectingModel.Snapshot(Map.of(mineral, 3))), "sample-key");

        Tag encoded = KnowledgeRecord.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        KnowledgeRecord decoded = KnowledgeRecord.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });

        assertEquals(original, decoded);
        KnowledgeRecord publicCopy = original.withoutSealedFacts();
        assertTrue(publicCopy.sealedFacts().isEmpty());
        assertEquals(original.kindId(), publicCopy.kindId());
        assertEquals(original.stateProperties(), publicCopy.stateProperties());
        assertEquals(original.contextFacts(), publicCopy.contextFacts());
        assertEquals(original.publicFacets(), publicCopy.publicFacets());
        Tag publicTag = KnowledgeRecord.CODEC.encodeStart(NbtOps.INSTANCE, publicCopy)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertFalse(publicTag.toString().contains("copper_ore"));
    }

    @Test
    void mergingAProviderKeyKeepsAllSourcesAndLatestVisibleState() {
        UUID firstObserver = UUID.randomUUID();
        UUID secondObserver = UUID.randomUUID();
        ResourceLocation firstChannel = id("field_note");
        ResourceLocation secondChannel = id("work_report");
        ResourceLocation extraFacet = id("changed_state");
        KnowledgeRecord first = KnowledgeRecord.create(KnowledgeRecord.Type.BLOCK,
                KnowledgeRecord.BLOCK_KIND, DIMENSION, BlockPos.ZERO, OAK_DOOR,
                Map.of("open", "false"), 10, firstObserver,
                Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET), Set.of(firstChannel),
                Optional.empty(), "shared-key");
        KnowledgeRecord second = KnowledgeRecord.create(KnowledgeRecord.Type.BLOCK,
                KnowledgeRecord.BLOCK_KIND, DIMENSION, BlockPos.ZERO, OAK_DOOR,
                Map.of("open", "true"), 20, secondObserver,
                Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET, extraFacet), Set.of(secondChannel),
                Optional.empty(), "shared-key");

        KnowledgeRecord merged = first.merge(second);

        assertEquals(Set.of(firstObserver, secondObserver), merged.observers());
        assertEquals(Set.of(firstChannel, secondChannel), merged.channels());
        assertTrue(merged.publicFacets().contains(extraFacet));
        assertEquals("true", merged.stateProperties().get("open"));
        assertEquals(10, merged.firstObserved());
        assertEquals(20, merged.lastObserved());
    }

    private static ObservationContext context(BlockPos position, UUID observer, boolean open) {
        return new ObservationContext(DIMENSION, position, OAK_DOOR,
                Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.OPEN, open),
                42, observer, ObservationProviderRegistry.NOTEBOOK_CHANNEL);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
