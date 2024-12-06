package com.teammoeg.frostedheart.content.town.event;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.TeamTownDataS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TownCommonEvents {
    @SubscribeEvent
    public static void attachToChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().isEmpty()) {
            Level world = event.getObject().getLevel();
            if (!world.isClientSide) {
                event.addCapability(new ResourceLocation(FHMain.MODID, "chunk_town_resource"), FHCapabilities.CHUNK_TOWN_RESOURCE.provider());
            }
        }
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {

            // Town data sync (currently, every tick for debug)
            FHNetwork.sendPlayer(player,new TeamTownDataS2CPacket(player));
        }
    }
}
