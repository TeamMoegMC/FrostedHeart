package com.teammoeg.frostedheart.mixin.minecraft.town;

import com.teammoeg.frostedheart.content.town.block.blockscanner.TownSectionSnapshots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Successful state writes include commands, fluids and neighbour updates, not just player actions. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin_TownSectionCache {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void frostedheart$dirtyRoadSection(BlockPos pos,BlockState state,boolean moving,CallbackInfoReturnable<BlockState> callback){
        BlockState old=callback.getReturnValue();
        if(old!=null&&old!=state&&((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level)TownSectionSnapshots.dirty(level,pos);
    }
}
