package com.teammoeg.frostedheart.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 专门用于破坏雪的工具，没有耐久度（不会损坏）
 */
public class SnowBreakerItem extends FHBaseItem {
    /**
     * 破坏雪时是否正常掉落雪球
     */
    public final boolean dropItem;

    public SnowBreakerItem(Boolean dropItem , Properties builder) {
        super(builder);
        this.dropItem = dropItem;
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack pStack, BlockState pState) {
        return pState.is(BlockTags.SNOW) ? 1024.0F : 1.0F;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return this.dropItem && state.is(BlockTags.SNOW);
    }

}
