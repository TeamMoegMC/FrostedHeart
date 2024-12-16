/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.radiator;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;

import com.teammoeg.frostedheart.content.climate.heatdevice.generator.HeatingLogic;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.compat.ie.FHMultiblockHelper;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.function.Function;

public class RadiatorLogic extends HeatingLogic<RadiatorLogic, RadiatorState> {

    public RadiatorLogic() {
        super();
    }

    @Override
    protected boolean tickFuel(IMultiblockContext<RadiatorState> ctx) {
        RadiatorState state = ctx.getState();
        boolean hasFuel;
        if (state.network.tryDrainHeat(4)) {
            state.setTempLevel(state.network.getTemperatureLevel());
            state.setRangeLevel(0.5f);
            state.setActive(true);
            hasFuel = true;
        } else {
            state.setActive(false);
            state.setTempLevel(0);
            state.setRangeLevel(0);
            hasFuel = false;
        }
        return hasFuel;
    }

    @Override
    public void tickHeat(IMultiblockContext<RadiatorState> ctx, boolean isActive) {

    }

    @Override
    protected void tickShutdown(IMultiblockContext<RadiatorState> ctx) {

    }

    @Override
    public <T> LazyOptional<T> getCapability(IMultiblockContext<RadiatorState> ctx, CapabilityPosition position, Capability<T> cap) {
        if (cap == FHCapabilities.HEAT_EP.capability() && position.posInMultiblock().getY() == 0) {
            return ctx.getState().heatcap.cast();
        }
        return super.getCapability(ctx, position, cap);
    }

    @Override
    public void tickEffects(IMultiblockContext<RadiatorState> ctx, BlockPos master, boolean isActive) {
        Level level = ctx.getLevel().getRawLevel();
        BlockPos pos = FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel());
        if (level != null && level.isClientSide && isActive && level.random.nextFloat() < 0.2) {
            ClientUtils.spawnSteamParticles(level, pos);
        }
    }

    @Override
    public RadiatorState createInitialState(IInitialMultiblockContext<RadiatorState> capabilitySource) {
        return new RadiatorState();
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType) {
        return b -> Shapes.block();
    }
}
