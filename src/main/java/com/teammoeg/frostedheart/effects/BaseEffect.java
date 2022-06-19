package com.teammoeg.frostedheart.effects;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class BaseEffect extends Effect {

	public BaseEffect(EffectType typeIn, int liquidColorIn) {
		super(typeIn, liquidColorIn);
	}

	@Override
	public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
	}

	@Override
	public boolean isReady(int duration, int amplifier) {
		return false;
	}

	@Override
	public void affectEntity(Entity source, Entity indirectSource, LivingEntity entityLivingBaseIn, int amplifier,
			double health) {
	}
	@Override
	public List<ItemStack> getCurativeItems() {
		return ImmutableList.of();
	}
}
