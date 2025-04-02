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

package com.teammoeg.frostedresearch.handler;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;

import java.util.Map;

import com.teammoeg.chorda.events.TeamLoadedEvent;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.FRSpecialDataTypes;
import com.teammoeg.frostedresearch.ResearchListeners;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.compat.JEICompat;
import com.teammoeg.frostedresearch.events.DrawDeskOpenEvent;
import com.teammoeg.frostedresearch.network.FHResearchDataSyncPacket;
import com.teammoeg.frostedresearch.recipe.InspireRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;

@Mod.EventBusSubscriber(modid = FRMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResearchCommonEvents {
    @SubscribeEvent
    public static void onDrawDeskOpen(DrawDeskOpenEvent event) {
        if (!event.getOpenPlayer().isCreative() && PlayerTemperatureData.getCapability(event.getOpenPlayer()).map(PlayerTemperatureData::getCoreBodyTemp).orElse(0f) < -0.2) {
            event.getOpenPlayer().displayClientMessage(Lang.translateMessage("research.too_cold"), true);
            event.setCanceled(true);
        }
    }
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
	public static void syncDataToClient(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer) {
			ServerLevel serverWorld = ((ServerPlayer) event.getEntity()).serverLevel();
			PacketTarget currentPlayer = PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity());
			FHResearch.sendSyncPacket(currentPlayer);
			FRNetwork.INSTANCE.send(currentPlayer,
					new FHResearchDataSyncPacket(ResearchDataAPI.getData((ServerPlayer) event.getEntity()).get()));
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
    	ev.getTeamData().getOptional(FRSpecialDataTypes.RESEARCH_DATA).ifPresent(t->t.initResearch(ev.getTeamData()));
    }
    /**
     * Custom tooltip handling.
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        Item i = stack.getItem();
        if (!stack.isEmpty()) {
            Map<String, Component> rstooltip= JEICompat.research.get(i);
            if(rstooltip!=null)
                event.getToolTip().addAll(rstooltip.values());

        }
        // Inspiration item
        for (InspireRecipe ir : CUtils.filterRecipes(null, InspireRecipe.TYPE)) {
            if (ir.item.test(stack)) {
                event.getToolTip().add(Lang.translateTooltip("inspire_item").withStyle(ChatFormatting.GRAY));
                break;
            }
        }
    }

	@SubscribeEvent
	public static void addReloadListenersLowest(AddReloadListenerEvent event) {
		event.addListener(new ServerReloadListener());
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
