package com.teammoeg.frostedresearch.validation;
@net.minecraftforge.fml.common.Mod("research_validation")
public final class M3ValidationMod {
    public M3ValidationMod() {
        // Optional instance-local opt-in avoids changing Prism's cached JVM arguments during another game session.
        var deskCheck = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("research-client-m4-desk-check.txt");
        if (java.nio.file.Files.isRegularFile(deskCheck)) {
            try {
                System.setProperty("frostedresearch.m4Root", java.nio.file.Files.readString(deskCheck).strip());
                System.setProperty("frostedresearch.m4Validation", "true");
                System.setProperty("frostedresearch.m4DeskOnly", "true");
            } catch (java.io.IOException error) {
                com.teammoeg.frostedresearch.FRMain.LOGGER.error("Could not read M4 desk validation opt-in", error);
            }
        }
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
            () -> () -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                !System.getProperty("frostedresearch.m7Role", "").isBlank() ? M7ClientValidation.class :
                Boolean.getBoolean("frostedresearch.m6Validation") ? M6ClientValidation.class :
                Boolean.getBoolean("frostedresearch.m5Validation") ? M5ClientValidation.class :
                Boolean.getBoolean("frostedresearch.m4Validation") ? M4ClientValidation.class : M3ClientValidation.class));
    }
}
