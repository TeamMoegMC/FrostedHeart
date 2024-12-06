package com.teammoeg.frostedheart.content.robotics.logistics.event;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.robotics.logistics.RobotChunk;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RoboticsCommonEvents {
    @SubscribeEvent
    public static void attachToWorld(AttachCapabilitiesEvent<Level> event) {
        // only attach to dimension with skylight (i.e. overworld)
        if (!event.getObject().dimensionType().hasFixedTime()) {
            event.addCapability(new ResourceLocation(FHMain.MODID, "logistic_data"),FHCapabilities.ROBOTIC_LOGISTIC_CHUNK.provider(RobotChunk::new));
        }
    }
}
