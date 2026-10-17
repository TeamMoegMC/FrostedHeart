/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.frostedresearch.knowledge.client.observation.ObservationPresentation;
import com.teammoeg.frostedresearch.knowledge.item.ResearchNotes;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.KnowledgeState;
import net.minecraft.SharedConstants;
import net.minecraft.locale.Language;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.*;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgePresentationTest {
    private Language previous;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void language() throws Exception {
        previous = Language.getInstance();
        Map<String, String> words = new HashMap<>();
        try (var stream = Files.newInputStream(Path.of("src/main/resources/assets/frostedresearch/lang/zh_cn.json"))) {
            Language.loadFromJson(stream, words::put);
        }
        words.put("block.minecraft.white_bed", "白色床");
        words.put("biome.minecraft.plains", "平原");
        words.put("item.minecraft.paper", "纸");
        Language.inject(new Language() {
            @Override
            public String getOrDefault(String key, String fallback) {
                return words.getOrDefault(key, previous.getOrDefault(key, fallback));
            }

            @Override
            public boolean has(String key) {
                return words.containsKey(key) || previous.has(key);
            }

            @Override
            public boolean isDefaultRightToLeft() {
                return false;
            }

            @Override
            public FormattedCharSequence getVisualOrder(FormattedText text) {
                return FormattedCharSequence.forward(text.getString(), Style.EMPTY);
            }
        });
    }

    @AfterEach
    void restore() {
        Language.inject(previous);
    }

    static Observation bed() {
        return new Observation(UUID.fromString("d0aedf24-cad2-4735-ac0a-10825b9b2019"), Observation.Type.BLOCK, new ResourceLocation("minecraft:white_bed"), Map.ofEntries(
                Map.entry("dimension", ObservationValue.known("minecraft:overworld")), Map.entry("biome", ObservationValue.known("minecraft:plains")),
                Map.entry("x", ObservationValue.known(38d)), Map.entry("y", ObservationValue.known(-60d)), Map.entry("z", ObservationValue.known(38d)),
                Map.entry("time", ObservationValue.known(46099d)), Map.entry("year", ObservationValue.known(2060d)), Map.entry("month", ObservationValue.known(10d)), Map.entry("day", ObservationValue.known(11d)),
                Map.entry("time_period", ObservationValue.known("dawn")), Map.entry("temperature_type", ObservationValue.known("cold")), Map.entry("temperature", ObservationValue.known(-5.25))), Observation.Source.command(), Optional.empty());
    }

    @Test
    void fieldNoteUsesLocalizedSubjectAndCompactContextWithoutIdentityNoise() {
        Observation o = bed();
        assertEquals("白色床", ObservationPresentation.title(o).getString());
        String summary = String.join("\n", ObservationPresentation.summary(o).stream().map(Component::getString).toList());
        assertTrue(summary.contains("平原 · 主世界"));
        assertTrue(summary.contains("2060年10月11日"));
        assertTrue(summary.contains("清晨"));
        assertTrue(summary.contains("寒冷"));
        assertFalse(summary.contains(o.recordId().toString()));
        assertFalse(summary.contains("minecraft:"));
        assertFalse(summary.contains("46099"));
        assertFalse(summary.contains("-60"));
        assertFalse(summary.contains("-5.25"));
        String details = String.join("\n", ObservationPresentation.details(o).stream().map(Component::getString).toList());
        assertTrue(details.contains("-60"));
        assertTrue(details.contains("°C"));
        assertFalse(details.contains(o.recordId().toString()));
    }

    @Test
    void definitionsAndUnknownReferencesNeverLeakRegistryOrTranslationKeys() {
        var element = new KnowledgeElement(KnowledgeKey.idea(new ResourceLocation("knowledge_example:materials")), Optional.empty(), "knowledge.knowledge_example.ideas.materials.title", "knowledge.knowledge_example.ideas.materials.body");
        assertEquals("纸页与木棍", KnowledgePresentation.title(element).getString());
        assertFalse(KnowledgePresentation.body(element).getString().contains("示例"));
        assertEquals("尚未读懂的内容", KnowledgePresentation.text("minecraft:oak_log").getString());
        assertEquals("尚未读懂的内容", KnowledgePresentation.text("knowledge.missing.title").getString());
        assertEquals("手写的句子。", KnowledgePresentation.text("手写的句子。").getString());
    }

    @Test
    void snapshotIndexRetainsIdentityButUsesReadableSearchAndNoSelectionIsValid() {
        var element = KnowledgeElement.observation(bed());
        CompoundTag entry = new CompoundTag();
        entry.put("element", KnowledgeState.encode(KnowledgeElement.CODEC, element));
        entry.putString("status", "LEARNABLE");
        ListTag records = new ListTag();
        records.add(entry);
        CompoundTag snapshot = new CompoundTag();
        snapshot.put("inbox", records);
        var data = new KnowledgeJournalData(snapshot);
        assertNull(data.entry(null));
        assertEquals(1, data.inbox.size());
        assertEquals(element.key(), data.inbox.get(0).key());
        assertTrue(data.inbox.get(0).search().contains("白色床"));
        assertFalse(data.inbox.get(0).search().contains("minecraft:"));
        assertSame(data.entry(element.key()), data.entry(element.key()));
    }

    @Test
    void unknownMeasurementsAreOmittedAndNoteTitleUsesTheObjectName() {
        var o = new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("minecraft:white_bed"), Map.of(), Observation.Source.command(), Optional.empty());
        assertFalse(ObservationPresentation.details(o).stream().anyMatch(c -> c.getString().contains("°C")));
        var content = new ResearchNotes.Content(KnowledgeElement.observation(o), "", "", Optional.empty());
        assertEquals("观察笔记：白色床", ResearchNotes.title(content).getString());
    }
}
