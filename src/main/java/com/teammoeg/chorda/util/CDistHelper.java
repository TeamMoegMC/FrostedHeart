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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * A bunch of method that is related to server but may be used on both dists
 * 
 * */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CDistHelper {
    private static MinecraftServer server;
	private CDistHelper() {
	}

	public static boolean isClient() {
		return FMLEnvironment.dist.isClient();
	}

	public static boolean isServer() {
		return FMLEnvironment.dist.isDedicatedServer();
	}
    
	/**
     * Get the server instance.
     * @return the server instance
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
     * Get the Recipe Manager instance
     * */
	public static RecipeManager getRecipeManager() {
	    if (getServer() != null)
	        return getServer().getRecipeManager();
	    return ClientUtils.getWorld().getRecipeManager();
	}
	/**
	 * Get the Registry Access instance, also the registry of registry
	 * Getting dynamic registry  with this is recommended
	 * */
	public static RegistryAccess getAccess() {
		MinecraftServer server= getServer();
		if(server!=null)
			return server.registryAccess();
		return ClientUtils.getWorld().registryAccess();
	}
	
}
