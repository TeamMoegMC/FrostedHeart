package com.teammoeg.frostedheart.devtools.pathmixin;

import com.teammoeg.frostedheart.devtools.TownPathProbe;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.teammoeg.frostedheart.content.town.block.blockscanner.*;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Timing only, enabled by the opt-in profile init script. */
@Mixin(value=TownConnectivity.class,remap=false)
public abstract class TownPathTickMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private void begin(ServerLevel level,BlockPos tower,Map<BlockPos,AbstractTownBuilding> buildings,CallbackInfo ci){TownPathProbe.beginTick((TownConnectivity)(Object)this,level);}
    @Inject(method="tick",at=@At("RETURN"))
    private void end(ServerLevel level,BlockPos tower,Map<BlockPos,AbstractTownBuilding> buildings,CallbackInfo ci){TownPathProbe.endTick((TownConnectivity)(Object)this,level);}
    @Inject(method="calculate(Lcom/teammoeg/frostedheart/content/town/block/blockscanner/TownConnectivity$Batch;)Lcom/teammoeg/frostedheart/content/town/block/blockscanner/TownConnectivity$Result;",at=@At("HEAD"))
    private static void workerBegin(@Coerce Object batch,CallbackInfoReturnable<TownConnectivity.Result> cir){TownPathProbe.beginWorker(batch);}
    @Inject(method="calculate(Lcom/teammoeg/frostedheart/content/town/block/blockscanner/TownConnectivity$Batch;)Lcom/teammoeg/frostedheart/content/town/block/blockscanner/TownConnectivity$Result;",at=@At("RETURN"))
    private static void workerEnd(@Coerce Object batch,CallbackInfoReturnable<TownConnectivity.Result> cir){TownPathProbe.endWorker();}
    @Redirect(method="calculateView(Ljava/util/Set;Lnet/minecraft/core/BlockPos;Ljava/util/function/Supplier;Ljava/util/function/BooleanSupplier;Ljava/util/Map;)Lcom/teammoeg/frostedheart/content/town/block/blockscanner/TownConnectivity$Result;",
            at=@At(value="INVOKE",target="Lcom/teammoeg/frostedheart/content/town/block/blockscanner/RoomPathfinder;getNeighbors(Lcom/teammoeg/frostedheart/content/town/block/blockscanner/AbstractWorld;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/util/List;"))
    private static List<BlockPos> expand(AbstractWorld world,BlockPos start,BlockPos foot){TownPathProbe.expanded();return RoomPathfinder.getNeighbors(world,start,foot);}
}
