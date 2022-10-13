package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;

import com.cannolicatfish.rankine.init.RankineItems;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.NetworkHolder;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IncubatorTileEntity extends IEBaseTileEntity implements INetworkConsumer, ITickableTileEntity,
		FHBlockInterfaces.IActiveState, IIEInventory, IInteractionObjectIE, IEBlockInterfaces.IProcessTile {
	protected NonNullList<ItemStack> inventory;
	protected FluidTank[] fluid = new FluidTank[] { new FluidTank(6000, w -> w.getFluid() == Fluids.WATER),
			new FluidTank(6000) };
	int process, processMax;
	int fuel, fuelMax;
	int water;

	float efficiency = 1;
	NetworkHolder network = new NetworkHolder();
	ItemStack out = ItemStack.EMPTY;
	FluidStack outfluid = FluidStack.EMPTY;

	public IncubatorTileEntity() {
		super(getTileType());
		this.inventory = NonNullList.withSize(4, ItemStack.EMPTY);
		LazyOptional.of(() -> new IEInventoryHandler(15, this));
	}

	protected static TileEntityType<IncubatorTileEntity> getTileType() {
		return FHTileTypes.INCUBATOR.get();
	}

	@Nullable
	@Override
	public NonNullList<ItemStack> getInventory() {
		return inventory;
	}

	@Override
	public boolean isStackValid(int i, ItemStack itemStack) {

		if (i == 0)
			return itemStack.getItem() == RankineItems.QUICKLIME.get();
		if (i == 3)
			return false;
		return true;
	}

	@Override
	public int getSlotLimit(int i) {
		return 64;
	}

	@Override
	public void doGraphicalUpdates() {

	}



	@Override
	public boolean canUseGui(PlayerEntity arg0) {
		return true;
	}

	@Override
	public IInteractionObjectIE getGuiMaster() {
		return this;
	}

	@Override
	public void tick() {
		if (!this.world.isRemote) {
			if (process > 0) {
				boolean changed = false;
				if (fuel <= 0) {
					ItemStack is = inventory.get(0);
					if (!is.isEmpty() && is.getItem() == RankineItems.QUICKLIME.get()) {
						is.shrink(1);
						fuel = fuelMax = 16000;
						changed = true;
					}
				}
				if (fuel > 0) {
					if(process%20==0) {
						if(fluid[0].drain(water, FluidAction.SIMULATE).getAmount() == water)
							fluid[0].drain(water, FluidAction.EXECUTE);
						else
							 return;
					}
					
					fuel--;
					process--;
					changed = true;
				}
				if (changed)
					this.markContainingBlockForUpdate(null);
				return;
			} else if (!out.isEmpty() || !outfluid.isEmpty()) {
				if (ItemHandlerHelper.canItemStacksStack(out, inventory.get(3))) {
					ItemStack is=inventory.get(3);
					int gr=Math.min(out.getCount(),is.getMaxStackSize()-is.getCount());
					out.shrink(gr);
					is.grow(gr);
					
				}else if(inventory.get(3).isEmpty()) {
					inventory.set(3,out);
					out = ItemStack.EMPTY;
				}
				if (!outfluid.isEmpty())
					outfluid.shrink(fluid[1].fill(outfluid, FluidAction.EXECUTE));
			} else {
				IncubateRecipe ir = IncubateRecipe.findRecipe(inventory.get(2), inventory.get(1));
				if (ir == null)
					return;
				if (!ir.output.isEmpty()&&!inventory.get(3).isEmpty() && !ItemHandlerHelper.canItemStacksStack(ir.output, out))
					return;
				if (!ir.output_fluid.isEmpty()
						&& fluid[1].fill(ir.output_fluid, FluidAction.SIMULATE) != ir.output_fluid.getAmount())
					return;
				if (ir.input != null)
					inventory.get(2).shrink(ir.input.getCount());
				if (ir.consume_catalyst && ir.catalyst != null)
					inventory.get(1).shrink(ir.catalyst.getCount());
				process = processMax = ir.time*20;
				water = ir.water;
				out = ir.output.copy();
				outfluid = ir.output_fluid.copy();
				this.markContainingBlockForUpdate(null);
			}
		}
	}

	@Override
	public boolean connect(Direction to, int dist) {
		TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
		if (te instanceof EnergyNetworkProvider) {
			network.connect(((EnergyNetworkProvider) te).getNetwork(), dist);
			return true;
		}
		return false;
	}

	@Override
	public boolean canConnectAt(Direction to) {
		return to == this.getBlockState().get(IncubatorBlock.HORIZONTAL_FACING);
	}

	@Override
	public NetworkHolder getHolder() {
		return network;
	}

	@Override
	public void readCustomNBT(CompoundNBT compound, boolean client) {
		process = compound.getInt("process");
		processMax = compound.getInt("processMax");
		fuel = compound.getInt("fuel");
		fuelMax = compound.getInt("fuelMax");

		efficiency = compound.getFloat("efficiency");
		fluid[0].readFromNBT(compound.getCompound("fluid1"));
		fluid[1].readFromNBT(compound.getCompound("fluid2"));
		if (!client) {
			
			water = compound.getInt("water");
			ItemStackHelper.loadAllItems(compound, this.inventory);
			out = ItemStack.read(compound.getCompound("out"));
			outfluid = FluidStack.loadFluidStackFromNBT(compound.getCompound("outfluid"));
		}
	}

	@Override
	public void writeCustomNBT(CompoundNBT compound, boolean client) {
		compound.putInt("process", process);
		compound.putInt("processMax", processMax);
		compound.putInt("fuel", fuel);
		compound.putInt("fuelMax", fuelMax);
		compound.putFloat("efficiency", efficiency);
		compound.put("fluid1", fluid[0].writeToNBT(new CompoundNBT()));
		compound.put("fluid2", fluid[1].writeToNBT(new CompoundNBT()));
		if (!client) {
			
			compound.putInt("water", water);
			ItemStackHelper.saveAllItems(compound, this.inventory);
			compound.put("out", out.serializeNBT());
			compound.put("outfluid", outfluid.writeToNBT(new CompoundNBT()));
		}
	}

	@Override
	public int[] getCurrentProcessesMax() {
		return new int[] { processMax, fuelMax };
	}

	@Override
	public int[] getCurrentProcessesStep() {
		return new int[] { process, fuel };
	}
	public LazyOptional<IFluidHandler> fluidHandler = registerConstantCap(new IFluidHandler() {

		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return fluid[tank].getFluidInTank(0);
		}

		@Override
		public int getTankCapacity(int tank) {
			return fluid[tank].getCapacity();
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return fluid[tank].isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return fluid[0].fill(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return fluid[1].drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return fluid[1].drain(maxDrain, action);
		}
		
	});
	LazyOptional<IItemHandler> invHandlerUp = registerConstantCap(new IEInventoryHandler(2, this, 1, true, false));
	LazyOptional<IItemHandler> invHandlerSide = registerConstantCap(new IEInventoryHandler(1, this, 0, true, false));
	LazyOptional<IItemHandler> invHandlerDown = registerConstantCap(new IEInventoryHandler(1, this, 3, false, true));

	@Nonnull
	@Override
	public <C> LazyOptional<C> getCapability(@Nonnull Capability<C> capability, @Nullable Direction facing) {
		if (facing != null) {
			if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
				return fluidHandler.cast();
			if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
				if (facing == Direction.UP)
					return invHandlerUp.cast();
				if (facing == Direction.DOWN)
					return invHandlerDown.cast();

				return invHandlerSide.cast();
			}
		}
		return super.getCapability(capability, facing);
	}

}
