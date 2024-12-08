package com.teammoeg.frostedheart.mixin.noremap;

import blusunrize.immersiveengineering.common.blocks.plant.HempBlock;
import com.teammoeg.caupona.blocks.plants.FruitsLeavesBlock;
import com.teammoeg.caupona.blocks.plants.SilphiumBlock;
import com.teammoeg.frostedheart.base.event.PerformBonemealEvent;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {
        SilphiumBlock.class, FruitsLeavesBlock.class
})
public class NonvanillaBonemealableBlockMixin_CheckTemp {
    /**
     * @reason Check if the block can be bonemealed
     * @author yuesha-yc
     */
    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true, remap = false)
    private void fh$checkTempForBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new PerformBonemealEvent(pLevel, pPos, pState, pRandom))) {
            ci.cancel();
        }
    }
}
