package com.teammoeg.frostedheart.effects;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHDamageSources;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class IonEffect extends Effect {

	public IonEffect(EffectType typeIn, int liquidColorIn) {
		super(typeIn, liquidColorIn);
	}

	@Override
	public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
		entityLivingBaseIn.attackEntityFrom(FHDamageSources.RAD, (float) (1+(amplifier)*0.5));
	}

	@Override
	public boolean isReady(int duration, int amplifier) {
		return duration%(100/(amplifier+1))==0;
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		return ImmutableList.of();
	}

}
