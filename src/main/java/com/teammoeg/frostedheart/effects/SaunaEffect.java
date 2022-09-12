package com.teammoeg.frostedheart.effects;


import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

import java.util.List;

public class SaunaEffect extends Effect {

    protected SaunaEffect(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            EnergyCore.addEnergy(player, 9);
        }
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
