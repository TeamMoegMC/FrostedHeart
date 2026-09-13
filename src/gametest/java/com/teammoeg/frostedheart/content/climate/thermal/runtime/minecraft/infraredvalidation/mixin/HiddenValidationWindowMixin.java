/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.infraredvalidation.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only loaded by -PinfraredValidation, never by the distributed mod. */
@Mixin(value = Window.class, remap = false)
public abstract class HiddenValidationWindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", ordinal = 0, target =
            "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void fh$hiddenTestWindow(int hint, int value) {
        GLFW.glfwWindowHint(hint, value);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
    }
}
