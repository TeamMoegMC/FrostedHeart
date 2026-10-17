/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Identity-only prototype profile used until host installation is implemented. */
public record PrototypeProfileDefinition(int format, int revision) {
    public static final int CURRENT_FORMAT = 1;
    public static final Codec<PrototypeProfileDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("format").forGetter(PrototypeProfileDefinition::format),
            Codec.INT.fieldOf("revision").forGetter(PrototypeProfileDefinition::revision)
    ).apply(instance, PrototypeProfileDefinition::new));
}
