package com.teammoeg.frostedheart.mixin.forge;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.FHVersion;
import com.teammoeg.frostedheart.util.FileUtil;
import com.teammoeg.frostedheart.util.ZipFile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

@Mixin(ServerLifecycleHooks.class)
public class ServerLifecycleHooksMixin {
    @Shadow(remap = false)
    private static FolderName SERVERCONFIG;
    private static FolderName bkfconfig = new FolderName("serverconfigbackup");
    @Shadow(remap = false)
    private static Logger LOGGER;

    //automatically update serverconfig
    @Inject(at = @At("HEAD"), method = "handleServerAboutToStart", remap = false)
    private static void fh$updateConfig(MinecraftServer server, CallbackInfoReturnable cir) {
        Path config = server.func_240776_a_(SERVERCONFIG);
        Path configbkf = server.func_240776_a_(bkfconfig);
        FHMain.lastbkf = null;
        FHMain.saveNeedUpdate = false;
        File fconfig = config.toFile();
        File saveVersion = new File(fconfig, ".twrsaveversion");
        FHMain.lastServerConfig = config.toFile();
        FHVersion local = FHMain.local.fetchVersion().orElse(FHVersion.empty);
        String localVersion = local.getOriginal();
        if (saveVersion.exists() && !localVersion.isEmpty()) {
            try {
                String lw = FileUtil.readString(saveVersion);
                FHVersion save = FHVersion.parse(lw);
                if (!lw.isEmpty() && (lw.equals(localVersion) || save.laterThan(local)))
                    return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FHMain.saveNeedUpdate = true;
        try {
            configbkf.toFile().mkdirs();
            File backup = new File(configbkf.toFile(), "backup-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + ".zip");
            ZipFile zf = new ZipFile(backup, config);
            zf.addAndDel(config.toFile(), f -> !f.getName().startsWith(".") && f.getName().endsWith(".toml"));
            zf.close();
            fconfig.mkdirs();
            FileUtil.transfer(localVersion, saveVersion);
            FHMain.lastbkf = backup;
            FHMain.saveNeedUpdate = false;

            LOGGER.info("Save update succeed, old configuration has been backup to " + backup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
