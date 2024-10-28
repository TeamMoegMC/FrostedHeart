package com.teammoeg.frostedheart.recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

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