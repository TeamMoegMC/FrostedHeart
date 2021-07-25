package com.teammoeg.frostedheart.common.tile;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.common.container.ElectrolyzerContainer;
import com.teammoeg.frostedheart.common.recipe.ElectrolyzerRecipe;
import electrodynamics.DeferredRegisters;
import electrodynamics.api.electricity.CapabilityElectrodynamic;
import electrodynamics.common.item.ItemProcessorUpgrade;
import electrodynamics.common.recipe.categories.fluiditem2fluid.FluidItem2FluidRecipe;
import electrodynamics.common.settings.Constants;
import electrodynamics.prefab.tile.GenericTileTicking;
import electrodynamics.prefab.tile.components.ComponentType;
import electrodynamics.prefab.tile.components.type.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class ElectrolyzerTile extends GenericTileTicking {

    public static final int MAX_TANK_CAPACITY = 5000;
    public int clientTicks = 0;
    public static Fluid[] SUPPORTED_INPUT_FLUIDS = new Fluid[]{

            Fluids.WATER, DeferredRegisters.fluidEthanol

    };
    public static Fluid[] SUPPORTED_OUTPUT_FLUIDS = new Fluid[]{

            DeferredRegisters.fluidSulfuricAcid, DeferredRegisters.fluidPolyethylene

    };

    public ElectrolyzerTile() {
        super(FHTileTypes.ELECTROLYZER.get());
        addComponent(new ComponentTickable());
        addComponent(new ComponentDirection());
        addComponent(new ComponentPacketHandler());
        addComponent(new ComponentElectrodynamic(this).relativeInput(Direction.NORTH).voltage(CapabilityElectrodynamic.DEFAULT_VOLTAGE * 2)
                .maxJoules(Constants.CHEMICALMIXER_USAGE_PER_TICK * 10));
        addComponent(new ComponentFluidHandler(this).relativeInput(Direction.EAST).relativeOutput(Direction.WEST)
                .addMultipleFluidTanks(SUPPORTED_INPUT_FLUIDS, MAX_TANK_CAPACITY, true)
                .addMultipleFluidTanks(SUPPORTED_OUTPUT_FLUIDS, MAX_TANK_CAPACITY, false));
        addComponent(new ComponentInventory(this).size(4).relativeSlotFaces(0, Direction.EAST, Direction.UP).relativeSlotFaces(1, Direction.DOWN)
                .valid((slot, stack) -> slot < 3 || stack.getItem() instanceof ItemProcessorUpgrade));
        addComponent(new FHComponentProcessor(this).upgradeSlots(1, 2, 3).canProcess(component -> canProcessChemMix((FHComponentProcessor) component))
                .process(component -> ((FHComponentProcessor) component).processElectrolyzerRecipe(component, ElectrolyzerRecipe.class))
                .usage(Constants.CHEMICALMIXER_USAGE_PER_TICK).type(ComponentProcessorType.ObjectToObject)
                .requiredTicks(Constants.CHEMICALMIXER_REQUIRED_TICKS));
        addComponent(new ComponentContainerProvider("electrolyzer")
                .createMenu((id, player) -> new ElectrolyzerContainer(id, player, getComponent(ComponentType.Inventory), getCoordsArray())));
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return super.getRenderBoundingBox().grow(1);
    }

    protected boolean canProcessChemMix(FHComponentProcessor processor) {

        ComponentDirection direction = getComponent(ComponentType.Direction);
        ComponentFluidHandler tank = getComponent(ComponentType.FluidHandler);
        BlockPos face = getPos().offset(direction.getDirection().rotateY().getOpposite());
        TileEntity faceTile = world.getTileEntity(face);
        if (faceTile != null) {
            LazyOptional<IFluidHandler> cap = faceTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                    direction.getDirection().rotateY().getOpposite().getOpposite());
            if (cap.isPresent()) {
                IFluidHandler handler = cap.resolve().get();
                for (Fluid fluid : SUPPORTED_OUTPUT_FLUIDS) {
                    if (tank.getTankFromFluid(fluid).getFluidAmount() > 0) {
                        tank.getStackFromFluid(fluid).shrink(handler.fill(tank.getStackFromFluid(fluid), IFluidHandler.FluidAction.EXECUTE));
                        break;
                    }
                }
            }
        }

        processor.consumeBucket(MAX_TANK_CAPACITY, SUPPORTED_INPUT_FLUIDS, 1).dispenseBucket(MAX_TANK_CAPACITY, 2);
        return processor.canProcessFluidItem2FluidRecipe(processor, FluidItem2FluidRecipe.class, ElectrolyzerRecipe.TYPE);
    }
}

