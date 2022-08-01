package com.teammoeg.frostedheart;

import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.function.Function;

public enum FHDifficulty {
    Easy(s -> 0.05F),
    Normal(s -> 0.036F),
    Hard(s -> s.isSprinting() ? 0.036F : 0.024F),
    HardCore(s -> 0F);

    private FHDifficulty(Function<ServerPlayerEntity, Float> self_heat) {
        this.self_heat = self_heat;

    }

    public final Function<ServerPlayerEntity, Float> self_heat;
}
