package com.teammoeg.frostedresearch.validation;
@net.minecraftforge.fml.common.Mod("research_validation")
public final class M3ValidationMod {
    public M3ValidationMod() {
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                !System.getProperty("frostedresearch.m7Role", "").isBlank() ? M7ClientValidation.class :
                Boolean.getBoolean("frostedresearch.m6Validation") ? M6ClientValidation.class :
                Boolean.getBoolean("frostedresearch.m5Validation") ? M5ClientValidation.class :
                Boolean.getBoolean("frostedresearch.m4Validation") ? M4ClientValidation.class : M3ClientValidation.class));
    }
}
