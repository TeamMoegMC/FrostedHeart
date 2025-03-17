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

package com.teammoeg.frostedheart.content.incubator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.chorda.block.CBlockInterfaces;
import com.teammoeg.chorda.block.entity.CBlockEntity;
import com.teammoeg.chorda.block.entity.CTickableBlockEntity;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IProcessBE;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class IncubatorTileEntity extends CBlockEntity implements CTickableBlockEntity,
        CBlockInterfaces.IActiveState,  MenuProvider, IProcessBE,IIEInventory {
    public static final ResourceLocation food = new ResourceLocation(FHMain.MODID, "food");
    public static final ResourceLocation pr = new ResourceLocation("kubejs", "protein");
    protected NonNullList<ItemStack> inventory;
    protected FluidTank[] fluid = new FluidTank[]{new FluidTank(6000, w -> w.getFluid() == Fluids.WATER),
            new FluidTank(6000)};
    int process, processMax, lprocess;
    int fuel, fuelMax;
    int water;
    boolean isFoodRecipe;
    float efficiency = 0;
    ResourceLocation last;
    ItemStack out = ItemStack.EMPTY;
    FluidStack outfluid = FluidStack.EMPTY;

    IFluidHandler handler = new IFluidHandler() {

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack fs = fluid[1].drain(resource, action);
            if (!fs.isEmpty() && action == FluidAction.EXECUTE) {
                setChanged();
            }
            return fs;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack fs = fluid[1].drain(maxDrain, action);
            if (!fs.isEmpty() && action == FluidAction.EXECUTE) {
            	setChanged();
            }
            return fs;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int f = fluid[0].fill(resource, action);
            if (f > 0 && action == FluidAction.EXECUTE) {
            	setChanged();

            }
            return f;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {

            return fluid[tank].getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return 6000;
        }

        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0) return false;
            return fluid[tank].isFluidValid(tank, stack);
        }

    };

    LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> handler);

    LazyOptional<IItemHandler> invHandlerUp = LazyOptional.of(() -> new IEInventoryHandler(2, this, 1, true, false));
    
    LazyOptional<IItemHandler> invHandlerSide = LazyOptional.of(() -> new IEInventoryHandler(1, this, 0, true, false));

    LazyOptional<IItemHandler> invHandlerDown = LazyOptional.of(() -> new IEInventoryHandler(1, this, 3, false, true));

    public static Fluid getProtein() {
        Fluid f = CRegistryHelper.getFluid(pr);
        return f == Fluids.EMPTY ? Fluids.WATER : f;
    }

    public IncubatorTileEntity(BlockPos bp,BlockState bs) {
        super(FHBlockEntityTypes.INCUBATOR.get(),bp,bs);
        this.inventory = NonNullList.withSize(4, ItemStack.EMPTY);
    }

    public IncubatorTileEntity(BlockEntityType<? extends BlockEntity> type,BlockPos bp,BlockState bs) {
        super(type,bp,bs);
        this.inventory = NonNullList.withSize(4, ItemStack.EMPTY);
    }
    @Override
    public void doGraphicalUpdates() {

    }

    protected boolean fetchFuel() {
        ItemStack is = inventory.get(0);
        if (!is.isEmpty() && is.getItem() == FHItems.QUICKLIME.get()) {
            is.shrink(1);
            fuel = fuelMax = 16000;
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    public <C> LazyOptional<C> getCapability(@Nonnull Capability<C> capability, Direction facing) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {

            if (!fluidHandler.isPresent()) {
                LazyOptional<IFluidHandler> old = fluidHandler;
                fluidHandler = LazyOptional.of(() -> handler);
                old.invalidate();
            }
            return ForgeCapabilities.FLUID_HANDLER.orEmpty(capability, fluidHandler);
        }
        if (facing != null) {
            if (capability == ForgeCapabilities.ITEM_HANDLER) {
                if (facing == Direction.UP)
                    return invHandlerUp.cast();
                if (facing == Direction.DOWN)
                    return invHandlerDown.cast();

                return invHandlerSide.cast();
            }
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int[] getCurrentProcessesMax() {
        return new int[]{processMax, 100, fuelMax};
    }

    @Override
    public int[] getCurrentProcessesStep() {
        return new int[]{processMax - process, Mth.ceil(efficiency * 100), fuel};
    }


    @Nullable
    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    protected float getMaxEfficiency() {
        return 1f;
    }

    @Override
    public int getSlotLimit(int i) {
        return 64;
    }
    public boolean canBeCatalyst(ItemStack catalyst) {
        return CUtils.filterRecipes(this.getLevel().getRecipeManager(),IncubateRecipe.TYPE).stream().filter(r -> r.catalyst != null).anyMatch(r -> r.catalyst.testIgnoringSize(catalyst));
    }

    public boolean canBeInput(ItemStack input) {
        return CUtils.filterRecipes(this.getLevel().getRecipeManager(),IncubateRecipe.TYPE).stream().anyMatch(r -> r.input.testIgnoringSize(input));
    }

    public IncubateRecipe findRecipe(ItemStack in, ItemStack catalyst) {
        return CUtils.filterRecipes(this.getLevel().getRecipeManager(),IncubateRecipe.TYPE).stream().filter(t -> t.input.test(in)).filter(t -> t.catalyst == null || t.catalyst.test(catalyst)).findAny().orElse(null);
    }
    @Override
    public boolean isStackValid(int i, ItemStack itemStack) {

        if (i == 0)
            return itemStack.getItem() == FHItems.QUICKLIME.get();
        if (i == 1)
            return canBeCatalyst(itemStack) || itemStack.getItem() == Items.ROTTEN_FLESH;
        if (i == 2)
            return canBeInput(itemStack) || itemStack.isEdible();
        return i != 3;
    }

    @Override
    public void readCustomNBT(CompoundTag compound, boolean client) {
        process = compound.getInt("process");
        lprocess = compound.getInt("lprocess");
        processMax = compound.getInt("processMax");
        fuel = compound.getInt("fuel");
        fuelMax = compound.getInt("fuelMax");

        efficiency = compound.getFloat("efficiency");
        fluid[0].readFromNBT(compound.getCompound("fluid1"));
        fluid[1].readFromNBT(compound.getCompound("fluid2"));
        if (!client) {
            if (compound.contains("last"))
                last = new ResourceLocation(compound.getString("last"));
            water = compound.getInt("water");
            ContainerHelper.loadAllItems(compound, this.inventory);
            out = ItemStack.of(compound.getCompound("out"));
            outfluid = FluidStack.loadFluidStackFromNBT(compound.getCompound("outfluid"));
        }
    }
    @Override
    public void tick() {
        if (!this.level.isClientSide) {
            if (process > 0) {
                if (efficiency <= 0.005) {
                    out = ItemStack.EMPTY;
                    outfluid = FluidStack.EMPTY;
                    process = processMax = 0;
                    efficiency = 0;
                    lprocess = 0;
                    this.setActive(false);
                    this.setChanged();
                    return;
                }
                if (fuel <= 0) {
                    fetchFuel();
                }
                if (fuel > 0) {
                    boolean d = false;
                    boolean e = false;
                    if (efficiency <= 1)
                        d = Math.random() < efficiency;
                    else {
                        d = Math.random() < efficiency - 1;
                        e = true;
                    }
                    if ((process / 20 != lprocess) && (d || e)) {
                        if (fluid[0].drain(water, FluidAction.SIMULATE).getAmount() == water) {
                            efficiency += 0.005F;
                            efficiency = Math.min(efficiency, getMaxEfficiency());
                            fluid[0].drain(water, FluidAction.EXECUTE);
                            lprocess = process / 20;
                        } else {
                            if (efficiency <= 0.2 && !isFoodRecipe) {
                                efficiency = 0.2f;
                                return;
                            } else
                                efficiency -= 0.005F;
                            this.setActive(false);
                            this.setChanged();
                            return;
                        }
                    }
                    if (e) process--;
                    if (d)
                        process--;
                    this.setActive(true);
                    fuel--;
                } else {
                    if (efficiency <= 0.2 && !isFoodRecipe) {
                        efficiency = 0.2f;
                        return;
                    } else
                        efficiency -= 0.0005F;
                    this.setActive(false);
                }

                this.setChanged();
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
                IncubateRecipe ir = findRecipe(inventory.get(2), inventory.get(1));
                if (ir != null) {
                    ItemStack outslot = inventory.get(3);
                    if (ir.output.isEmpty() || outslot.isEmpty()
                            || (ir.output.getCount() + outslot.getCount() <= outslot.getMaxStackSize()
                            && ItemHandlerHelper.canItemStacksStack(ir.output, outslot))) {
                        if (ir.output_fluid.isEmpty() || fluid[1].fill(ir.output_fluid,
                                FluidAction.SIMULATE) == ir.output_fluid.getAmount()) {
                            if (ir.input != null)
                                inventory.get(2).shrink(ir.input.getCount());
                            if (ir.consume_catalyst && ir.catalyst != null)
                                inventory.get(1).shrink(ir.catalyst.getCount());
                            if (isFoodRecipe || !ir.getId().equals(last)) {
                                last = ir.getId();
                                efficiency = 0.2f;
                            }
                            process = processMax = ir.time * 20;
                            water = ir.water;
                            out = ir.output.copy();
                            outfluid = ir.output_fluid.copy();
                            isFoodRecipe = false;
                            lprocess = 0;
                            this.setChanged();
                            return;
                        }
                    }
                } else {
                    ItemStack catalyst = inventory.get(1);

                    ItemStack in = inventory.get(2);
                    if (!in.isEmpty() && in.isEdible()) {
                        if (!catalyst.isEmpty() && catalyst.getItem() == Items.ROTTEN_FLESH && (efficiency <= 0.01 || !isFoodRecipe)) {
                            isFoodRecipe = true;
                            last = food;
                            catalyst.shrink(1);
                            efficiency = 0.2f;
                            this.setChanged();
                            return;
                        }
                        if (efficiency > 0.01) {
                            int value = in.getFoodProperties(null).getNutrition();
                            //add caupona
                            /*if (in.getItem() instanceof StewItem) {
                                value = ThermopoliumApi.getInfo(in).healing;

                            } else {
                                out = in.getContainerItem();
                                in.shrink(1);
                            }*/
                            int nvalue = value * 25;
                            outfluid = new FluidStack(getProtein(), nvalue);
                            lprocess = 0;
                            process = processMax = 20 * 20 * value;
                            water = 1;
                            this.setActive(true);
                            this.setChanged();
                            return;
                        }
                    }
                }
                boolean changed = false;
                if (efficiency > 0) {
                    efficiency -= 0.0005F;
                    changed = true;
                }
                if (efficiency < 0.005) {
                    last = null;
                    efficiency = 0;
                    changed = true;
                }
                this.setActive(false);
                if (changed) {
                    this.setChanged();
                }

            }
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag compound, boolean client) {
        compound.putInt("process", process);
        compound.putInt("lprocess", lprocess);
        compound.putInt("processMax", processMax);
        compound.putInt("fuel", fuel);
        compound.putInt("fuelMax", fuelMax);
        compound.putFloat("efficiency", efficiency);
        compound.put("fluid1", fluid[0].writeToNBT(new CompoundTag()));
        compound.put("fluid2", fluid[1].writeToNBT(new CompoundTag()));
        if (!client) {
            if (last != null)
                compound.putString("last", last.toString());
            compound.putInt("water", water);
            ContainerHelper.saveAllItems(compound, this.inventory);
            compound.put("out", out.serializeNBT());
            compound.put("outfluid", outfluid.writeToNBT(new CompoundTag()));
        }
    }


	@Override
	public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
		return new IncubatorT1Container(pContainerId,pPlayerInventory,this,true);
	}

	@Override
	public Component getDisplayName() {
		return Lang.translateGui("incubator.t1.title");
	}

}
