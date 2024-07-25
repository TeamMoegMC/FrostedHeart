package com.teammoeg.frostedheart.content.climate.event;

import com.teammoeg.frostedheart.FHConfig;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHParticleTypes;
import com.teammoeg.frostedheart.FHSounds;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorScreen;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    /**
     * Simulate breath particles when the player is in a cold environment
     */
    @SubscribeEvent
    public static void addBreathParticles(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && FHConfig.CLIENT.enableBreathParticle.get() && event.phase == TickEvent.Phase.START
                && event.player instanceof ClientPlayerEntity) {
            ClientPlayerEntity player = (ClientPlayerEntity) event.player;
            if(ClientUtils.mc().currentScreen instanceof MasterGeneratorScreen&&player.ticksExisted%20==0) {
            	((MasterGeneratorScreen)ClientUtils.mc().currentScreen).fullInit();
            }
            if (!player.isSpectator() && !player.isCreative() && player.world != null) {
                if (player.ticksExisted % 60 <= 3) {
                	 PlayerTemperatureData ptd=PlayerTemperatureData.getCapability(player).orElse(null);
                    float envTemp = ptd.getEnvTemp();
                    if (envTemp < -10.0F) {
                        // get the player's facing vector and make the particle spawn in front of the player
                        double x = player.getPosX() + player.getLookVec().x * 0.3D;
                        double z = player.getPosZ() + player.getLookVec().z * 0.3D;
                        double y = player.getPosY() + 1.3D;
                        // the speed of the particle is based on the player's facing, so it looks like it's coming from their mouth
                        double xSpeed = player.getLookVec().x * 0.03D;
                        double ySpeed = player.getLookVec().y * 0.03D;
                        double zSpeed = player.getLookVec().z * 0.03D;
                        // apply the player's motion to the particle
                        xSpeed += player.getMotion().x;
                        ySpeed += player.getMotion().y;
                        zSpeed += player.getMotion().z;
                        player.world.addParticle(FHParticleTypes.BREATH.get(), x, y, z, xSpeed, ySpeed, zSpeed);
                    }
                }
            }
        }
    }
    static int forstedSoundCd;
    /**
     * Play ice cracking sound when player's body temperature transitions across integer threshold.
     */
    @SubscribeEvent
    public static void playFrostedSound(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START
                && event.player instanceof ClientPlayerEntity) {
            ClientPlayerEntity player = (ClientPlayerEntity) event.player;
            if(forstedSoundCd>0)
            	forstedSoundCd--;
            if (!player.isSpectator() && !player.isCreative() && player.world != null&&forstedSoundCd>0) {
            	
            	PlayerTemperatureData ptd=PlayerTemperatureData.getCapability(player).orElse(null);
                float prevTemp = ptd.smoothedBodyPrev;
                float currTemp = ptd.smoothedBody;
                // play sound if currTemp transitions across integer threshold
                if (currTemp <= 0.5F && MathHelper.floor(prevTemp - 0.5F) != MathHelper.floor(currTemp - 0.5F)) {
                    player.world.playSound(player, player.getPosition(), FHSounds.ICE_CRACKING.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
                    forstedSoundCd=20;
                }
            }
        }
    }
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
    	forstedSoundCd=0;
    }
    

}
