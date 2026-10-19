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
 *
 */

package com.teammoeg.frostedresearch.knowledge.state;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.knowledge.model.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeStateTest {
    static final AcquisitionSource SOURCE = new AcquisitionSource("note", Optional.empty(), 12);
    static final UUID PLAYER = UUID.fromString("5f0c8a52-3c4f-4d0e-9f62-1d3b7b0e6a11");
    static final KnowledgeEntry IDEA = new KnowledgeEntry(KnowledgeElement.idea(id("fire")), SOURCE, 5, "");
    static final KnowledgeEntry WARM = observation(-5), COLD = observation(-30);

    static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }

    static Observation block(Map<String, ObservationValue> values) {
        return new Observation(UUID.randomUUID(), Observation.Type.BLOCK, id("rock"), values, Observation.Source.command(), Optional.empty());
    }

    static KnowledgeEntry observation(double temperature) {
        return new KnowledgeEntry(KnowledgeElement.observation(block(Map.of("temperature", ObservationValue.known(temperature)))), SOURCE, 5, "");
    }

    /**
     * One entry in every saved section: archive, inbox, links, research, acquisitions, hints, daily and dream topics.
     */
    static KnowledgeState everySection() {
        KnowledgeState state = new KnowledgeState();
        state.putArchive(IDEA);
        state.putArchive(WARM);
        state.putInbox(COLD);
        state.discover(new LinkDiscovery(id("link"), IDEA.element().key(), Map.of("a", WARM.element().key()), 7));
        state.recordResearch(new ResearchHistory(UUID.randomUUID(), id("kiln"), IDEA.element().key(), 8, true, List.of(KnowledgeKey.result(id("kiln")))));
        state.recordAcquisition(WARM);
        state.addHint(id("link"), "warmth matters");
        state.markDay("dream:" + PLAYER, 3);
        state.dreamTopic(PLAYER, IDEA.element().key());
        state.initialize();
        return state;
    }

    /**
     * Stands in for a record saved before {@code temperature} changed type, which this version can no longer decode.
     */
    static CompoundTag retyped(CompoundTag entry) {
        CompoundTag copy = entry.copy();
        copy.getCompound("element").getCompound("observation").getCompound("values")
                .put("temperature", KnowledgeState.encode(ObservationValue.CODEC, ObservationValue.known("cold")));
        return copy;
    }

    @Test
    void readableStateRoundTripsToTheSameTag() {
        CompoundTag saved = everySection().write();
        assertEquals(KnowledgeState.SCHEMA_VERSION, saved.getInt("schema_version"));
        assertEquals(saved, KnowledgeState.read(saved).write());
    }

    @Test
    void undecodableArchiveEntryIsSetAsideWhileTheRestLoads() {
        CompoundTag saved = everySection().write();
        ListTag archive = saved.getList("archive", Tag.TAG_COMPOUND);
        archive.set(1, retyped(archive.getCompound(1)));

        KnowledgeState loaded = KnowledgeState.read(saved);

        assertEquals(List.of(IDEA.element().key()), List.copyOf(loaded.archive().entries().keySet()));
        assertFalse(loaded.inbox().contains(WARM.element().key()));
        assertEquals(COLD, loaded.inbox().entries().get(COLD.element().key()));
        assertEquals(1, loaded.links().size());
        assertEquals(1, loaded.research().size());
        assertEquals(List.of(WARM), loaded.acquisitions());
        assertEquals(List.of("warmth matters"), loaded.hints().get(id("link")));
        assertEquals(IDEA.element().key(), loaded.dreamTopic(PLAYER));
        assertEquals(3, loaded.lastDay("dream:" + PLAYER));
        assertTrue(loaded.initialized());
    }

    @Test
    void unreadableEntriesSurviveSaveLoadReplaceAndMerge() {
        CompoundTag saved = everySection().write();
        CompoundTag archived = retyped(saved.getList("archive", Tag.TAG_COMPOUND).getCompound(1));
        saved.getList("archive", Tag.TAG_COMPOUND).set(1, archived);
        CompoundTag acquired = retyped(saved.getList("acquisitions", Tag.TAG_COMPOUND).getCompound(0));
        saved.getList("acquisitions", Tag.TAG_COMPOUND).set(0, acquired);
        CompoundTag link = saved.getList("links", Tag.TAG_COMPOUND).getCompound(0);
        link.putString("rule", "Not A Rule");
        CompoundTag research = saved.getList("research", Tag.TAG_COMPOUND).getCompound(0);
        research.getCompound("idea").putString("kind", "rumour");
        ListTag hint = new ListTag();
        hint.add(StringTag.valueOf("kept anyway"));
        saved.getCompound("hints").put("Bad Rule", hint);
        Tag topic = saved.getCompound("dream_topics").get(PLAYER.toString()).copy();
        saved.getCompound("dream_topics").put("not-a-uuid", topic);

        KnowledgeState loaded = KnowledgeState.read(saved);
        assertTrue(loaded.links().isEmpty());
        assertTrue(loaded.research().isEmpty());
        assertTrue(loaded.acquisitions().isEmpty());
        assertEquals(Set.of(id("link")), loaded.hints().keySet());
        assertEquals(IDEA.element().key(), loaded.dreamTopic(PLAYER));

        CompoundTag resaved = loaded.write();
        assertKept(resaved, archived, acquired, link, research, hint, topic);
        assertEquals(2, resaved.getList("archive", Tag.TAG_COMPOUND).size());

        KnowledgeState reloaded = KnowledgeState.read(resaved);
        assertFalse(reloaded.archive().contains(WARM.element().key()));
        assertEquals(resaved, reloaded.write());

        TeamKnowledgeData replaced = TeamKnowledgeData.CODEC.parse(NbtOps.INSTANCE, resaved)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertKept(replaced.copy().write(), archived, acquired, link, research, hint, topic);

        KnowledgeState target = new KnowledgeState();
        target.merge(loaded);
        target.merge(loaded);
        CompoundTag merged = target.write();
        assertKept(merged, archived, acquired, link, research, hint, topic);
        assertEquals(1, merged.getList("acquisitions", Tag.TAG_COMPOUND).size());
        assertEquals(1, merged.getList("links", Tag.TAG_COMPOUND).size());
    }

    @Test
    void observationWithOneUnreadableValueIsSetAsideRatherThanEmptied() {
        CompoundTag saved = everySection().write();
        CompoundTag warm = saved.getList("archive", Tag.TAG_COMPOUND).getCompound(1);
        warm.getCompound("element").getCompound("observation").getCompound("values").getCompound("temperature")
                .putString("state", "estimated");

        KnowledgeState loaded = KnowledgeState.read(saved);

        assertFalse(loaded.archive().contains(WARM.element().key()));
        assertTrue(loaded.write().getList("archive", Tag.TAG_COMPOUND).contains(warm));
    }

    static void assertKept(CompoundTag saved, CompoundTag archived, CompoundTag acquired, CompoundTag link,
                           CompoundTag research, Tag hint, Tag topic) {
        assertTrue(saved.getList("archive", Tag.TAG_COMPOUND).contains(archived));
        assertTrue(saved.getList("acquisitions", Tag.TAG_COMPOUND).contains(acquired));
        assertTrue(saved.getList("links", Tag.TAG_COMPOUND).contains(link));
        assertTrue(saved.getList("research", Tag.TAG_COMPOUND).contains(research));
        assertEquals(hint, saved.getCompound("hints").get("Bad Rule"));
        assertEquals(topic, saved.getCompound("dream_topics").get("not-a-uuid"));
    }

    @Test
    void missingOrNewerSchemaVersionAndMisshapenSectionsStillDecode() {
        CompoundTag saved = everySection().write();
        saved.remove("schema_version");
        assertEquals(2, KnowledgeState.read(saved).archive().entries().size());

        saved.putInt("schema_version", KnowledgeState.SCHEMA_VERSION + 1);
        saved.putString("research", "not a list");
        saved.putString("hints", "not a compound");
        KnowledgeState loaded = KnowledgeState.read(saved);
        assertEquals(2, loaded.archive().entries().size());
        assertTrue(loaded.research().isEmpty());
        assertEquals(KnowledgeState.SCHEMA_VERSION, loaded.write().getInt("schema_version"));
    }

    @Test
    void observationThatCouldNotBeLoadedIsRejectedWhenBuilt() {
        assertThrows(IllegalArgumentException.class, () -> block(Map.of("temperature", ObservationValue.known("cold"))));
        assertThrows(IllegalArgumentException.class, () -> block(Map.of("colour", ObservationValue.known("red"))));
        assertThrows(IllegalArgumentException.class, () -> block(Map.of("temperature", ObservationValue.known(Double.NaN))));
        assertThrows(IllegalArgumentException.class, () -> new Observation(UUID.randomUUID(), Observation.Type.ITEM, id("stick"),
                Map.of("biome", ObservationValue.known("minecraft:plains")), Observation.Source.command(), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new Observation(UUID.randomUUID(), Observation.Type.BLOCK, id("rock"),
                Map.of(), Observation.Source.command(), Optional.of(new CompoundTag())));
        assertDoesNotThrow(() -> new Observation(UUID.randomUUID(), Observation.Type.ITEM, id("stick"),
                Map.of("biome", ObservationValue.notApplicable(ObservationValue.ValueType.STRING)), Observation.Source.command(), Optional.empty()));

        // Saved data that breaks the same rule stays an ordinary codec error rather than an exception, and the cause
        // reaches the entry instead of reading as a missing observation.
        CompoundTag entry = retyped((CompoundTag) KnowledgeState.encode(KnowledgeEntry.CODEC, WARM));
        assertTrue(Observation.CODEC.parse(NbtOps.INSTANCE, entry.getCompound("element").get("observation")).error().isPresent());
        String error = KnowledgeEntry.CODEC.parse(NbtOps.INSTANCE, entry).error().orElseThrow().message();
        assertTrue(error.contains("mistyped observation field: temperature"), error);
    }
}
