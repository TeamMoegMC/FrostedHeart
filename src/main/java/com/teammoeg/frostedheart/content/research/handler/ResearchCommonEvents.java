package com.teammoeg.frostedheart.content.research.handler;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;

import com.teammoeg.chorda.events.TeamLoadedEvent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchCommonEvents {
    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        //Common capabilities
        //event.addCapability(new ResourceLocation(FHMain.MODID, "rsenergy"), FHCapabilities.ENERGY.provider());
    }

    @SubscribeEvent
    public static void tickResearch(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START
                && event.player instanceof ServerPlayer) {
            ResearchListeners.tick((ServerPlayer) event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        Entity ent = event.getSource().getEntity();

        if (!(ent instanceof Player) || ent instanceof FakePlayer) return;
        if (ent.getCommandSenderWorld().isClientSide) return;
        ServerPlayer p = (ServerPlayer) ent;

        ResearchListeners.kill(p, event.getEntity());
    }

    @SubscribeEvent
    public static void onIEMultiBlockForm(MultiblockHandler.MultiblockFormEvent event) {
        if (event.getEntity() instanceof FakePlayer) {
            event.setCanceled(true);
            return;
        }
        if (ResearchListeners.multiblock.has(event.getMultiblock()))
            if (event.getEntity().getCommandSenderWorld().isClientSide) {
                if (!ClientResearchDataAPI.getData().get().building.has(event.getMultiblock())) {
                    event.setCanceled(true);
                }
            } else {
                if (!ResearchDataAPI.getData(event.getEntity()).get().building.has(event.getMultiblock())) {
                    //event.getEntity().sendStatusMessage(GuiUtils.translateMessage("research.multiblock.cannot_build"), true);
                    event.setCanceled(true);
                }

            }
    }

    @SubscribeEvent
    public static void canUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ResearchListeners.canUseBlock(event.getEntity(), event.getLevel().getBlockState(event.getHitVec().getBlockPos()).getBlock())) {
            event.setUseBlock(Event.Result.DENY);

            event.getEntity().displayClientMessage(Lang.translateMessage("research.cannot_use_block"), true);
        }

    }
    @SubscribeEvent
    public static void initData(TeamLoadedEvent ev) {
    	ev.getTeamData().getOptional(FHSpecialDataTypes.RESEARCH_DATA).ifPresent(t->t.initResearch(ev.getTeamData()));
    }
   /* @SubscribeEvent
    public static void checkSleep(SleepingTimeCheckEvent event) {
        if (event.getEntity().getSleepTimer() >= 100 && !event.getEntity().getCommandSenderWorld().isClientSide) {
            EnergyCore.applySleep((ServerPlayer) event.getEntity());
        }
    }*/

    /*@SubscribeEvent
    public static void tickEnergy(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.START
                && event.player instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.player;
            if (!player.isSpectator() && !player.isCreative() && player.tickCount % 20 == 0)
                EnergyCore.dT(player);
        }
    }*/

    /*@SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer && !(event.getEntity() instanceof FakePlayer)) {
            EnergyCore.getCapability(event.getEntity()).ifPresent(t -> {
                t.onrespawn();
                t.sendUpdate((ServerPlayer) event.getEntity());
            });
        }
    }*/
}
