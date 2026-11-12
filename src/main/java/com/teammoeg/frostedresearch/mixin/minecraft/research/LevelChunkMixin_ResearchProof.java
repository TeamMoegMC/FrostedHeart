package com.teammoeg.frostedresearch.mixin.minecraft.research;
import com.teammoeg.frostedresearch.knowledge.experiment.PendingResearchProofs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkMixin_ResearchProof {
    @Shadow @Final private Level level;
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void changed(BlockPos pos,BlockState next,boolean moving,CallbackInfoReturnable<BlockState> ci) {
        if(level instanceof ServerLevel server && ci.getReturnValue()!=null && ci.getReturnValue()!=next) PendingResearchProofs.blockChanged(server,pos);
    }
}
