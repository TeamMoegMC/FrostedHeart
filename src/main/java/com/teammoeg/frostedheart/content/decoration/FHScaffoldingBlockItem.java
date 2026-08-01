package com.teammoeg.frostedheart.content.decoration;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.foundation.mixin.accessor.UseOnContextAccessor;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.teammoeg.frostedheart.item.FHBlockItem;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class FHScaffoldingBlockItem extends FHBlockItem {

    public FHScaffoldingBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var result = helperPlace(stack, context, PLACEMENT_HELPER_ID);
        if (result != null) return result;

        return super.onItemUseFirst(stack, context);
    }

    @Nullable
    public static InteractionResult helperPlace(ItemStack stack, UseOnContext context, int helperId) {
        var player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var state = level.getBlockState(pos);
            IPlacementHelper helper = PlacementHelpers.get(helperId);
            if (helper.matchesItem(stack) && helper.matchesState(state)) {
                var hit = ((UseOnContextAccessor)context).create$getHitResult();
                var result = helper.getOffset(player, level, state, pos, hit)
                        .placeInWorld(level, (BlockItem) stack.getItem(), player, context.getHand(), hit);
                if (result == InteractionResult.PASS) {
                    return InteractionResult.FAIL;
                }
                return result;
            }
        }

        return null;
    }

    public static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new ScaffoldingPlacementHelper());

    public static class ScaffoldingPlacementHelper implements IPlacementHelper {

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem
                    && ((BlockItem) i.getItem()).getBlock() instanceof FHScaffoldingBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return FHScaffoldingBlock::isScaffolding;
        }

        private boolean canExtendToward(BlockState state, Direction side) {
            return getStatePredicate().test(state) && side.getAxis() == Direction.Axis.Y;
        }

        private int attachedBlocks(Level world, BlockPos pos, Direction direction) {
            BlockPos checkPos = pos.relative(direction);
            int count = 0;
            while (getStatePredicate().test(world.getBlockState(checkPos))) {
                count++;
                checkPos = checkPos.relative(direction);
            }
            return count;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
            List<Direction> directions;

            if (player != null && ray.getDirection() == Direction.UP) {
                Direction facing = player.getDirection();
                directions = List.of(facing);
            } else {
                directions = IPlacementHelper.orderedByDistance(pos, ray.getLocation(),
                        dir -> canExtendToward(state, dir));
            }

            int range = AllConfigs.server().equipment.placementAssistRange.get();
            if (player != null) {
                AttributeInstance reach = player.getAttribute(ForgeMod.BLOCK_REACH.get());
                if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier))
                    range += 4;
            }

            for (Direction dir : directions) {
                int poles = attachedBlocks(world, pos, dir);
                if (poles >= range)
                    continue;

                BlockPos newPos = pos.relative(dir, poles + 1);
                int newY = newPos.getY();
                if (newY <= world.getMinBuildHeight() || newY >= world.getMaxBuildHeight())
                    continue;
                BlockState newState = world.getBlockState(newPos);
                if (!newState.canBeReplaced())
                    continue;

                return PlacementOffset.success(newPos,
                        bState -> FHScaffoldingBlock.calcState(bState, newPos, world));
            }

            return PlacementOffset.fail();
        }
    }
}
