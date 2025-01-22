package com.teammoeg.chorda;

import com.teammoeg.chorda.recipe.RecipeReloadListener;
import com.teammoeg.chorda.scheduler.SchedulerQueue;

import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Chorda.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChordaCommonEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START) {
            Level world = event.level;
            if (!world.isClientSide && world instanceof ServerLevel) {
                ServerLevel serverWorld = (ServerLevel) world;

                // Scheduled checks (e.g. town structures)
                SchedulerQueue.tickAll(serverWorld);
            }
        }
    }
    @SubscribeEvent
    public static void addReloadListenersLowest(AddReloadListenerEvent event) {
        ReloadableServerResources dataPackRegistries = event.getServerResources();
        event.addListener(new RecipeReloadListener(dataPackRegistries));
    }
}
