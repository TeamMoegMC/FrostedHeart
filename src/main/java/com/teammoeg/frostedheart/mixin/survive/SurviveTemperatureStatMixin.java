package com.teammoeg.frostedheart.mixin.survive;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.stereowalker.survive.util.TemperatureStats;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mixin(TemperatureStats.class)
public class SurviveTemperatureStatMixin {
	/**
	 * @author khjxiaogu
	 * @reason overwrite
	 */
	@Overwrite(remap = false)
	private boolean addTemperature(ServerPlayerEntity player, double temperature) {
		return true;
	}

	/**
	 * @author khjxiaogu
	 * @reason overwrite
	 */
	@Overwrite(remap = false)
	public void tick(ServerPlayerEntity player) {

	}

	/**
	 * @author khjxiaogu
	 * @reason overwrite
	 */
	@Overwrite(remap = false)
	@SubscribeEvent
	public static void tickTemperature(LivingUpdateEvent event) {

	}
}
