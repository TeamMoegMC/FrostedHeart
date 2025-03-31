package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MagmaBlock.class)
public class MagmaBlockMixin {

    @Unique
    private static final int COOLING_CHECK_INTERVAL = 20; //24000; // Ticks between cooling checks
    @Unique
    private static final float COOLING_CHANCE = 1; //1F / 2F; // On average it can stay 2 days

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // Schedule another tick for cooling check
        level.scheduleTick(pos, state.getBlock(), COOLING_CHECK_INTERVAL);

        // Check for cooling condition
        if (!frostedHeart$isTouchingLava(level, pos)) {
            // Check temperature condition (you may want additional conditions)
            if (random.nextFloat() < COOLING_CHANCE) {
                // Transform to cooled magma block
                BlockState cooledState = FHBlocks.COOLED_MAGMA_BLOCK.get().defaultBlockState();
                level.setBlockAndUpdate(pos, cooledState);
            }
        }
    }

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void onPlace(BlockState state, Level level, BlockPos pos,
                         BlockState oldState, boolean isMoving, CallbackInfo ci) {
        BubbleColumnBlock.updateColumn(level, pos.above(), state);
        level.scheduleTick(pos, state.getBlock(), COOLING_CHECK_INTERVAL);
        ci.cancel();
    }

    /**
     * Helper method to check if a block is touching lava on any side
     */
    @Unique
    private boolean frostedHeart$isTouchingLava(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (neighborState.is(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
