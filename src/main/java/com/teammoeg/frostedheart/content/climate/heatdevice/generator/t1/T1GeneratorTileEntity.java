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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t1;

import java.util.function.Function;

import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorTileEntity;
import com.teammoeg.frostedheart.util.FHMultiblockHelper;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.AlloySmelterLogic;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.BlastFurnaceLogic;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public final class T1GeneratorTileEntity extends MasterGeneratorTileEntity<T1GeneratorTileEntity,T1GeneratorState> {
    public T1GeneratorTileEntity() {
        super();
    }
    public boolean findTileEntity(IMultiblockContext<T1GeneratorState> ctx) {
        Vec3i vec = FHMultiblockHelper.getSize(ctx.getLevel());
        int xLow = -1, xHigh = vec.getX(), yLow = 0, yHigh = vec.getY(), zLow = -1, zHigh = vec.getZ();
        int blastBlockCount = 0, alloySmelterCount = 0;
        BlockPos.MutableBlockPos blockpos=new BlockPos.MutableBlockPos();
        for (int x = xLow; x <= xHigh; ++x)
            for (int y = yLow; y < yHigh; ++y)
                for (int z = zLow; z <= zHigh; ++z) {
                	
                    // Enum a seamless NoUpandDown hollow cube
                    if ( ( (z>zLow && z<zHigh) && ((x==xLow) || (x==xHigh)) ) || ((z==zLow || z==zHigh) && (x>xLow && x<xHigh)) ) {
                    	blockpos.set(x, y, z);
                        IMultiblockBEHelper<?> te = FHMultiblockHelper.getBEHelper(ctx.getLevel().getRawLevel(), ctx.getLevel().toAbsolute(blockpos)).orElse(null);
                        IMultiblockState state=te.getContext().getState();
                        if (state instanceof BlastFurnaceLogic.State) {
                            if (++blastBlockCount == 9) {
                            	ctx.getState().lastSupportPos=te.getContext().getLevel().toAbsolute(te.getMultiblock().masterPosInMB());
                                return true;
                            }
                        }
                        if (te instanceof AlloySmelterLogic.State) {
                            if (++alloySmelterCount == 4) {
                            	ctx.getState().lastSupportPos = te.getContext().getLevel().toAbsolute(te.getMultiblock().masterPosInMB());
                                return true;
                            }
                        }
                    }
                }
        return false;
    }

	@Override
	public IETemplateMultiblock getNextLevelMultiblock() {
		return FHMultiblocks.Multiblock.GENERATOR_T2;
	}
	@Override
	public T1GeneratorState createInitialState(IInitialMultiblockContext<T1GeneratorState> capabilitySource) {
		return new T1GeneratorState();
	}
	@Override
	public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType) {
		return b->Shapes.block();
	}
	@Override
	public void tickHeat(IMultiblockContext<T1GeneratorState> ctx, boolean isWorking) {
		
	}
	@Override
	public void tickEffects(IMultiblockContext<T1GeneratorState> ctx, BlockPos pos, boolean isActive) {
		if (isActive) {
			Level level=ctx.getLevel().getRawLevel();
            RandomSource random = level.random;
            if (random.nextFloat() < 0.2F) {
                //for (int i = 0; i < random.nextInt(2) + 2; ++i) {
                ClientUtils.spawnSmokeParticles(level, pos.relative(Direction.UP, 1));
                ClientUtils.spawnSmokeParticles(level, pos);
                ClientUtils.spawnFireParticles(level, pos);
                //}
            }
        }
		super.tickEffects(ctx, pos, isActive);
	}
	
}
