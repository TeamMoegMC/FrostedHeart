/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * 每条规则只保留首次实际输入绑定。 / First actual binding of each discovered rule.
 */
public record LinkDiscovery(ResourceLocation rule, KnowledgeKey output, Map<String, KnowledgeKey> inputs, long time) {
    public static final Codec<LinkDiscovery> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("rule").forGetter(LinkDiscovery::rule),
            KnowledgeKey.CODEC.fieldOf("output").forGetter(LinkDiscovery::output),
            Codec.unboundedMap(Codec.STRING, KnowledgeKey.CODEC).fieldOf("inputs").forGetter(LinkDiscovery::inputs),
            Codec.LONG.fieldOf("time").forGetter(LinkDiscovery::time)
    ).apply(i, LinkDiscovery::new));

    public LinkDiscovery {
        inputs = Map.copyOf(inputs);
    }
}
