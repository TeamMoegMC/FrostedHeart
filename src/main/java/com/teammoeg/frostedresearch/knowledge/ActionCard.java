/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/** One data-addressable next action shown after an idea has been recorded. */
public record ActionCard(ResourceLocation topicId, ResourceLocation protocolId,
        ResourceLocation actionId, boolean executable) {
    public static final Codec<ActionCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("topic").forGetter(ActionCard::topicId),
            ResourceLocation.CODEC.fieldOf("protocol").forGetter(ActionCard::protocolId),
            ResourceLocation.CODEC.fieldOf("action").forGetter(ActionCard::actionId),
            Codec.BOOL.optionalFieldOf("executable", false).forGetter(ActionCard::executable)
    ).apply(instance, ActionCard::new));

    public ActionCard(ResourceLocation topicId, ResourceLocation protocolId, ResourceLocation actionId) {
        this(topicId, protocolId, actionId, false);
    }
}
