/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.generator;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.town.GeneratorData;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public abstract class BurnerGeneratorTileEntity<T extends BurnerGeneratorTileEntity<T>> extends ZoneHeatingMultiblockTileEntity<T> implements IIEInventory,
        FHBlockInterfaces.IActiveState, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, IEBlockInterfaces.IBlockBounds {

    @Override
    public boolean shouldUnique() {
        return true;
    }

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public int process;
    public int processMax;
    protected ItemStack currentItem;
    //local inventory, prevent lost
	NonNullList<ItemStack> linventory = NonNullList.withSize(2, ItemStack.EMPTY);

    public class GeneratorUIData implements IIntArray {
        public static final int MAX_BURN_TIME = 0;
        public static final int BURN_TIME = 1;

        @Override
        public int get(int index) {
            switch (index) {
                case MAX_BURN_TIME:
                	return processMax;
                case BURN_TIME:
                	return process;
                default:
                    throw new IllegalArgumentException("Unknown index " + index);
            }
        }

        @Override
        public void set(int index, int value) {
        	//throw new UnsupportedOperationException();
            switch (index) {
                case MAX_BURN_TIME:
                	processMax=value;
                    break;
                case BURN_TIME:
                    process = value;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown index " + index);
            }
        }

        @Override
        public int size() {
            return 2;
        }
    }

    public BurnerGeneratorTileEntity(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
        super(multiblockInstance, type, hasRSControl);
        
    }
    public Optional<GeneratorData> getData(){
    	return getTeamData().map(t->t.generatorData).filter(t->this.pos.equals(t.actualPos));
    }
    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx) {
        return VoxelShapes.fullCube();
    }

    @Override
    public boolean receiveClientEvent(int id, int arg) {
        if (id == 0)
            this.formed = arg == 1;
        markDirty();
        this.markContainingBlockForUpdate(null);
        return true;
    }

    @Override
    public void receiveMessageFromClient(CompoundNBT message) {
        super.receiveMessageFromClient(message);
        if (message.contains("isWorking", Constants.NBT.TAG_BYTE))
            setWorking(message.getBoolean("isWorking"));
        if (message.contains("isOverdrive", Constants.NBT.TAG_BYTE))
            setOverdrive(message.getBoolean("isOverdrive"));
        this.markContainingBlockForUpdate(null);
        this.markDirty();
       /* if (message.contains("temperatureLevel", Constants.NBT.TAG_INT))
            setTemperatureLevel(message.getInt("temperatureLevel"));
        if (message.contains("rangeLevel", Constants.NBT.TAG_INT))
            setRangeLevel(message.getInt("rangeLevel"));*/
    }

    @Nonnull
    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Nullable
    @Override
    public IEBlockInterfaces.IInteractionObjectIE getGuiMaster() {
        return master();
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return formed;
    }

    @Override
    public int[] getCurrentProcessesStep() {
        T master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesStep();
        return new int[]{getData().map(t->t.processMax-t.process).orElse(0)};
    }

    @Override
    public int[] getCurrentProcessesMax() {
        T master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesMax();
        return new int[]{getData().map(t->t.processMax).orElse(0)};
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        T master = master();
        return Optional.ofNullable(master).flatMap(t->t.getData()).map(t->t.getInventory()).orElseGet(()->master!=null?master.linventory:this.linventory);
    }
    public boolean isDataPresent() {
    	T master = master();
    	return Optional.ofNullable(master).flatMap(t->t.getData()).isPresent();
    }
    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return GeneratorRecipe.findRecipe(stack) != null;
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public void doGraphicalUpdates() {

    }

    LazyOptional<IItemHandler> invHandler = registerConstantCap(
            new IEInventoryHandler(2, this, 0, new boolean[]{true, false},
                    new boolean[]{false, true})
    );

    @Nonnull
    @Override
    public <X> LazyOptional<X> getCapability(@Nonnull Capability<X> capability, Direction facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            T master = master();
            if (master != null)
                return master.invHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void onShutDown() {
    }
    @Override
    protected void tickFuel() {
        // just finished process or during process
    	Optional<GeneratorData> data=this.getData();
    	data.ifPresent(t->t.tick());
    	this.setActive(data.map(t->t.isActive).orElse(false));
    	process=data.map(t->t.process).orElse(0);
    	processMax=data.map(t->t.processMax).orElse(0);
    	/*if(this.getIsActive())
    		this.markContainingBlockForUpdate(null);*/
    }


    @Override
    protected void tickEffects(boolean isActive) {

    }

	@Override
	public void tick() {
		
		super.tick();
		
	}

	@Override
	public void forEachBlock(Consumer<T> consumer) {
	}



}
