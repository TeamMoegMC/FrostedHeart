package com.teammoeg.frostedheart.mixin.electrodynamics;

import electrodynamics.prefab.tile.components.type.ComponentFluidHandler;
import net.minecraft.fluid.Fluid;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;

@Mixin(ComponentFluidHandler.class)
public interface ComponentFluidHandlerAccess {

    @Accessor("fluids")
    HashMap<Fluid, FluidTank> getFluids();
}
