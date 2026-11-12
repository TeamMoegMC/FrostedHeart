package com.teammoeg.frostedresearch.validation;
@net.minecraftforge.fml.common.Mod("research_validation")
public final class M3ValidationMod {
    public M3ValidationMod() {
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                Boolean.getBoolean("frostedresearch.m4Validation") ? M4ClientValidation.class : M3ClientValidation.class));
    }
}
