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

    /**
     * Helper method to apply a function to all block positions in the multiblock.
     * @param ctx
     * @param consumer
     */
    private void forEachBlock(IMultiblockContext<R> ctx, Consumer<BlockPos> consumer) {
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

    /**
     * Helper method to set all blocks active or inactive
     * @param ctx
     * @param state
     */
    public void setAllActive(IMultiblockContext<R> ctx, boolean state) {
        forEachBlock(ctx, s -> setActive(ctx, s, state));
    }

    /**
     * Core server tick method.
     *
     * It ticks the fuel, tick the heat, update heat adjust, then tick the shutdown.
     * Normally, you should not override this method.
     */
    @Override
    public final void tickServer(IMultiblockContext<R> ctx) {
        HeatingState state = ctx.getState();

        // Check the previous activity status
        final boolean activeBeforeTick = getIsActive(ctx);

        // Tick the fuel
        boolean isActive = tickFuel(ctx);

        // Tick the heat
        tickHeat(ctx, isActive);

        // Set the activity status
        if (activeBeforeTick != isActive)
            setAllActive(ctx, isActive);

        // Update the heat area
        if (state.shouldUpdateAdjust()) {
            if (state.getRadius() > 0 && state.getTempMod() > 0) {
                ChunkHeatData.addPillarTempAdjust(ctx.getLevel().getRawLevel(), FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()), state.getRadius(), state.getUpwardRange(),
                        state.getDownwardRange(), state.getTempMod());
            } else {
                ChunkHeatData.removeTempAdjust(ctx.getLevel().getRawLevel(), FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()));
            }
        }
        // If active, mark for update
        else if (isActive) {
            state.shouldUpdateAdjust();
        }

        // Mark the master dirty
        ctx.markMasterDirty();

        // Tick the shutdown
        tickShutdown(ctx);

    }

    /**
     * Core client tick method
     * @param ctx
     */
    @Override
    public final void tickClient(IMultiblockContext<R> ctx) {
        tickEffects(ctx, FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel()), getIsActive(ctx));
    }


    /**
     * Tick effects on client side. By default, does nothing.
     * @param ctx the multiblock context
     * @param master position of the master block
     * @param isActive is the generator active
     */
    public void tickEffects(IMultiblockContext<R> ctx, BlockPos master, boolean isActive) {

    }

    /**
     * Fired at the beginning of each tick.
     * Use this to consume fuel.
     * @param ctx
     * @return if the consuming was successful
     */
    protected abstract boolean tickFuel(IMultiblockContext<R> ctx);

    /**
     * Fired right after tickFuel but before updating the adjust.
     * Use this to do additional logic before the adjust is updated.
     * @param ctx
     * @param isActive is the result of tickFuel.
     */
    public abstract void tickHeat(IMultiblockContext<R> ctx, boolean isActive);

    /**
     * Fired at the end of each tick.
     * Use this to do additional logic after the adjust is updated.
    */
    protected abstract void tickShutdown(IMultiblockContext<R> ctx);

}
