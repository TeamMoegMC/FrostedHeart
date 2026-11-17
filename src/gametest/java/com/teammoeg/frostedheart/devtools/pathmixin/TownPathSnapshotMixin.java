package com.teammoeg.frostedheart.devtools.pathmixin;

import com.teammoeg.frostedheart.devtools.TownPathProbe;

import java.util.concurrent.*;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="com.teammoeg.frostedheart.content.town.block.blockscanner.TownConnectivity$Snapshot",remap=false)
public abstract class TownPathSnapshotMixin {
    @Redirect(method="load",at=@At(value="INVOKE",target="Ljava/util/concurrent/CompletableFuture;get(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;"))
    private Object await(CompletableFuture<Object> future,long timeout,TimeUnit unit,SectionPos section)
            throws InterruptedException,ExecutionException,TimeoutException {
        long start=TownPathProbe.beforeWait(future,section);
        try{return future.get(timeout,unit);}finally{TownPathProbe.afterWait(start);}
    }
}
