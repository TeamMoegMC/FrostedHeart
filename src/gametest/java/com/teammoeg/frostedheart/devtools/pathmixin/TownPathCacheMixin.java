package com.teammoeg.frostedheart.devtools.pathmixin;

import com.teammoeg.frostedheart.content.town.block.blockscanner.TownSectionSnapshots;
import com.teammoeg.frostedheart.devtools.TownPathProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The shared cache is serviced before individual towns; include its cost for the selected world. */
@Mixin(value=TownSectionSnapshots.class,remap=false)
public abstract class TownPathCacheMixin {
    @Inject(method="service",at=@At("HEAD"))
    private static void begin(ServerLevel level,CallbackInfo ci){TownPathProbe.cacheBegin(level);}
    @Inject(method="service",at=@At("RETURN"))
    private static void end(ServerLevel level,CallbackInfo ci){TownPathProbe.cacheEnd(level);}
    @Inject(method="copySection",at=@At("HEAD"))
    private static void copied(ServerLevel level,long key,CallbackInfoReturnable<byte[]> cir){TownPathProbe.cacheCopied(level);}
    @Inject(method="copySection",at=@At("RETURN"))
    private static void copyEnd(ServerLevel level,long key,CallbackInfoReturnable<byte[]> cir){TownPathProbe.cacheCopyEnd(level);}
}
