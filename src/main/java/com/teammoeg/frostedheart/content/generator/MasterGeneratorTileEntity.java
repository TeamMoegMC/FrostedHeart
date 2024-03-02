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

package com.teammoeg.frostedheart.content.generator;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.mixin.IOwnerChangeListener;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public abstract class MasterGeneratorTileEntity<T extends MasterGeneratorTileEntity<T>> extends ZoneHeatingMultiblockTileEntity<T> implements IIEInventory,
        FHBlockInterfaces.IActiveState, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, IEBlockInterfaces.IBlockBounds,IOwnerChangeListener {
	public static final int PROCESS=0;
	public static final int PROCESS_MAX=1;
	public static final int OVERDRIVE=2;
	public static final int POWER=3;
	public static final int TLEVEL=4;
	public static final int RLEVEL=5;
    public IIntArray guiData = new IIntArray() {

    	IIntArray base=new IntArray(4);
		@Override
		public int get(int index) {
			if(index<base.size())
				return base.get(index);
			index-=base.size();
			switch(index) {
			case 0:return (int) (getTemperatureLevel()*100);
			case 1:return (int) (getRangeLevel()*100);
			}
			return 0;
		}

		@Override
		public void set(int index, int value) {
			
			if(index<base.size()) {
				base.set(index,value);
				return;
			}
			index-=base.size();
			switch(index) {
			case 0:setTemperatureLevel(value/100f);break;
			case 1:setRangeLevel(value/100f);break;
			}
		}

		@Override
		public int size() {
			return base.size()+2;
		}
    	
    };

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private boolean hasFuel;//for rendering
    protected ItemStack currentItem;
    //local inventory, prevent lost
    NonNullList<ItemStack> linventory = NonNullList.withSize(2, ItemStack.EMPTY);

    LazyOptional<IItemHandler> invHandler = registerConstantCap(
            new IEInventoryHandler(2, this, 0, new boolean[]{true, false},
                    new boolean[]{false, true})
    );

    public MasterGeneratorTileEntity(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
        super(multiblockInstance, type, hasRSControl);

    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return formed;
    }

    @Override
    public void disassemble() {
        if (master() != null)
            master().unregist();
        super.disassemble();
    }

    @Override
    public void doGraphicalUpdates() {

    }

    @Nonnull
    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx) {
        return VoxelShapes.fullCube();
    }

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
    public int[] getCurrentProcessesMax() {
        T master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesMax();
        return new int[]{getData().map(t -> t.processMax).orElse(0)};
    }

    @Override
    public int[] getCurrentProcessesStep() {
        T master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesStep();
        return new int[]{getData().map(t -> t.processMax - t.process).orElse(0)};
    }

    public final Optional<GeneratorData> getData() {
        return getTeamData().map(t -> t.getData(SpecialDataTypes.GENERATOR_DATA)).filter(t -> master().pos.equals(t.actualPos));
    }

    @Nullable
    @Override
    public IEBlockInterfaces.IInteractionObjectIE getGuiMaster() {
        return master();
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        T master = master();
        return Optional.ofNullable(master).flatMap(t -> t.getData()).map(t -> t.getInventory()).orElseGet(() -> master != null ? master.linventory : this.linventory);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    public boolean isDataPresent() {
        T master = master();
        return Optional.ofNullable(master).flatMap(t -> t.getData()).isPresent();
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return findRecipe(stack) != null;
        return false;
    }
    @SuppressWarnings("resource")
	public GeneratorRecipe findRecipe(ItemStack input) {
        for (GeneratorRecipe recipe : FHUtils.filterRecipes(this.getWorld().getRecipeManager(), GeneratorRecipe.TYPE))
            if (recipe.input.test(input))
                return recipe;
        return null;
    }
    @Override
    public void onShutDown() {
    }
    @Override
    public void shutdownTick() {
        boolean invState = !this.getInventory().get(INPUT_SLOT).isEmpty()||this.guiData.get(PROCESS)>0;
        if (invState != hasFuel) {
            hasFuel = invState;
            this.markContainingBlockForUpdate(null);
        }

    }
    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        ItemStackHelper.loadAllItems(nbt, linventory);
        hasFuel = nbt.getBoolean("hasFuel");
        if(!descPacket&&this.getWorld() instanceof ServerWorld) {
            Optional<GeneratorData> data = this.getData();
            data.ifPresent(t -> {
                this.isOverdrive=t.isOverdrive;
                this.isWorking=t.isWorking;
            });
        }
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

    public void regist() {
        // TODO: seems to be an issue here. getData is empty
    	getData().ifPresent(t -> {
        	if(!master().pos.equals(t.actualPos))
        		t.onPosChange();
        	this.setWorking(t.isWorking);
        	this.setOverdrive(t.isOverdrive);
            t.actualPos = master().pos;
            t.dimension = this.world.getDimensionKey();
        });
    }

	@Override
	public void onOwnerChange() {
		if(!this.isDummy())
			regist();
	}
    @Override
    public void tick() {

        super.tick();

    }

    @Override
    protected void tickEffects(boolean isActive) {

    }
    protected void tickDrives(boolean isActive) {

    }
    @Override
    protected boolean tickFuel() {
        // just finished process or during process
        Optional<GeneratorData> data = this.getData();
        data.ifPresent(t -> {
            t.isOverdrive = this.isOverdrive;
            t.isWorking = this.isWorking;
        });
        data.ifPresent(t -> t.tick(this.getWorld()));
        boolean isWorking=data.map(t -> t.isActive).orElse(false);
        setTemperatureLevel(data.map(t -> t.TLevel).orElse(0F));
        setRangeLevel(data.map(t -> t.RLevel).orElse(0F));
        guiData.set(PROCESS, data.map(t -> t.process).orElse(0));
        guiData.set(PROCESS_MAX, data.map(t -> t.processMax).orElse(0));
        guiData.set(OVERDRIVE, data.map(t -> t.overdriveLevel).orElse(0));
        guiData.set(POWER, (int)(float)data.map(t->t.power).orElse(0F));
        tickDrives(isWorking);
        return isWorking;
    	/*if(this.getIsActive())
    		this.markContainingBlockForUpdate(null);*/
    }

    public void tickHeat(boolean isWorking) {
    }
    public void unregist() {
        getData().ifPresent(t -> {
            t.actualPos = BlockPos.ZERO;
            t.dimension = null;
        });
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        if(!this.isDummy()||descPacket) {
	        ItemStackHelper.saveAllItems(nbt, linventory);
	        nbt.putBoolean("hasFuel", hasFuel);
        }
    }


    @Override
    public int getLowerBound() {
        int extra = MathHelper.ceil(getRangeLevel()*2+1);
        return extra;
    }

    @Override
    public int getUpperBound() {
        int rLevel = MathHelper.ceil(getRangeLevel() * 4+1);
        return rLevel;
    }
	@Override
	protected void callBlockConsumerWithTypeCheck(Consumer<T> consumer, TileEntity te) {
	}

	@Override
	public void setRangeLevel(float f) {
		super.setRangeLevel(f);
	}

	@Override
	public void setTemperatureLevel(float temperatureLevel) {
		super.setTemperatureLevel(temperatureLevel);
	}

	public boolean hasFuel() {
		return hasFuel;
	}
}
