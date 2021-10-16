package com.teammoeg.frostedheart.mixin.projecte;

import com.teammoeg.frostedheart.client.util.GuiUtils;
import moze_intel.projecte.gameObjs.blocks.TransmutationStone;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;

@Mixin(TransmutationStone.class)
public class MixinTransmutationStone {

    /**
     * @author yuesha-yc
     */
    @Inject(method = "onBlockActivated", at = @At(value = "HEAD"), remap = false)
    public void onBlockActivated(@Nonnull BlockState state, World world, @Nonnull BlockPos pos, @Nonnull PlayerEntity player, @Nonnull Hand hand,
                                 @Nonnull BlockRayTraceResult rtr, CallbackInfoReturnable<ActionResultType> cir) {
        player.sendStatusMessage(GuiUtils.translateMessage("too_cold_to_transmute"), true);
        cir.setReturnValue(ActionResultType.PASS);
    }
}
