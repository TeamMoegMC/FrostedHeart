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

package com.teammoeg.frostedheart.climate;

import java.util.ArrayList;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.compat.CuriosCompat;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.network.FHDataSyncPacket;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.util.FHDamageSources;
import com.teammoeg.frostedheart.util.FHEffects;
import com.teammoeg.frostedheart.util.FHUtils;

import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

@Mod.EventBusSubscriber
public class TemperatureUpdate {
	public static final float HEAT_EXCHANGE_CONSTANT = 0.0012F;
	public static final float SELF_HEATING_CONSTANT = 0.036F;
	private static final class HeatingEquipment{
		IHeatingEquipment e;
		ItemStack i;
		public HeatingEquipment(IHeatingEquipment e, ItemStack i) {
			this.e = e;
			this.i = i;
		}
		public float compute(float body,float env) {
			return e.compute(i, body, env);
		}
	}
	/**
	 * Perform temperature tick logic
	 *
	 * @param event fired every tick on player
	 */
	@SubscribeEvent
	public static void updateTemperature(PlayerTickEvent event) {
		if (event.side == LogicalSide.SERVER && event.phase == Phase.START
				&& event.player instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.player;
			if (player.ticksExisted % 10 != 0 || player.isCreative() || player.isSpectator())
				return;
			//soak in water modifier
			if (player.isInWater()) {
				boolean hasArmor = false;
				for (ItemStack is : player.getArmorInventoryList()) {
					if (!is.isEmpty()) {
						hasArmor = true;
						break;
					}
				}
				EffectInstance current = player.getActivePotionEffect(FHEffects.WET);
				if (hasArmor)
					player.addPotionEffect(new EffectInstance(FHEffects.WET, 400, 0));// punish for wet clothes
				else if (current == null || current.getDuration() < 100)
					player.addPotionEffect(new EffectInstance(FHEffects.WET, 100, 0));
			}
			//load current data
			float current = TemperatureCore.getBodyTemperature(player);
			double tspeed = FHConfig.SERVER.tempSpeed.get();
			if (current < 0)
				current += FHConfig.SERVER.tdiffculty.get().self_heat.apply(player) * tspeed;
			//world and chunk temperature
			World world = player.getEntityWorld();
			BlockPos pos = player.getPosition();
			float envtemp = ChunkData.getTemperature(world, pos);
			//time temperature
			float skyLight = world.getChunkProvider().getLightManager().getLightEngine(LightType.SKY).getLightFor(pos);
			float gameTime = world.getDayTime() % 24000L;
			gameTime = gameTime / (200 / 3);
			gameTime = (float) Math.sin(Math.toRadians(gameTime));
			envtemp += TemperatureCore.getBlockTemp(world, pos);
			envtemp += skyLight > 5.0F ?(FHUtils.isRainingAt(pos,world)?-8F:(gameTime * 5.0F)) : (-5.0F);
			// burning heat
			if (player.isBurning())
				envtemp += 150F;
			// normalize
			envtemp -= 37F;
			float keepwarm = 0;
			//list of equipments to be calculated
			ArrayList<HeatingEquipment> equipments=new ArrayList<>(7);
			for (ItemStack is : CuriosCompat.getAllCuriosIfVisible(player)) {
				if (is == null)
					continue;
				Item it = is.getItem();
				if (it instanceof IHeatingEquipment)
					equipments.add(new HeatingEquipment((IHeatingEquipment) it,is));
				if (it instanceof IWarmKeepingEquipment) {//only for direct warm keeping
					keepwarm += ((IWarmKeepingEquipment) it).getFactor(player, is);
				} else {
					IWarmKeepingEquipment iw = FHDataManager.getArmor(is);
					if (iw != null)
						keepwarm += iw.getFactor(player, is);
				}
			}
			for (ItemStack is : player.getArmorInventoryList()) {
				if (is.isEmpty())
					continue;
				Item it = is.getItem();
				if (it instanceof IHeatingEquipment)
					equipments.add(new HeatingEquipment((IHeatingEquipment) it,is));
				if (it instanceof IWarmKeepingEquipment) {
					keepwarm += ((IWarmKeepingEquipment) it).getFactor(player, is);
				} else {//include inner
					String s = ItemNBTHelper.getString(is, "inner_cover");
					IWarmKeepingEquipment iw = null;
					EquipmentSlotType aes = MobEntity.getSlotForItemStack(is);
					if (s.length() > 0 && aes != null) {
						iw = FHDataManager.getArmor(s + "_" + aes.getName());
					} else
						iw = FHDataManager.getArmor(is);
					if (iw != null)
						keepwarm += iw.getFactor(player, is);
				}
			}
			{//main hand
				ItemStack hand = player.getHeldItemMainhand();
				Item it = hand.getItem();
				if (it instanceof IHeatingEquipment && ((IHeatingEquipment) it).canHandHeld())
					equipments.add(new HeatingEquipment((IHeatingEquipment) it,hand));
			}
			{//off hand
				ItemStack hand = player.getHeldItemOffhand();
				Item it = hand.getItem();
				if (it instanceof IHeatingEquipment && ((IHeatingEquipment) it).canHandHeld())
					equipments.add(new HeatingEquipment((IHeatingEquipment) it,hand));;
			}
			if (keepwarm > 1)//prevent negative
				keepwarm = 1;
			//environment heat exchange
			float dheat= HEAT_EXCHANGE_CONSTANT  * (1 - keepwarm) * (envtemp - current);
			//simulate temperature transform to get heating device working
			float simulated=(float) (current/tspeed+dheat);
			for(HeatingEquipment it:equipments) {
				float addi=it.compute(simulated,envtemp);
				dheat+=addi;
				simulated+=addi;
			}
			if(dheat>0.1)
				player.attackEntityFrom(FHDamageSources.HYPERTHERMIA_INSTANT,(dheat)*10);
			else if(dheat<-0.1)
				player.attackEntityFrom(FHDamageSources.HYPOTHERMIA_INSTANT,(-dheat)*10);
			current +=dheat* tspeed;
			if (current < -10)
				current = -10;
			else if (current > 10)
				current = 10;

			TemperatureCore.setTemperature(player, current, envtemp + 37);
			PacketHandler.send(PacketDistributor.PLAYER.with(() -> player), new FHDataSyncPacket(player));
		}
	}

