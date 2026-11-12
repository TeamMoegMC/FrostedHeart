package com.teammoeg.frostedheart.mixin.charcoalpit;
import com.teammoeg.frostedresearch.knowledge.experiment.TuyereTrials;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="charcoalPit.block.BlockBellows",remap=false)
public class BlockBellowsMixin_ResearchTrial {
    @Inject(method="blow",at=@At("HEAD"),cancellable=true,remap=false)
    private void researchTrial(Level level,BlockPos pos,BlockState state,CallbackInfo ci) {
        if(level instanceof ServerLevel server && TuyereTrials.tryBlow(server,pos,state)) ci.cancel();
    }
}
