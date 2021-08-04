package com.teammoeg.frostedheart.mixin.survive;

import com.stereowalker.survive.config.Config;
import com.stereowalker.survive.events.SurviveEvents;
import com.stereowalker.survive.util.TemperatureStats;
import com.teammoeg.frostedheart.climate.SurviveTemperature;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(SurviveEvents.class)
public class SurviveEventsMixin {
    /**
     * @author yuesha-yc
     * Add our chunk temperature logic
     */
    @Overwrite
    @SubscribeEvent
    public static void updateTemperature(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() != null && !event.getEntityLiving().world.isRemote && event.getEntityLiving() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            for (SurviveTemperature.TempType type : SurviveTemperature.TempType.values()) {
                double temperature;
                if (type.isUsingExact()) {
                    temperature = SurviveTemperature.getExactTemperature(player.world, player.getPosition(), type);
                } else {
                    temperature = SurviveTemperature.getAverageTemperature(player.world, player.getPosition(), type, 5, Config.tempMode);
                }
                double modifier = (temperature) / type.getReductionAmount();
                int modInt = (int) (modifier * 1000);
                modifier = modInt / 1000.0D;
                if (player.ticksExisted % type.getTickInterval() == type.getTickInterval() - 1) {
                    TemperatureStats.setTemperatureModifier(player, "survive:" + type.getName(), modifier);
                }
            }
        }
    }

}
