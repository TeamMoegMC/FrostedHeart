/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.observation;

import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;

import java.util.Optional;
import java.util.Set;

/** Catch-all provider: every real block has an observable identity, state and location. */
public final class GenericBlockObservationProvider implements ObservationProvider {
    public static final ResourceLocation ID = new ResourceLocation("frostedresearch", "generic_block");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean supports(ObservationContext context) {
        return context.targetType() == ObservationContext.TargetType.BLOCK;
    }

    @Override
    public Contribution observe(BlockGetter level, ObservationContext context) {
        return new Contribution(KnowledgeRecord.BLOCK_KIND, KnowledgeRecord.Type.BLOCK,
                Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET),
                ObservationDeduplication.exactBlock(), Optional.empty());
    }
}
