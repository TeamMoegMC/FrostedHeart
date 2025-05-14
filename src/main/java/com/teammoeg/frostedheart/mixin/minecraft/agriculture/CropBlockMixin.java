package com.teammoeg.frostedheart.mixin.minecraft.agriculture;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin extends BushBlock implements BonemealableBlock {
    public CropBlockMixin(Properties pProperties) {
        super(pProperties);
    }
    /**
     * @author lcyzsdh
     * @reason Allow crops to be planted on fertilized farmland
     */
    @Inject(method = "mayPlaceOn", at = @At("HEAD"),cancellable = true)
    protected void mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        if(pState.is(FHBlocks.FERTILIZED_FARMLAND.get())){
            cir.setReturnValue(true);
        }
    }
}
