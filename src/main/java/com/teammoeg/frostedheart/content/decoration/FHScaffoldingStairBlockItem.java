package com.teammoeg.frostedheart.content.decoration;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.foundation.mixin.accessor.UseOnContextAccessor;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeMod;

import java.util.function.Predicate;

public class FHScaffoldingStairBlockItem extends BlockItem {

    public static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new PlacementHelper());

    public FHScaffoldingStairBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            var heldItem = context.getItemInHand();
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var state = level.getBlockState(pos);
            IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
            if (helper.matchesItem(heldItem) && helper.matchesState(state)) {
                var hit = ((UseOnContextAccessor)context).create$getHitResult();
                return helper.getOffset(player, level, state, pos, hit)
                        .placeInWorld(level, (BlockItem) heldItem.getItem(), player, context.getHand(), hit);
            }
        }

        return super.useOn(context);
    }

    public static class PlacementHelper implements IPlacementHelper {

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem
                    && ((BlockItem) i.getItem()).getBlock() instanceof FHScaffoldingStairsBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return FHScaffoldingBlock::isScaffolding;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
            if (player == null)
                return PlacementOffset.fail();

            Direction facing = player.getDirection();
            BlockPos newPos = pos.relative(facing).above();
            if (FHScaffoldingBlock.isScaffolding(world.getBlockState(newPos))) {
                int range = AllConfigs.server().equipment.placementAssistRange.get();
                AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
                if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier))
                    range += 4;
                if (newPos.getY() <= world.getMaxBuildHeight() && newPos.closerThan(player.getOnPos(), range)) {
                    return getOffset(player, world, state, newPos, ray);
                }
            }
            int newY = newPos.getY();
            if (newY <= world.getMinBuildHeight() || newY >= world.getMaxBuildHeight() || !world.getBlockState(newPos).canBeReplaced())
                return PlacementOffset.fail();

            return PlacementOffset.success(newPos,
                    bState -> bState.setValue(HorizontalDirectionalBlock.FACING, facing));
        }
    }
}
