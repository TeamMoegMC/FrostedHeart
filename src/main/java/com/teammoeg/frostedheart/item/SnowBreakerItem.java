package com.teammoeg.frostedheart.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 专门用于破坏雪的工具，有耐久度（会损坏）
 */
public class SnowBreakerItem extends FHBaseItem {
    /**
     * 破坏雪时是否正常掉落雪球
     */
    public final boolean dropItem;

    public SnowBreakerItem(Boolean dropItem, Properties builder) {
        super(builder);
        this.dropItem = dropItem;

    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack pStack, BlockState pState) {
        if (pState.is(BlockTags.SNOW)) {
            return 1024.0F;
        } else {
            return 1.0F;
        }
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level world, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity entityLiving) {
        if (state.is(BlockTags.SNOW)) {
            // 挖掘雪块时消耗耐久
            stack.hurtAndBreak(1, entityLiving, (player) -> player.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return this.dropItem && state.is(BlockTags.SNOW);
    }
}
