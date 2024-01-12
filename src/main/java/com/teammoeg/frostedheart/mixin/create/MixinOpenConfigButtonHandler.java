/*
 * Copyright (c) 2022-2024 TeamMoeg
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
    @Inject(at = @At("HEAD"), method = "onGuiInit", remap = false, cancellable = true)
    private static void fh$disableMainMenuButton(GuiScreenEvent.InitGuiEvent event, CallbackInfo cbi) {
        if (event.getGui() instanceof MainMenuScreen) cbi.cancel();

    }
}
