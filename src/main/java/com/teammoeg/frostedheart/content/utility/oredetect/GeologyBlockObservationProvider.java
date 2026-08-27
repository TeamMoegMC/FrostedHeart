/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.utility.oredetect;

import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationContext;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationDeduplication;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.Set;

/** Frosted Heart's geology enrichment for the generic block-observation channel. */
public final class GeologyBlockObservationProvider implements ObservationProvider {
    public static final ResourceLocation ID = new ResourceLocation("frostedheart", "geology_block");
    public static final ResourceLocation ORE_FACET = new ResourceLocation("frostedheart", "geology/ore");
    public static final ResourceLocation STONE_FACET = new ResourceLocation("frostedheart", "geology/stone");
    private static final TagKey<Block> ORES = BlockTags.create(new ResourceLocation("forge", "ores"));
    private static final TagKey<Block> STONE = BlockTags.create(new ResourceLocation("forge", "stone"));

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public boolean supports(ObservationContext context) {
        return context.targetType() == ObservationContext.TargetType.BLOCK
                && (context.state().is(ORES) || context.state().is(STONE));
    }

    @Override
    public Contribution observe(BlockGetter level, ObservationContext context) {
        if (context.state().is(ORES)) {
            return new Contribution(KnowledgeRecord.COPPER_OUTCROP_KIND,
                    KnowledgeRecord.Type.COPPER_OUTCROP,
                    Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET,
                            KnowledgeRecord.COPPER_OUTCROP_FACET, ORE_FACET),
                    ObservationDeduplication.cell(KnowledgeRecord.Type.COPPER_OUTCROP.name(), 16, 16, 16),
                    Optional.empty());
        }
        return new Contribution(KnowledgeRecord.ROCK_SAMPLE_KIND,
                KnowledgeRecord.Type.ROCK_SAMPLE,
                Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET,
                        KnowledgeRecord.ROCK_SAMPLE_FACET, STONE_FACET),
                ObservationDeduplication.cell(KnowledgeRecord.Type.ROCK_SAMPLE.name(), 16, 16, 16),
                Optional.of(OreProspectingModel.scan(level, context.position(), 4, 3)));
    }
}
