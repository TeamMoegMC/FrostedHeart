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

package com.teammoeg.frostedheart.content.town.citizen.client;

import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 居民系统的客户端事件钩子：批量渲染、准星交互、退出清理。
 * <p>
 * Client-side event hooks for the citizen system: batched rendering,
 * crosshair interaction, and cleanup on logout.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CitizenClientEvents {

	/** 交互选取距离（方块） / Interaction pick distance in blocks */
	private static final double PICK_DIST = 4.5;

	private CitizenClientEvents() {
	}

	/**
	 * 在实体渲染阶段之后批量绘制居民。
	 * <p>
	 * Draws citizens in one batch after the entity render stage.
	 *
	 * @param event 渲染事件 / the render event
	 */
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			CitizenRenderCoordinator.render(event);
	}

	/**
	 * 每客户端 tick 驱动假实体生命周期与位姿。
	 * <p>
	 * Drives fake-entity lifecycle and pose every client tick.
	 *
	 * @param event tick 事件 / the tick event
	 */
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null)
			return;
		CitizenRenderCoordinator.tick(mc);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onFlywheelRenderersReloaded(ReloadRenderersEvent event) {
		CitizenRenderCoordinator.onRenderersReloaded(event.getWorld());
	}

	/**
	 * 准星对准居民时拦截使用键，本地打开交互菜单（流浪难民同款对话 GUI）。
	 * 菜单数据全部来自本地缓存快照，打开本身不产生网络流量；
	 * 菜单按钮再发 C2S 动作包，由服务端权威校验。
	 * <p>
	 * Intercepts the use key while aiming at a citizen and opens the
	 * interaction menu locally (the same dialogue GUI the wandering refugee
	 * uses). Menu data comes from the local cache snapshot, so opening costs
	 * no traffic; menu buttons then send C2S action packets which the server
	 * validates authoritatively.
	 *
	 * @param event 按键事件 / the input event
	 */
	@SubscribeEvent
	public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isUseItem())
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return;
		int id = ClientCitizenCache.pick(mc.gameRenderer.getMainCamera(), PICK_DIST);
		if (id >= 0) {
			CitizenMenuClient.open(id);
			event.setCanceled(true);
		}
	}

	/**
	 * 退出世界时清空居民缓存。
	 * <p>
	 * Clears the citizen cache when leaving the world.
	 *
	 * @param event 登出事件 / the logout event
	 */
	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		CitizenRenderCoordinator.clearWorld();
	}
}
