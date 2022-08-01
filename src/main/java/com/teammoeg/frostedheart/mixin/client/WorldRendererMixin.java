package com.teammoeg.frostedheart.mixin.client;

import com.teammoeg.frostedheart.climate.BlizzardRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Used to render blizzard
 * Set priority to 1 for injecting earlier than Primal Winter
 */
@OnlyIn(Dist.CLIENT)
@Mixin(value = WorldRenderer.class, priority = 1)
public abstract class WorldRendererMixin
{
    @Shadow @Final private Minecraft mc;
    @Shadow private ClientWorld world;
    @Shadow private int ticks;

    @SuppressWarnings({"deprecation"})
    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
    public void inject$renderWeather(LightTexture manager, float partialTicks, double x, double y, double z, CallbackInfo ci) {
        if (this.mc != null && this.mc.gameRenderer != null) {
            // ClimateData data = ClimateData.get(world);
            // blizzard when vanilla 'thundering' is true, to save us from doing sync
            if (world.isThundering()) {
                BlizzardRenderer.render(mc, this.world, manager, ticks, partialTicks, x, y, z);
                // Road-block injection to remove any Vanilla / Primal Winter weather rendering code
                ci.cancel();
            }
            /*
             * if not blizzard, use primal winter's rendering
             * @see primalwinter's WorldRendererMixin
             */
        }
    }

    /*
    // Render the particle when precipitation hit the ground
    // (e.g. the splash of rain drops).
    // Not required for blizzard.
    @Inject(method = "addRainParticles", at = @At("RETURN"))
    public void inject$tickRain(ActiveRenderInfo renderInfo, CallbackInfo ci) {

    }
    */
}
