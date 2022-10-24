/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.content.incubator;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;

import com.cannolicatfish.rankine.init.RankineItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.SteamNetworkHolder;
import com.teammoeg.thermopolium.api.ThermopoliumApi;
import com.teammoeg.thermopolium.items.StewItem;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
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
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IncubatorTileEntity extends IEBaseTileEntity implements ITickableTileEntity,
		FHBlockInterfaces.IActiveState, IIEInventory, IInteractionObjectIE, IEBlockInterfaces.IProcessTile {
	protected NonNullList<ItemStack> inventory;
	protected FluidTank[] fluid = new FluidTank[] { new FluidTank(6000, w -> w.getFluid() == Fluids.WATER),
			new FluidTank(6000) };
	int process, processMax,lprocess;
	int fuel, fuelMax;
	int water;
	boolean isFoodRecipe;
	float efficiency = 0;
	ResourceLocation last;
	ItemStack out = ItemStack.EMPTY;
	FluidStack outfluid = FluidStack.EMPTY;
	public static final ResourceLocation food=new ResourceLocation(FHMain.MODID,"food");
	public static final ResourceLocation pr=new ResourceLocation("kubejs","protein");
	public static Fluid getProtein() {
		Fluid f=ForgeRegistries.FLUIDS.getValue(pr);
		return f==Fluids.EMPTY?Fluids.WATER:f;
	}
	public IncubatorTileEntity() {
		super(FHTileTypes.INCUBATOR.get());
		this.inventory = NonNullList.withSize(4, ItemStack.EMPTY);
		LazyOptional.of(() -> new IEInventoryHandler(15, this));
	}

	public IncubatorTileEntity(TileEntityType<?> type) {
		super(type);
		this.inventory = NonNullList.withSize(4, ItemStack.EMPTY);
		LazyOptional.of(() -> new IEInventoryHandler(15, this));
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
		if(i==1)
			return IncubateRecipe.canBeCatalyst(itemStack)||itemStack.getItem()==Items.ROTTEN_FLESH;
		if(i==2)
			return IncubateRecipe.canBeInput(itemStack)||itemStack.isFood();
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

	protected boolean fetchFuel() {
		ItemStack is = inventory.get(0);
		if (!is.isEmpty() && is.getItem() == RankineItems.QUICKLIME.get()) {
			is.shrink(1);
			fuel = fuelMax = 16000;
			return true;
		}
		return false;
	}
	protected float getMaxEfficiency() {
		return 1f;
	}
	
	@Override
	public void tick() {
		if (!this.world.isRemote) {
			if (process > 0) {
				if(efficiency<=0.2&&!isFoodRecipe)
					efficiency=0.2f;
				if(efficiency<=0) {
					out=ItemStack.EMPTY;
					outfluid=FluidStack.EMPTY;
					process=processMax=0;
					efficiency=0;
					lprocess=0;
					this.markDirty();
					this.markContainingBlockForUpdate(null);
					return;
				}
				if (fuel <= 0) {
					fetchFuel();
				}
				if (fuel > 0) {
					boolean d=false;
					boolean e=false;
					if(efficiency<=1)
						d=Math.random()<efficiency;
					else {
						d=Math.random()<efficiency-1;
						e=true;
					}
					if ((process/20 != lprocess)&&(d||e)) {
						if (fluid[0].drain(water, FluidAction.SIMULATE).getAmount() == water) {
							efficiency+=0.005;
							efficiency=Math.min(efficiency, getMaxEfficiency());
							fluid[0].drain(water, FluidAction.EXECUTE);
							lprocess=process/20;
						}else {
							efficiency-=0.005;
							this.markDirty();
							this.markContainingBlockForUpdate(null);
							return;
						}
					}
					if(e)process--;
					if(d) 
						process--;
					fuel--;
				}else efficiency-=0.0005;
				this.markDirty();
				this.markContainingBlockForUpdate(null);
				return;
			} else if (!out.isEmpty() || !outfluid.isEmpty()) {
				if (ItemHandlerHelper.canItemStacksStack(out, inventory.get(3))) {
					ItemStack is = inventory.get(3);
					int gr = Math.min(out.getCount(), is.getMaxStackSize() - is.getCount());
					out.shrink(gr);
					is.grow(gr);

				} else if (inventory.get(3).isEmpty()) {
					inventory.set(3, out);
					out = ItemStack.EMPTY;
				}
				if (!outfluid.isEmpty())
					outfluid.shrink(fluid[1].fill(outfluid, FluidAction.EXECUTE));
			} else {
				IncubateRecipe ir = IncubateRecipe.findRecipe(inventory.get(2), inventory.get(1));
				if (ir != null) {
					ItemStack outslot = inventory.get(3);
					if (ir.output.isEmpty() || outslot.isEmpty()
							|| (ir.output.getCount() + outslot.getCount() < outslot.getMaxStackSize()
									&& ItemHandlerHelper.canItemStacksStack(ir.output, outslot))) {
						if (ir.output_fluid.isEmpty() || fluid[1].fill(ir.output_fluid,
								FluidAction.SIMULATE) == ir.output_fluid.getAmount()) {
							if (ir.input != null)
								inventory.get(2).shrink(ir.input.getCount());
							if (ir.consume_catalyst && ir.catalyst != null)
								inventory.get(1).shrink(ir.catalyst.getCount());
							if(isFoodRecipe||!ir.getId().equals(last)) {
								last=ir.getId();
								efficiency = 0.2f;
							}
							process = processMax = ir.time * 20;
							water = ir.water;
							out = ir.output.copy();
							outfluid = ir.output_fluid.copy();
							isFoodRecipe = false;
							lprocess=0;
							this.markDirty();
							this.markContainingBlockForUpdate(null);
							return;
						}
					}
				}else {
					ItemStack catalyst=inventory.get(1);
					
					ItemStack in=inventory.get(2);
					if(!in.isEmpty()&&in.isFood()) {
						if(!catalyst.isEmpty()&&catalyst.getItem()==Items.ROTTEN_FLESH&&(efficiency<=0.01||!isFoodRecipe)) {
							isFoodRecipe=true;
							last=food;
							catalyst.shrink(1);
							efficiency = 0.2f;
							this.markDirty();
							this.markContainingBlockForUpdate(null);
							return;
						}
						if(efficiency>0.01) {
							int value=in.getItem().getFood().getHealing();
							if(in.getItem() instanceof StewItem) {
								value=ThermopoliumApi.getInfo(in).healing;
							}
							out=in.getContainerItem();
							in.shrink(1);
							int nvalue=value*25;
							outfluid=new FluidStack(getProtein(),nvalue);
							lprocess=0;
							process = processMax = 20 * 20*value;
							water=1;
						}
					}
				}
				boolean changed=false;
				if(efficiency>0) {
					efficiency-=0.0005;
					changed=true;
				}
				if(efficiency<0.005) {
					last=null;
					efficiency=0;
					changed=true;
				}
				if(changed) {
					this.markDirty();
					this.markContainingBlockForUpdate(null);
				}
				
			}
		}
	}

	@Override
	public void readCustomNBT(CompoundNBT compound, boolean client) {
		process = compound.getInt("process");
		lprocess = compound.getInt("lprocess");
		processMax = compound.getInt("processMax");
		fuel = compound.getInt("fuel");
		fuelMax = compound.getInt("fuelMax");
		
		efficiency = compound.getFloat("efficiency");
		fluid[0].readFromNBT(compound.getCompound("fluid1"));
		fluid[1].readFromNBT(compound.getCompound("fluid2"));
		if (!client) {
			if(compound.contains("last"))
			last=new ResourceLocation(compound.getString("last"));
			water = compound.getInt("water");
			ItemStackHelper.loadAllItems(compound, this.inventory);
			out = ItemStack.read(compound.getCompound("out"));
			outfluid = FluidStack.loadFluidStackFromNBT(compound.getCompound("outfluid"));
		}
	}

	@Override
	public void writeCustomNBT(CompoundNBT compound, boolean client) {
		compound.putInt("process", process);
		compound.putInt("lprocess", lprocess);
		compound.putInt("processMax", processMax);
		compound.putInt("fuel", fuel);
		compound.putInt("fuelMax", fuelMax);
		compound.putFloat("efficiency", efficiency);
		compound.put("fluid1", fluid[0].writeToNBT(new CompoundNBT()));
		compound.put("fluid2", fluid[1].writeToNBT(new CompoundNBT()));
		if (!client) {
			if(last!=null)
			compound.putString("last", last.toString());
			compound.putInt("water", water);
			ItemStackHelper.saveAllItems(compound, this.inventory);
			compound.put("out", out.serializeNBT());
			compound.put("outfluid", outfluid.writeToNBT(new CompoundNBT()));
		}
	}

	@Override
	public int[] getCurrentProcessesMax() {
		return new int[] { processMax,100, fuelMax };
	}

	@Override
	public int[] getCurrentProcessesStep() {
		return new int[] { process,MathHelper.ceil(efficiency*100),fuel };
	}

	public LazyOptional<IFluidHandler> fluidHandler = registerConstantCap(new IFluidHandler() {

		@Override
		public int getTanks() {
			return 2;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return fluid[tank].getFluidInTank(tank);
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
			int f=fluid[0].fill(resource, action);
			if(f>0&&action==FluidAction.EXECUTE) {
				markContainingBlockForUpdate(null);
			}
			return f;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			FluidStack fs= fluid[1].drain(resource, action);
			if(!fs.isEmpty()&&action==FluidAction.EXECUTE) {
				markContainingBlockForUpdate(null);
			}
			return fs;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			FluidStack fs= fluid[1].drain(maxDrain, action);
			if(!fs.isEmpty()&&action==FluidAction.EXECUTE) {
				markContainingBlockForUpdate(null);
			}
			return fs;
		}

	});
	LazyOptional<IItemHandler> invHandlerUp = registerConstantCap(new IEInventoryHandler(2, this, 1, true, false));
	LazyOptional<IItemHandler> invHandlerSide = registerConstantCap(new IEInventoryHandler(1, this, 0, true, false));
	LazyOptional<IItemHandler> invHandlerDown = registerConstantCap(new IEInventoryHandler(1, this, 3, false, true));

	@Nonnull
	@Override
	public <C> LazyOptional<C> getCapability(@Nonnull Capability<C> capability, @Nullable Direction facing) {
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return fluidHandler.cast();
		if (facing != null) {
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
