/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.health.handler;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.content.health.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.health.event.GatherFoodNutritionEvent;
import com.teammoeg.frostedheart.content.water.network.PlayerDrinkWaterMessage;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Level;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HealthCommonEvents {
    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        Player player = event.getEntity();
        //drink water block
        if (heldItem.isEmpty()  && player.getPose() == Pose.CROUCHING) {
        	BlockPos pos=event.getHitVec().getBlockPos().offset(event.getFace().getNormal());
        	if(!event.getLevel().getFluidState(pos).isEmpty())
        		FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage(pos));
        }
    }

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.Clone event) {
		Player player = event.getEntity();
		Player original = event.getOriginal();

		if (!(player instanceof ServerPlayer))
			return;
		original.reviveCaps();
		NutritionCapability.getCapability(player).ifPresent(nutrition -> {
			NutritionCapability.getCapability(original).ifPresent(n -> {
				nutrition.set(n.get());
			});
			nutrition.addAttributes(player);
		});

		original.invalidateCaps();
	}

	@SubscribeEvent
	public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player) {
			if (!(player instanceof FakePlayer)) {
				// Common capabilities
				event.addCapability(FHMain.rl("nutrition"), FHCapabilities.PLAYER_NUTRITION.provider());
				// Server only
				if (player instanceof ServerPlayer) {
					event.addCapability(FHMain.rl("wanted_food"), FHCapabilities.WANTED_FOOD.provider());
				}
			}
		}



	}

	@SubscribeEvent
	public static void finishUsingItems(LivingEntityUseItemEvent.Finish event) {
		DailyKitchen.tryGiveBenefits(event);
		if (event.getEntity() instanceof Player player) {
			NutritionCapability.getCapability(player).ifPresent(e -> e.eat(player, event.getItem()));
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		// InsightHandler.onPlayerTick(event);

		Player player = event.player;
		if (!(player instanceof ServerPlayer))
			return;
		if (player.isCreative() || player.isSpectator())
			return;

		long gameTime = event.player.tickCount;
		if (event.phase == TickEvent.Phase.END) {
			NutritionCapability.getCapability(player).ifPresent(nutrition -> {
				nutrition.consume(player);
			});
			if (gameTime % 200 == 0) {
				NutritionCapability.getCapability(player).ifPresent(nutrition -> {
					nutrition.punishment(player);
					nutrition.addAttributes(player);
				});
			}
		}

	}

	@SubscribeEvent
	public static void punishEatingRawMeat(LivingEntityUseItemEvent.Finish event) {
		if (event.getEntity() != null && !event.getEntity().level().isClientSide
				&& event.getEntity() instanceof ServerPlayer
				&& ForgeRegistries.ITEMS.getHolder(event.getItem().getItem()).get().is(FHTags.Items.RAW_FOOD.tag)) {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 1));
			player.displayClientMessage(Lang.translateKey("message.frostedheart.eaten_poisonous_food"), false);
		}
	}

}
