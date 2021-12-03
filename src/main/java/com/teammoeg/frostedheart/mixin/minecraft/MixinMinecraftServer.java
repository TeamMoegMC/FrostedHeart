package com.teammoeg.frostedheart.mixin.minecraft;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {
	@Shadow
	private Thread serverThread;
	@Shadow
	public abstract CrashReport addServerInfoToCrashReport(CrashReport report);
	@Shadow
	public abstract File getDataDirectory();
	@Inject(at=@At(value="INVOKE",ordinal=0,target="Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",remap=false),method="func_240802_v_")
	public void FH$Diagnoise(CallbackInfo cbi) {
		CrashReport crashreport = CrashReport.makeCrashReport(new Exception(), "Exception ticking world");
		
		Minecraft.fillCrashReport(Minecraft.getInstance().getLanguageManager(),Minecraft.getInstance().getVersion(),Minecraft.getInstance().gameSettings,crashreport);
        File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "delay-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
		crashreport.saveToFile(file1);
		
	}
}
