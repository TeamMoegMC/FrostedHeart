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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator.t2;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorData;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorLogic;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.content.steamenergy.capabilities.HeatProviderEndPoint;
import com.teammoeg.frostedheart.util.FHMultiblockHelper;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.Optional;
import java.util.function.Function;

public class T2GeneratorLogic extends GeneratorLogic<T2GeneratorLogic, T2GeneratorState> {
    private static final MultiblockFace REDSTONE_OFFSET = new MultiblockFace(1, 1, 2, RelativeBlockFace.BACK);
    // TODO: Check if FRONT is correct
    private static final MultiblockFace NETWORK_OFFSET = new MultiblockFace(1, 0, 0, RelativeBlockFace.FRONT);
    private static final CapabilityPosition NETWORK_CAP = CapabilityPosition.opposing(NETWORK_OFFSET);
    private static final MultiblockFace FLUID_INPUT_OFFSET = new MultiblockFace(1, 0, 2, RelativeBlockFace.BACK);
    private static final CapabilityPosition FLUID_INPUT_CAP = CapabilityPosition.opposing(FLUID_INPUT_OFFSET);
    LazyOptional<HeatProviderEndPoint> endPoint;
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
                Vec3 wind = new Vec3(0, 0, 0);
                ClientUtils.spawnInvertedConeSteam(level, blockpos, wind);
            }
        }
    }

    @Override
    public <C> LazyOptional<C> getCapability(IMultiblockContext<T2GeneratorState> ctx, CapabilityPosition position, Capability<C> capability) {
        if (capability == FHCapabilities.HEAT_EP.capability() && NETWORK_CAP.equals(position)) {
            LazyOptional<HeatProviderEndPoint> cep = getData(ctx).map(t -> t.epcap).orElseGet(LazyOptional::empty);
            if (endPoint != cep) {
                endPoint = cep;
            }
            return endPoint.cast();
        } else if (capability == ForgeCapabilities.FLUID_HANDLER && FLUID_INPUT_CAP.equals(position)) {
            LazyOptional<IFluidHandler> tankCap = ctx.getState().tankCap;
            return tankCap.cast();
        }
        return super.getCapability(ctx, position, capability);
    }

    @Override
    protected boolean tickFuel(IMultiblockContext<T2GeneratorState> ctx) {
        if (ctx.getState().manager == null) {
            ctx.getState().manager = new HeatEnergyNetwork(ctx.getLevel().getBlockEntity(BlockPos.ZERO), c -> {
                BlockPos pos = FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel());
                BlockPos networkPos = pos.offset(NETWORK_OFFSET.posInMultiblock());
                Direction dir = ctx.getLevel().getOrientation().front();
                c.accept(networkPos.relative(dir.getOpposite()), dir.getOpposite());

            });
        }
        if ((!getData(ctx).map(t -> t.ep.hasValidNetwork()).orElse(true) || ctx.getState().manager.data.size() <= 1) && !ctx.getState().manager.isUpdateRequested()) {
            ctx.getState().manager.requestSlowUpdate();
        }
        ctx.getState().manager.tick();
        boolean active = super.tickFuel(ctx);
        if (active)
            this.tickLiquid(ctx);
        tickControls(ctx);
        return active;
    }

    private void tickLiquid(IMultiblockContext<T2GeneratorState> ctx) {
        Optional<GeneratorData> data = getData(ctx);
        ctx.getState().liquidtick = data.map(t -> t.steamProcess).orElse(0);
        if (!this.getIsActive(ctx))
            return;
        float rt = ctx.getState().getTempLevel();
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
            }
        } else {
            data.ifPresent(t -> {
                t.steamLevel = 0;
                t.steamProcess = 0;
                t.power = 0;
            });
        }
        ctx.getState().noliquidtick = 40;
    }

    // TODO: Check if the position is correct
    private void tickControls(IMultiblockContext<T2GeneratorState> ctx) {
        // Get origin position of the MB
        BlockPos origin = FHMultiblockHelper.getAbsoluteMaster(ctx.getLevel());
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

    @Override
    public IETemplateMultiblock getNextLevelMultiblock() {
        return null;
    }


    @Override
    public T2GeneratorState createInitialState(IInitialMultiblockContext<T2GeneratorState> ctx) {
        return new T2GeneratorState();
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType) {
        return b -> Shapes.block();
    }


    // TODO: Still need?
//    @Override
//    protected void callBlockConsumerWithTypeCheck(Consumer<T2GeneratorLogic> consumer, BlockEntity te) {
//        if (te instanceof T2GeneratorLogic)
//            consumer.accept((T2GeneratorLogic) te);
//    }

//    @Override
//    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
//        return side == this.getFacing() && this.posInMultiblock.equals(fluidIn);
//    }

    // TODO: Disassemble is moved to MB class. What about invalidate?
//    @Override
//    public void disassemble() {
//        if (manager != null)
//            manager.invalidate();
//        super.disassemble();
//    }

    // TODO: Check if this is implemented correctly in new getCapability method
//    @Override
//    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
//        T2GeneratorLogic master = master();
//        if (master != null && side == this.getFacing() && this.posInMultiblock.equals(fluidIn))
//            return new FluidTank[]{master.tank};
//        return new FluidTank[0];
//    }

    // TODO: Is this still needed?
//    @Override
//    public AABB getRenderBoundingBox() {
//        return new AABB(worldPosition.getX() - 2, worldPosition.getY() - 2, worldPosition.getZ() - 2, worldPosition.getX() + 2, worldPosition.getY() + 6,
//                worldPosition.getZ() + 2);
//    }
}
