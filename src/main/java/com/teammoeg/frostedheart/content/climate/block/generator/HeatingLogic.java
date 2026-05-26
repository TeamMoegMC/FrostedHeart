/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.block.generator;

import com.teammoeg.chorda.dataholders.SpecialDataHolder;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import net.minecraft.core.BlockPos;

/**
 * Common base class for any generator like block that maintains a heat area
 */
public abstract class HeatingLogic<T extends HeatingLogic<T, ?>, R extends HeatingState> implements IServerTickableComponent<R>, IMultiblockLogic<R>, IClientTickableComponent<R> {

    public HeatingLogic() {
        super();
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
        final boolean activeBeforeTick = state.isActive();

        // Tick the fuel
        boolean isActive = tickFuel(ctx);

        // Tick the heat
        tickHeat(ctx, isActive);

        // Set the activity status
        if (activeBeforeTick != isActive) {
            state.setActive(isActive);
            this.onActiveStateChange(ctx, isActive);
        }

        // Update the heat area
        if (state.shouldUpdateAdjust()) {
            BlockPos pos =  CMultiblockHelper.getMultiblock(ctx).masterPosInMB();
            int masterYPosInMB = pos.getY();
//            FHMain.LOGGER.debug("masterPosInMB " + pos);
            if (state.getRadius() > 0 && state.getTempMod() > 0) {
//                ChunkHeatData.addPillarTempAdjust(ctx.getLevel().getRawLevel(), CMultiblockHelper.getAbsoluteMaster(ctx), state.getRadius(), state.getUpwardRange(),
//                        state.getDownwardRange(), state.getTempMod());
                ChunkHeatData.addSphereTempAdjust(ctx.getLevel().getRawLevel(), CMultiblockHelper.getAbsoluteMaster(ctx).below(masterYPosInMB), state.getRadius(), state.getTempMod());
            } else {
                ChunkHeatData.removeTempAdjust(ctx.getLevel().getRawLevel(), CMultiblockHelper.getAbsoluteMaster(ctx).below(masterYPosInMB));
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
    public void onActiveStateChange(IMultiblockContext<R> ctx,boolean active) {
    }
    /**
     * Core client tick method
     * @param ctx
     */
    @Override
    public final void tickClient(IMultiblockContext<R> ctx) {
        tickEffects(ctx, CMultiblockHelper.getAbsoluteMaster(ctx), ctx.getState().isActive());
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

    /**
     * 纯函数：模拟发电机的燃料消耗与热量输出。
     * 不依赖世界实例，仅操作数据。
     *
     * @param data       发电机数据
     * @param teamData   团队数据持有者（用于获取研究加成）
     * @return 本次模拟产生的热量修改信息
     */
    public static HeatSimulationResult simulateHeatOutput(GeneratorData data, SpecialDataHolder<?> teamData) {
        // 如果损坏或未开启，返回空结果
        if (data.isBroken || !data.isWorking) {
            return HeatSimulationResult.EMPTY;
        }

        // 1. 燃料消耗 (将 tickFuelProcess 的逻辑搬过来，但要调整为每秒调用一次)
        //    原方法每 tick 消耗，现在每 20 tick 调用一次，因此燃料消耗量乘以 20
        int fuelTicks = 20; // 代表本次模拟覆盖 20 个原版 tick
        boolean hadFuel = data.hasFuel(); // 记录开始前是否有燃料
        // 这里需要实现一个批量消耗的版本，或者循环调用 consumesFuel 多次
        // 为了简洁，我们可以修改 consumesFuel 使其支持批量消耗，或直接调用原逻辑多次。
        // 但注意原 consumesFuel 里会即时产出 currentItem，需要适配。

        // 简化处理：直接访问内部字段，执行核心消耗和功率计算。
        // 以下为伪代码，实际需替换为真实计算：
        int baseFuelCost = fuelTicks * 1; // 基础消耗，原版每 tick 1，这里乘20
        if (data.isOverdrive) baseFuelCost += fuelTicks * 1;
        // 功率计算 ...
        // 消耗 inventory 中的燃料，扣除 process 等
        // 如果燃料不足，data.isWorking 可能变为 false
        // 此处省略具体实现，应由你根据原版 tickFuelProcess 改写为不依赖世界的版本。

        // 2. 热量级别更新 (tickHeatedProcess 逻辑)
        //    注意原方法中有随机过程，需调整概率（原每 tick 概率，现在每20 tick 概率放大）
        int heatedOld = data.heated;
        int rangedOld = data.ranged;
        // 更新 heated 和 ranged，并计算 TLevel 和 RLevel
        // ...

        // 3. 返回本次产生的热量调整（半径、温度修正值）
        int radius = data.getRadius();
        int tempMod = data.getTempMod();
        return new HeatSimulationResult(radius, tempMod);
    }

    public static class HeatSimulationResult {
        public static final HeatSimulationResult EMPTY = new HeatSimulationResult(0, 0);
        public final int radius;
        public final int tempMod;
        public HeatSimulationResult(int radius, int tempMod) {
            this.radius = radius;
            this.tempMod = tempMod;
        }
    }

}
