package com.teammoeg.frostedheart.content.climate.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** World fluid with no bucket pickup. Machine collection is blocked at its world-drain paths. */
public class HotSpringBlock extends LiquidBlock {
    public HotSpringBlock(Supplier<? extends FlowingFluid> fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
