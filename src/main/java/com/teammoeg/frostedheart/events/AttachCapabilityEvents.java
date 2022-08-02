package com.teammoeg.frostedheart.events;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.ClimateData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkDataCapabilityProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttachCapabilityEvents {

    @SubscribeEvent
    public static void attachToWorld(AttachCapabilitiesEvent<World> event) {
        // only attach to dimension with skylight (i.e. overworld)
        if (event.getObject().getDimensionType().hasSkyLight()) {
            event.addCapability(ClimateData.ID, new ClimateData());
        }
    }

    @SubscribeEvent
    public static void attachToChunk(AttachCapabilitiesEvent<Chunk> event) {
        if (!event.getObject().isEmpty()) {
            World world = event.getObject().getWorld();
            ChunkPos chunkPos = event.getObject().getPos();
            if (!world.isRemote) {
                if (!event.getCapabilities().containsKey(ChunkDataCapabilityProvider.KEY))
                    event.addCapability(ChunkDataCapabilityProvider.KEY, new ChunkData(chunkPos));
            }
        }
    }

    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<PlayerEntity> event) {

    }

}
