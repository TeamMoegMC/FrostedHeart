package com.teammoeg.frostedheart.effects;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.util.FHUtils;
import gloridifice.watersource.registry.EffectRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;

import java.util.List;

public class AnemiaEffect extends Effect {

    public AnemiaEffect(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(Effects.SLOWNESS, 100, amplifier)));
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(Effects.MINING_FATIGUE, 100, amplifier)));
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(Effects.WEAKNESS, 100, amplifier * 2)));
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(EffectRegistry.THIRST, 100, amplifier * 2)));
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration % 100 == 0;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return ImmutableList.of();
    }
}
