package com.teammoeg.frostedheart.mixin.minecraft.agriculture;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.teammoeg.frostedheart.content.agriculture.CropFertilization;

/** Shares growth-earned yield with vanilla and mechanical harvesting. */
@Mixin(Block.class)
public class BlockFertilizerLootMixin {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void fh$environmentalYield(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity be,
            CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> result = CropFertilization.harvest(level, pos, state, cir.getReturnValue());
        if (result != cir.getReturnValue()) cir.setReturnValue(result);
    }
}
