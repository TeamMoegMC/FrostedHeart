package com.teammoeg.frostedheart.content.climate.event;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Event that is fired before any apply bonemeal logic is performed.
 * Canceling this event will prevent the bonemeal logic from being performed.
 *
 * This is a stronger logical server only implementation of BonemealEvent.
 * As that event is only fired when BonemealItem is used.
 * But we care about all possible usage, including Create's tree fertilizers.
 *
 * However, this event does not have access to player.
 */
public class PerformBonemealEvent extends BlockEvent {
    private final RandomSource random;
    public PerformBonemealEvent(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random) {
        super(level, pos, state);
        this.random = random;
    }

    public RandomSource getRandom() {
        return random;
    }
}
