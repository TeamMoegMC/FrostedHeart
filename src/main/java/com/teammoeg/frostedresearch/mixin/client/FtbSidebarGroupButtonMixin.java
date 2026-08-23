/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedresearch.mixin.client;

import com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB Library is optional; suppress only its unusual visibility-ignoring render override. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton", remap = false)
public abstract class FtbSidebarGroupButtonMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void fh$hideWhileResearchArchiveOpen(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback) {
        if (DrawDeskScreen.isResearchArchiveOpen()) {
            callback.cancel();
        }
    }
}
