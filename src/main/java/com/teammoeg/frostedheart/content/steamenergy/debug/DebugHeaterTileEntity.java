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

package com.teammoeg.frostedheart.content.steamenergy.debug;

import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.steamenergy.*;
import com.teammoeg.frostedheart.content.steamenergy.creative.HeatManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DebugHeaterTileEntity extends HeatManagerBlockEntity {

    public DebugHeaterTileEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.DEBUGHEATER.get(), pos, state);
        endpoint = HeatEndpoint.provider(-1, Integer.MAX_VALUE);
    }

    @Override
    public void tick() {
        super.tick();
        endpoint.fill();
        endpoint.setTempLevel(this.getBlockState().getValue(BlockStateProperties.LEVEL_FLOWING));
    }

}
