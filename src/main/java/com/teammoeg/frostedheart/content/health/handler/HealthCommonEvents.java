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

package com.teammoeg.frostedheart.content.health.handler;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.content.health.dailykitchen.DailyKitchen;
import com.teammoeg.frostedheart.content.health.dailykitchen.FluidFoodHelper;
import com.teammoeg.frostedheart.content.health.dailykitchen.WantedFoodCapability;
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
	public static void onPlayerRespawn(PlayerEvent.Clone event) {
		Player player = event.getEntity();
		Player original = event.getOriginal();

		if (!(player instanceof ServerPlayer))
			return;
		original.reviveCaps();
		// 保留每日厨房记录（吃过的食物种类 + 今日想吃的菜），避免死亡后清零
		// Preserve daily kitchen records (eaten food kinds + today's wanted foods) across death
		FHCapabilities.WANTED_FOOD.getCapability(original).ifPresent(old -> {
			FHCapabilities.WANTED_FOOD.getCapability(player).ifPresent(n -> n.copyFrom(old));
		});
		NutritionCapability.getCapability(player).ifPresent(nutrition -> {
			NutritionCapability.getCapability(original).ifPresent(n -> {
				nutrition.set(n.get());
			});
			nutrition.addAttributes(player);
		});

		original.invalidateCaps();
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		// 登录时若当天已生成过想吃的菜，只重发消息（不重新生成）；
		// 当天未生成（如离线跨日）时跳过，由玩家 tick 的补生成逻辑生成并发送新消息。
		// On login, re-send today's wanted foods message if already generated today
		// (without re-generating); if not generated yet (e.g. offline over a day rollover),
		// skip and let the tick-based catch-up generation send a fresh message.
		WantedFoodCapability cap = FHCapabilities.WANTED_FOOD.getCapability(player).orElse(null);
		long today = player.level().getDayTime() / 24000L;
		if (cap != null && today == cap.getLastGeneratedDay()) {
			DailyKitchen.sendWantedFoodsMessage(player);
		}
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
		if (event.getEntity() instanceof ServerPlayer player) {
			NutritionCapability.getCapability(player).ifPresent(e -> e.eat(player, event.getItem()));
			// 记录吃过的食物种类（仅可食用），供每日厨房次日生成"想吃的菜"。
			// 流体容器（保温杯）经 FluidFoodHelper 解析为对应的汤碗后再记录。
			// 服务端专属：WANTED_FOOD 能力只附加在 ServerPlayer 上。
			// Record eaten food kinds (edible only) for the daily kitchen to generate
			// wanted foods next morning. Fluid containers (thermos) are resolved to their
			// soup bowl Item via FluidFoodHelper first. Server only: WANTED_FOOD is attached
			// to ServerPlayer.
			if (!player.level().isClientSide) {
				FHCapabilities.WANTED_FOOD.getCapability(player).ifPresent(w -> w.addEatenFood(FluidFoodHelper.resolveFoodItem(event.getItem())));
			}
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
				if (gameTime % 200 == 0) {
					nutrition.punishment(player);
					nutrition.addAttributes(player);
				}
			});

		}else if(event.phase==TickEvent.Phase.START) {
			NutritionCapability.getCapability(player).ifPresent(nutrition -> {
				nutrition.calculatedFoodLevel=player.getFoodData().getFoodLevel();
			});
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
