/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.observation;

import com.teammoeg.frostedheart.content.utility.oredetect.OreProspectingModel;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;

import java.util.Optional;
import java.util.Set;

/** A bounded adapter that enriches a generic block observation with domain facts. */
public interface ObservationProvider {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    boolean supports(ObservationContext context);

    Contribution observe(BlockGetter level, ObservationContext context);

    record Contribution(ResourceLocation kindId, KnowledgeRecord.Type compatibilityType,
            Set<ResourceLocation> publicFacets, ObservationDeduplication deduplication,
            Optional<OreProspectingModel.Snapshot> sealedFacts) {
        public Contribution {
            publicFacets = Set.copyOf(publicFacets);
        }
    }
}
