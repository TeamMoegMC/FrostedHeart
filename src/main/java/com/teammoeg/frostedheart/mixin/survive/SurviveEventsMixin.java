/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.mixin.survive;

import com.stereowalker.survive.config.Config;
import com.stereowalker.survive.entity.SurviveEntityStats;
import com.stereowalker.survive.events.SurviveEvents;
import com.stereowalker.survive.potion.SEffects;
import com.stereowalker.survive.util.TemperatureStats;
import com.teammoeg.frostedheart.climate.IHeatingEquipment;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;
import com.teammoeg.frostedheart.climate.SurviveTemperature;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.compat.CuriosCompat;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SurviveEvents.class)
public class SurviveEventsMixin {
	/**
	 * @author yuesha-yc
	 * @reason Add our chunk temperature logic
	 */
	@Overwrite(remap = false)
	@SubscribeEvent
	public static void updateTemperature(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote
				&& event.getEntityLiving() instanceof ServerPlayerEntity) {

			ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
			if (player.ticksExisted % 10 != 0)
				return;
			float current = SurviveTemperature.getBodyTemperature(player);
			if (current < 0)
				current += 0.05;
			World world = player.getEntityWorld();
			BlockPos pos = player.getPosition();
			float envtemp = ChunkData.getTemperature(world, pos);
			float skyLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.SKY).getLightFor(pos);
			float gameTime = world.getDayTime() % 24000L;
			gameTime = gameTime / (200 / 3);
			gameTime = (float) Math.sin(Math.toRadians(gameTime));
			envtemp += SurviveTemperature.getBlockTemp(world, pos);
			envtemp += skyLight > 5.0F ? (gameTime * 5.0F) : (-1.0F * 5.0F);
			envtemp -= 37F;// normalize
			float keepwarm = 0;
			for (ItemStack is : CuriosCompat.getAllCuriosIfVisible(player)) {
				if (is == null)
					continue;
				Item it = is.getItem();
				if (it instanceof IHeatingEquipment)
					current = ((IHeatingEquipment) it).compute(is, current, envtemp);
				if (it instanceof IWarmKeepingEquipment)
					keepwarm += ((IWarmKeepingEquipment) it).getFactor(is);
			}
			for (ItemStack is : player.getArmorInventoryList()) {
				if (is == null)
					continue;
				Item it = is.getItem();
				if (it instanceof IHeatingEquipment)
					current = ((IHeatingEquipment) it).compute(is, current, envtemp);
				if (it instanceof IWarmKeepingEquipment)
					keepwarm += ((IWarmKeepingEquipment) it).getFactor(is);
			}
			if (keepwarm > 1)
				keepwarm = 1;
			current += 0.0012 * (1 - keepwarm) * (envtemp - current);
			SurviveTemperature.setBodyTemperature(player, current);
			//TemperatureStats ts = SurviveEntityStats.getTemperatureStats(player);
			//ts.setTemperatureLevel(50);
			/*
			 * SurviveTemperature.resetTState(ts);
			 * TemperatureStats.setTemperatureModifier(player, "survive:all",current);
			 * ts.setTemperatureLevel((int)(current+37F));
			 */
			
			  /*if (player.ticksExisted %20==0) {
			  System.out.println(current);
			  }*/
			 
		}
	}

	/**
	 * @author khjxiaogu
	 * @reason overwrite
	 */
	@Overwrite(remap = false)
	@SubscribeEvent
	public static void updateEnvTemperature(LivingUpdateEvent event) {

	}

	/**
	 * @author khjxiaogu
	 * @reason overwrite
	 */
	@Overwrite(remap = false)
	@SubscribeEvent
	public static void regulateTemperature(LivingUpdateEvent event) {
		if (event.getEntityLiving() != null && !(event.getEntityLiving()).world.isRemote
				&& event.getEntityLiving() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
			double calculatedTarget = SurviveTemperature.getBodyTemperature(player) + 37;
			if (!(player.isCreative() || player.isSpectator())) {
				if (calculatedTarget > 37.8 || calculatedTarget < 36) {
					
					if (!player.isPotionActive(SEffects.HYPERTHERMIA)
							&& !player.isPotionActive(SEffects.HYPOTHERMIA)) {
						
						if (calculatedTarget > 37.8) {
							if (calculatedTarget < 38.5) {
								player.addPotionEffect(new EffectInstance(SEffects.HYPERTHERMIA, 100, 0));
							} else if (calculatedTarget < 39.5) {
								player.addPotionEffect(new EffectInstance(SEffects.HYPERTHERMIA, 100, 1));
							} else {
								player.addPotionEffect(new EffectInstance(SEffects.HYPERTHERMIA, 100, 2));
							}
						} else if (calculatedTarget >= 35) {
							player.addPotionEffect(new EffectInstance(SEffects.HYPOTHERMIA, 100, 0));
						} else if (calculatedTarget >= 34) {
							player.addPotionEffect(new EffectInstance(SEffects.HYPOTHERMIA, 100, 1));
						} else {
							player.addPotionEffect(new EffectInstance(SEffects.HYPOTHERMIA, 100, 2));
						}
					}
					}
			}
		}
	}
}
