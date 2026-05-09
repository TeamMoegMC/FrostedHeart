package com.teammoeg.chorda.mixin;

import com.teammoeg.chorda.events.client.WindowResizeEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftResizeDisplayMixin {

    @Inject(method = "resizeDisplay", at = @At("RETURN"))
    private void chorda$windowResizeEvent(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        int width = self.getWindow().getWidth();
        int height = self.getWindow().getHeight();
        MinecraftForge.EVENT_BUS.post(new WindowResizeEvent(self, width, height));
    }
}
