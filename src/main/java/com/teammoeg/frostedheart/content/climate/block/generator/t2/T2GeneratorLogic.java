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

package com.teammoeg.frostedheart.content.climate.block.generator.t2;

import java.util.Optional;
import java.util.function.Function;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.content.climate.ClientClimateData;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.util.client.FHClientUtils;

import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class T2GeneratorLogic extends GeneratorLogic<T2GeneratorLogic, T2GeneratorState> {
    private static final MultiblockFace REDSTONE_OFFSET = new MultiblockFace(1, 1, 0, RelativeBlockFace.FRONT);
    // TODO: Check if FRONT is correct
    private static final CapabilityPosition NETWORK_CAP = new CapabilityPosition(1, 0, 2, RelativeBlockFace.BACK);
    private static final CapabilityPosition FLUID_INPUT_CAP = new CapabilityPosition(1, 0, 0, RelativeBlockFace.FRONT);
    
    public T2GeneratorLogic() {
        super();
    }

    @Override
    public void tickHeat(IMultiblockContext<T2GeneratorState> ctx, boolean isActive) {

    }

    @Override
    public void tickEffects(IMultiblockContext<T2GeneratorState> ctx, BlockPos pos, boolean isActive) {
        if (isActive) {
            Level level = ctx.getLevel().getRawLevel();
            BlockPos blockpos = pos.relative(Direction.UP, 5);
            RandomSource random = level.random;
            boolean isOverdrive = ctx.getState().getData(pos).map(t -> t.isOverdrive).orElse(false);
            if (random.nextFloat() < (isOverdrive ? 0.8F : 0.5F)) {
                ClientUtils.spawnT2FireParticles(level, blockpos);
                float windSpeed = Mth.clampedMap(ClientClimateData.getWind(), 0F, 100F, 0F, 100F);
                Vec3 windVec = new Vec3(windSpeed, 0, windSpeed);
                FHClientUtils.spawnInvertedConeSteam(level, blockpos, windVec);
            }
        }
    }

    @Override
    public <C> LazyOptional<C> getCapability(IMultiblockContext<T2GeneratorState> ctx, CapabilityPosition position, Capability<C> capability) {
    	//System.out.println(position);
    	//System.out.println(capability);
        if (FHCapabilities.HEAT_EP.isCapability(capability) && NETWORK_CAP.equalsOrNullFace(position)) {
           return ctx.getState().heatCap.cast(ctx);
        } else if (capability == ForgeCapabilities.FLUID_HANDLER && FLUID_INPUT_CAP.equals(position)) {
            LazyOptional<IFluidHandler> tankCap = ctx.getState().tankCap;
            return tankCap.cast();
        }
        return super.getCapability(ctx, position, capability);
    }

    @Override
    protected boolean tickFuel(IMultiblockContext<T2GeneratorState> ctx) {
        if (!ctx.getState().manager.hasBounded()) {
            ctx.getState().manager.setReconnector(() -> {
                BlockPos networkPos = ctx.getLevel().toAbsolute(NETWORK_CAP.posInMultiblock());
                Direction dir = NETWORK_CAP.side().forFront(ctx.getLevel().getOrientation());
               // System.out.println(networkPos.relative(dir)+"-"+dir);
                ctx.getState().manager.connectTo(ctx.getLevel().getRawLevel(),networkPos.relative(dir),networkPos, dir.getOpposite());
            });
        }
       
 
    	if(!ctx.getState().ep.hasValidNetwork()) {
            BlockPos pos = ctx.getLevel().toAbsolute(NETWORK_CAP.posInMultiblock());
            Level level = ctx.getLevel().getRawLevel();
            ctx.getState().manager.addEndpoint(ctx.getState().heatCap.cast(ctx), 0, level, pos);
        }
        
        if (ctx.getState().manager.getNetworkSize()<=2) {
            ctx.getState().manager.requestSlowUpdate();
        }
        ctx.getState().manager.tick(ctx.getLevel().getRawLevel());
        boolean active = super.tickFuel(ctx);
        if (active)
            this.tickLiquid(ctx);
        tickControls(ctx);
        return active;
    }

    private void tickLiquid(IMultiblockContext<T2GeneratorState> ctx) {
        Optional<GeneratorData> data = getData(ctx);
        ctx.getState().liquidtick = data.map(t -> t.steamProcess).orElse(0);
        float rt = data.map(t->t.TLevel).orElse(0f);
        /*if (rt == 0) {
            this.spowerMod = 0;
            this.slevelMod = 0;
        }*/
        if (ctx.getState().noliquidtick > 0) {
            ctx.getState().noliquidtick--;
            return;
        }

        int liquidtick = data.map(t -> t.steamProcess).orElse(0);
        if (liquidtick >= rt) {
            data.ifPresent(t -> t.steamProcess -= (int) rt);
            return;
        }
        GeneratorSteamRecipe sgr = GeneratorSteamRecipe.findRecipe(ctx.getState().tank.getFluid());
        if (sgr != null) {
            int rdrain = (int) (20 * ctx.getState().getTempLevel());
            int actualDrain = rdrain * sgr.input.getAmount();
            FluidStack fs = ctx.getState().tank.drain(actualDrain, FluidAction.SIMULATE);
            if (fs.getAmount() >= actualDrain) {
                data.ifPresent(t -> {
                    t.steamProcess = rdrain;
                    t.steamLevel = sgr.level;
                    t.power = sgr.power;
                });

                final FluidStack fs2 = ctx.getState().tank.drain(actualDrain, FluidAction.EXECUTE);
                data.ifPresent(t -> t.fluid = fs2.getFluid());
                return;
            }else {
            	ctx.getState().tank.setFluid(FluidStack.EMPTY);
            }
        }
    	data.ifPresent(t -> {
            t.steamLevel = 0;
            t.steamProcess = 0;
            t.power = 0;
        });
        
        ctx.getState().noliquidtick = 40;
    }

    // TODO: Check if the position is correct
    private void tickControls(IMultiblockContext<T2GeneratorState> ctx) {
        // Get origin position of the MB
        BlockPos origin = ctx.getLevel().getAbsoluteOrigin();
        // get the position of the redstone block from the relative position in MB
        BlockPos redstone = origin.offset(REDSTONE_OFFSET.posInMultiblock());

        int power = ctx.getLevel().getRawLevel().getDirectSignalTo(redstone);

        Optional<GeneratorData> data = getData(ctx);

        // Check if isOverdrive is true
        boolean isOverdrive = data.map(t -> t.isOverdrive).orElse(false);
        // isWorking
        boolean isWorking = data.map(t -> t.isWorking).orElse(false);

        if (power > 0) {
            if (power > 10) {
                if (!isOverdrive) {
                    data.ifPresent(t -> t.isOverdrive = true);
                }
                // Set isWorking to true if power is greater than 10
                if (!isWorking) {
                    data.ifPresent(t -> t.isWorking = true);
                }
            } else if (power > 5) {
                if (isOverdrive) {
                    data.ifPresent(t -> t.isOverdrive = false);
                }
                if (!isWorking) {
                    data.ifPresent(t -> t.isWorking = true);
                }
            } else {
                if (isWorking) {
                    data.ifPresent(t -> t.isWorking = false);
                }
            }
        }
    }
    public TemplateMultiblock getMultiblock() {
        return FHMultiblocks.GENERATOR_T2;
    }

    @Override
    public T2GeneratorState createInitialState(IInitialMultiblockContext<T2GeneratorState> ctx) {
        
    	return new T2GeneratorState();
        
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType) {
        return b -> Shapes.block();
    }


}
