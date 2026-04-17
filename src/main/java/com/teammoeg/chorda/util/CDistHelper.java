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

package com.teammoeg.chorda.util;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.ClientUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * 与服务端相关但可能在客户端和服务端两侧使用的工具方法集合。
 * 提供分发端判断、服务器实例获取、配方管理器和注册表访问等功能。
 * <p>
 * A collection of utility methods related to the server but usable on both distributions.
 * Provides distribution side checks, server instance access, recipe manager and registry access.
 */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CDistHelper {
    private static MinecraftServer server;
	private CDistHelper() {
	}

	/**
	 * 判断当前运行环境是否为客户端。
	 * <p>
	 * Check if the current runtime environment is a client.
	 *
	 * @return 如果是客户端返回true / true if running on the client
	 */
	public static boolean isClient() {
		return FMLEnvironment.dist.isClient();
	}

	/**
	 * 判断当前运行环境是否为专用服务端。
	 * <p>
	 * Check if the current runtime environment is a dedicated server.
	 *
	 * @return 如果是专用服务端返回true / true if running on a dedicated server
	 */
	public static boolean isServer() {
		return FMLEnvironment.dist.isDedicatedServer();
	}
    
	/**
	 * 获取服务器实例。
	 * <p>
	 * Get the server instance.
	 *
	 * @return 服务器实例 / the server instance
	 */
	public static MinecraftServer getServer() {
		return server;
	}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    static void serverAboutToStart(final ServerAboutToStartEvent event) {
       server=event.getServer();
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    static void serverStopped(final ServerStoppedEvent event) {
        server=null;
    }
	/**
	 * 获取配方管理器实例，服务端优先，否则回退到客户端。
	 * <p>
	 * Get the recipe manager instance, server-side preferred, falling back to client-side.
	 *
	 * @return 配方管理器 / the recipe manager
	 */
	public static RecipeManager getRecipeManager() {
	    if (getServer() != null)
	        return getServer().getRecipeManager();
	    return ClientUtils.getWorld().getRecipeManager();
	}
	/**
	 * 获取注册表访问实例（注册表的注册表），推荐通过此方法获取动态注册表。
	 * <p>
	 * Get the RegistryAccess instance (the registry of registries).
	 * Getting dynamic registries through this method is recommended.
	 *
	 * @return 注册表访问实例 / the registry access instance
	 */
	public static RegistryAccess getAccess() {
		MinecraftServer server= getServer();
		if(server!=null)
			return server.registryAccess();
		return ClientUtils.getWorld().registryAccess();
	}

	/**
	 * 获取所有在线的管理员玩家列表。
	 * <p>
	 * Get a list of all online operator players.
	 *
	 * @return 在线管理员列表 / the list of online operators
	 */
	public static List<ServerPlayer> getOnlineOPs() {
		if (getServer() == null) return List.of();
		var ops = new ArrayList<ServerPlayer>();
		for (ServerPlayer p1 : getServer().getPlayerList().getPlayers()) {
			if (getServer().getPlayerList().isOp(p1.getGameProfile())) {
				ops.add(p1);
			}
		}
		return ops;
	}
	
}
