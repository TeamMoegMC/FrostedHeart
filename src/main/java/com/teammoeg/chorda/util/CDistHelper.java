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
/**
 * A bunch of method that is related to server but may be used on both dists
 * 
 * */
@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CDistHelper {
    private static MinecraftServer server;
	private CDistHelper() {
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
