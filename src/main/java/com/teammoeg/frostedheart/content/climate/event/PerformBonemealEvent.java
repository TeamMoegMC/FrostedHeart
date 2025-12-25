/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.event;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Cancelable;

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
@Cancelable
public class PerformBonemealEvent extends BlockEvent{
    private final RandomSource random;
    public PerformBonemealEvent(LevelAccessor level, BlockPos pos, BlockState state, RandomSource random) {
        super(level, pos, state);
        this.random = random;
    }

    public RandomSource getRandom() {
        return random;
    }
}
