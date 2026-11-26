package com.teammoeg.frostedheart.mixin.minecraft.agriculture;

import com.teammoeg.frostedheart.content.agriculture.tree.CultivatedSaplingPlacement;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes client placement prediction match the server cultivation state immediately. */
@Mixin(BlockItem.class)
public class BlockItemMixin_CultivatedSapling {
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void fh$cultivatedPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState original = cir.getReturnValue();
        if (original == null) return;
        var target = CultivatedSaplingPlacement.cultivatedFor(original.getBlock());
        if (target == null) return;
        BlockState planted = target.defaultBlockState();
        if (original.hasProperty(BlockStateProperties.WATERLOGGED))
            planted = planted.setValue(BlockStateProperties.WATERLOGGED,
                    original.getValue(BlockStateProperties.WATERLOGGED));
        cir.setReturnValue(planted);
    }
}
