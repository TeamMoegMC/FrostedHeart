/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.mixin.stoneage;

import java.util.function.Supplier;

import net.minecraftforge.fml.DistExecutor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stone Age 1.6.8 captures its proxy constructors in lambdas declared by the
 * referring class. Forge deliberately rejects that pattern only in a
 * development environment. The unsafe variant performs the same sided
 * selection without the development-only referent validator.
 */
    @Mixin(targets = "com.yanny.age.stone.ExampleMod", remap = false)
public abstract class ExampleModMixin {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/DistExecutor;safeRunForDist(Ljava/util/function/Supplier;Ljava/util/function/Supplier;)Ljava/lang/Object;"))
    private static Object fh$allowStoneAgeProxySelectionInDevelopment(
            Supplier clientTarget, Supplier serverTarget) {
        return DistExecutor.unsafeRunForDist(clientTarget, serverTarget);
    }
}
