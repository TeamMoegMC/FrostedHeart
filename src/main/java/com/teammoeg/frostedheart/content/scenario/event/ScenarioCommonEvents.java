package com.teammoeg.frostedheart.content.scenario.event;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.scenario.EventTriggerType;
import com.teammoeg.frostedheart.content.scenario.FHScenario;
import com.teammoeg.frostedheart.content.scenario.runner.ScenarioConductor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScenarioCommonEvents {
    @SubscribeEvent
    public static void attachToPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof ServerPlayer) {//server-side only capabilities
            ServerPlayer player = (ServerPlayer) event.getObject();
            if (!(player instanceof FakePlayer)) {
                event.addCapability(new ResourceLocation(FHMain.MODID, "scenario"       ), FHCapabilities.SCENARIO.provider());
            }
        }
    }

    @SubscribeEvent
    public static void tickPlayer(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            // Scenario runner
            ScenarioConductor runner= FHScenario.getNullable(player);
            if (runner != null)
                runner.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doPlayerInteract(PlayerInteractEvent ite) {
        if(ite.getEntity() instanceof ServerPlayer&&!(ite.getEntity() instanceof FakePlayer)) {
            FHScenario.trigVar(ite.getEntity(), EventTriggerType.PLAYER_INTERACT);
        }
    }
}