	/**
	 * Perform temperature effect
	 *
	 * @param event fired every tick on player
	 */
	@SubscribeEvent
	public static void regulateTemperature(PlayerTickEvent event) {
		if (event.side == LogicalSide.SERVER && event.phase == Phase.END
				&& event.player instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.player;
			double calculatedTarget = TemperatureCore.getBodyTemperature(player);
			if (!(player.isCreative() || player.isSpectator())) {
				if (calculatedTarget > 1 || calculatedTarget < -1) {
					if (!player.isPotionActive(FHEffects.HYPERTHERMIA)
							&& !player.isPotionActive(FHEffects.HYPOTHERMIA)) {
						if (calculatedTarget > 1) { // too hot
							if (calculatedTarget <= 2) {
								player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, 0));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							} else if (calculatedTarget <= 3) {
								player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, 1));
								player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							} else if (calculatedTarget <= 5) {
								player.addPotionEffect(new EffectInstance(FHEffects.HYPERTHERMIA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							} else {
								player.addPotionEffect(
										new EffectInstance(FHEffects.HYPERTHERMIA, 100, (int) (calculatedTarget - 2)));
								player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							}
						} else { // too cold
							if (calculatedTarget >= -2) {
								player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, 0));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							} else if (calculatedTarget >= -3) {
								player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, 1));
								player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							} else if (calculatedTarget >= -5) {
								player.addPotionEffect(new EffectInstance(FHEffects.HYPOTHERMIA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							} else {
								player.addPotionEffect(
										new EffectInstance(FHEffects.HYPOTHERMIA, 100, (int) (-calculatedTarget - 2)));
								player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 100, 2));
								player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 0));
							}
						}
					}
				}
			}
		}
	}
}
