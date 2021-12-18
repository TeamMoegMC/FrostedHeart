package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.foundation.config.ui.OpenCreateMenuButton;

import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OpenCreateMenuButton.OpenConfigButtonHandler.class)
public class MixinOpenConfigButtonHandler {
    /**
     * @author yuesha-yc khjxiaogu
     * @reason remove from main menu
     */
    @Inject(at= @At("HEAD"),method="onGuiInit",remap = false,cancellable=true)
    private static void fh$disableMainMenuButton(GuiScreenEvent.InitGuiEvent event,CallbackInfo cbi) {
        if(event.getGui() instanceof MainMenuScreen)cbi.cancel();
        	
    }
}
