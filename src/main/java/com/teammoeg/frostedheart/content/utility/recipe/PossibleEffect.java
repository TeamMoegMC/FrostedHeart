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

package com.teammoeg.frostedheart.content.utility.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.io.CodecUtil;

import net.minecraft.world.effect.MobEffectInstance;

public record PossibleEffect(java.util.function.Supplier<MobEffectInstance> effectSupplier, float probability) {
    public static final Codec<PossibleEffect> CODEC = RecordCodecBuilder.create(
        p_337893_ -> p_337893_.group(
                    CodecUtil.MOB_EFFECT_CODEC.fieldOf("effect").forGetter(PossibleEffect::effect),
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(PossibleEffect::probability)
                )
                .apply(p_337893_, PossibleEffect::new)
    );

    private PossibleEffect(MobEffectInstance effect, float probability) {
        this(() -> effect, probability);
    }

    public MobEffectInstance effect() {
        return new MobEffectInstance(this.effectSupplier.get());
    }
}