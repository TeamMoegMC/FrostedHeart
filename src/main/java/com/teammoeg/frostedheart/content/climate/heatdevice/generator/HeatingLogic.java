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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.function.Consumer;

import com.teammoeg.frostedheart.base.block.FHBlockInterfaces.IActiveStateLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.FHMultiblockHelper;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

/**
 * Common base class for any generator like block that maintains a heat area
 */
public abstract class HeatingLogic<T extends HeatingLogic<T, ?>, R extends HeatingState> implements IServerTickableComponent<R>, IMultiblockLogic<R>, IActiveStateLogic, IClientTickableComponent<R> {

    public HeatingLogic() {
        super();
    }

    public final void forEachBlock(IMultiblockContext<R> ctx, Consumer<BlockPos> consumer) {
        Vec3i vec = FHMultiblockHelper.getSize(ctx.getLevel());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < vec.getX(); ++x) {
            pos.setX(x);
            for (int y = 0; y < vec.getY(); ++y) {
                pos.setY(y);
                for (int z = 0; z < vec.getZ(); ++z) {
                    pos.setZ(z);
                    consumer.accept(pos);
                }
            }
        }
    }

    protected abstract void shutdownTick(IMultiblockContext<R> ctx);

    protected void setAllActive(IMultiblockContext<R> ctx, boolean state) {
        forEachBlock(ctx, s -> setActive(ctx, s, state));
    }

    @Override
    public void tickServer(IMultiblockContext<R> ctx) {
        HeatingState state = ctx.getState();
        final boolean activeBeforeTick = getIsActive(ctx);
        boolean isActive = tickFuel(ctx);
        tickHeat(ctx, isActive);
        if (activeBeforeTick != isActive)
            setAllActive(ctx, isActive);
        // set activity status
        final boolean activeAfterTick = isActive;
        if (state.shouldUpdate()) {
            if (state.getRadius() > 0 && state.getTempMod() > 0) {
                ChunkHeatData.addPillarTempAdjust(ctx.getLevel().getRawLevel(), FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()), state.getRadius(), state.getUpwardRange(),
                        state.getDownwardRange(), state.getTempMod());
            } else {
                ChunkHeatData.removeTempAdjust(ctx.getLevel().getRawLevel(), FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()));
            }
        } else if (activeAfterTick) {
            state.shouldUpdate();
        }
        ctx.markMasterDirty();
        shutdownTick(ctx);

    }

    @Override
    public void tickClient(IMultiblockContext<R> context) {
        tickEffects(context, FHMultiblockHelper.getAbsoluteMaster(context.getLevel()), getIsActive(context));
    }

    public void tickEffects(IMultiblockContext<R> ctx, BlockPos master, boolean isActive) {

    }

    protected abstract boolean tickFuel(IMultiblockContext<R> ctx);

    public abstract void tickHeat(IMultiblockContext<R> ctx, boolean isWorking);
}
