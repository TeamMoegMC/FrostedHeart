package com.teammoeg.frostedresearch.knowledge.item;

import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.model.ObservationValue;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResearchNotesTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void copiedHistoricalRecordPreservesIdentityCompleteSnapshotAndOriginalSource() {
        Observation observation = historicalObservation();
        AcquisitionSource origin = new AcquisitionSource("npc_dialogue", Optional.of(UUID.randomUUID()), 900);
        var encoded = ResearchNotes.encodeContent(KnowledgeElement.observation(observation), Optional.of(origin), "Old snowfall", "Found in ruins").orElseThrow();
        var copied = ResearchNotes.decodeContent(encoded.copy()).orElseThrow();
        assertEquals(observation, copied.element().observation().orElseThrow());
        assertEquals(observation.key(), copied.element().key());
        assertEquals(origin, copied.originalSource().orElseThrow());
        assertEquals(ObservationValue.State.UNKNOWN, copied.element().observation().orElseThrow().value("time").state());
        assertEquals(-18.75, copied.element().observation().orElseThrow().value("temperature").number().orElseThrow().doubleValue());
    }

    @Test
    void carrierAnnotationsCannotCreateNewEvidenceOrFillMissingOrigin() {
        KnowledgeElement element = KnowledgeElement.observation(historicalObservation());
        var first = ResearchNotes.decodeContent(ResearchNotes.encodeContent(element, Optional.empty(), "Original", "").orElseThrow()).orElseThrow();
        var second = ResearchNotes.decodeContent(ResearchNotes.encodeContent(first.element(), first.originalSource(), "A new title", "Different owner").orElseThrow()).orElseThrow();
        assertEquals(first.element(), second.element());
        assertEquals(first.element().key(), second.element().key());
        assertNotEquals(first.title(), second.title());
        assertTrue(second.originalSource().isEmpty());
    }

    @Test
    void itemObservationsCannotBeWrittenOrReadAsResearchNotes() {
        Observation item = new Observation(UUID.randomUUID(), Observation.Type.ITEM, new ResourceLocation("minecraft", "oak_log"),
                Map.of("item_block", ObservationValue.known("minecraft:oak_log")), Observation.Source.command(), Optional.empty());
        KnowledgeElement element = KnowledgeElement.observation(item);
        assertFalse(ResearchNotes.canExport(element));
        assertTrue(ResearchNotes.encodeContent(element, Optional.empty(), "", "").isEmpty());
        CompoundTag externallySuppliedNote = new CompoundTag();
        externallySuppliedNote.put("element", KnowledgeElement.CODEC.encodeStart(NbtOps.INSTANCE, element).result().orElseThrow());
        assertTrue(ResearchNotes.decodeContent(externallySuppliedNote).isEmpty());
        assertEquals(ObservationValue.State.NOT_APPLICABLE, item.value("temperature").state());
        assertEquals(ObservationValue.State.NOT_APPLICABLE, item.value("block_state.axis").state());
    }

    private static Observation historicalObservation() {
        return new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("minecraft", "snow"),
                Map.of("temperature", ObservationValue.known(-18.75), "climate", ObservationValue.known("snow"),
                        "block_state.layers", ObservationValue.known("3"), "time", ObservationValue.unknown(ObservationValue.ValueType.NUMBER)),
                new Observation.Source("npc_observation", Optional.of(UUID.randomUUID()), Optional.of(UUID.randomUUID()),
                        Optional.of(new ResourceLocation("minecraft", "overworld")), Optional.of(new BlockPos(100, 64, -30))), Optional.empty());
    }
}
