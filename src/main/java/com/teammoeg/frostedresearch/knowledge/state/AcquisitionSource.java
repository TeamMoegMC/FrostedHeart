/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

/**
 * 队伍接收来源，不覆盖观察原始来源。 / Team acquisition, distinct from original authorship.
 */
public record AcquisitionSource(String mechanism, Optional<UUID> actor, long time) {
    public static final Codec<AcquisitionSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("mechanism").forGetter(AcquisitionSource::mechanism),
            UUIDUtil.CODEC.optionalFieldOf("actor").forGetter(AcquisitionSource::actor),
            Codec.LONG.fieldOf("time").forGetter(AcquisitionSource::time)
    ).apply(i, AcquisitionSource::new));

    public static AcquisitionSource of(String mechanism, ServerPlayer player) {
        return new AcquisitionSource(mechanism, Optional.of(player.getUUID()), player.serverLevel().getGameTime());
    }
}
