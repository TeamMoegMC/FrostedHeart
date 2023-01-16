package com.teammoeg.frostedheart.mixin.engdecor;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.util.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import wile.engineersdecor.blocks.DecorBlock;

@Mixin(DecorBlock.DirectedWaterLoggable.class)
public abstract class EdDecorBlockMixin extends AbstractBlock {
    public EdDecorBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.with(DirectionalBlock.FACING, rot.rotate(state.get(DirectionalBlock.FACING)));
    }
}