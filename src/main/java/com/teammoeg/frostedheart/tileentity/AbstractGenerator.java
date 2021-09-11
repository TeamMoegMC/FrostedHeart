package com.teammoeg.frostedheart.tileentity;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.state.FHBlockInterfaces;

import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;

public abstract class AbstractGenerator<T extends AbstractGenerator<T>> extends MultiblockPartTileEntity<T> implements FHBlockInterfaces.IActiveState{

	public int temperatureLevel;
	public int rangeLevel;
	public int overdriveBoost;
	boolean isWorking;
	boolean isOverdrive;
	boolean isDirty;//mark if user changes settings
	public AbstractGenerator(IETemplateMultiblock multiblockInstance, TileEntityType<T> type, boolean hasRSControl) {
		super(multiblockInstance, type, hasRSControl);
	}

	public int getActualRange() {
	    return 8 + (getRangeLevel() - 1) * 4;
	}

	public int getActualTemp() {
	    return getTemperatureLevel() * 10;
	}

	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
	    super.readCustomNBT(nbt, descPacket);
	    setWorking(nbt.getBoolean("isWorking"));
	    setOverdrive(nbt.getBoolean("isOverdrive"));
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
	    super.writeCustomNBT(nbt, descPacket);
	    nbt.putBoolean("isWorking", isWorking());
	    nbt.putBoolean("isOverdrive", isOverdrive());

	}

	@Override
	public void disassemble() {
	    super.disassemble();
	    ChunkData.removeTempAdjust(world, getPos(), getActualRange());
	}
	protected abstract void onShutDown();
	protected abstract void tickFuel();
	protected abstract void setAllActive(boolean state);
	protected abstract void tickEffects(boolean isActive);
	@Override
	public void tick() {
	    checkForNeedlessTicking();
	    // spawn smoke particle
	    if (world != null && world.isRemote && formed && !isDummy()) {
	    	tickEffects(getIsActive());
	    }
	    //user set shutdown
	    if(isUserOperated())
		    if (!world.isRemote && formed && !isDummy() && !isWorking()) {
		    	setActive(false);
		    	onShutDown();
		        ChunkData.removeTempAdjust(world, getPos(), getActualRange());
		    }
	    if (!world.isRemote && formed && !isDummy() && isWorking()) {
	        final boolean activeBeforeTick = getIsActive();
	        tickFuel();
	        // set activity status
	        final boolean activeAfterTick = getIsActive();
	        if (activeBeforeTick != activeAfterTick) {
	            this.markDirty();
	            if (activeAfterTick) {
	                ChunkData.addCubicTempAdjust(world, getPos(),getActualRange(), (byte) getActualTemp());
	            } else {
	                ChunkData.removeTempAdjust(world, getPos(), getActualRange());
	            }
	            setAllActive(activeAfterTick);        
	        } else if (activeAfterTick) {
	            if (isUserOperated()) {
	            	markUserOperation(false);
	                ChunkData.addCubicTempAdjust(world, getPos(),getActualRange(), (byte) getActualTemp());
	            }
	        }
	    }
	
	}

	public void setWorking(boolean working) {
	    if (master() != null) {
	        master().isWorking = working;
	        markUserOperation(true);
	    }
	}

	public boolean isWorking() {
	    if (master() != null)
	        return master().isWorking;
		return false;
	}

	public boolean isOverdrive() {
	    if (master() != null)
	        return master().isOverdrive;
		return false;
	}
	public void markUserOperation(boolean dirty) {
		if (master() != null)
	        master().isDirty=dirty;
	}

	public boolean isUserOperated() {
	    if (master() != null)
	        return master().isDirty;
		return false;
	}
	public void setOverdrive(boolean overdrive) {
	    if (master() != null) {
	    	markUserOperation(true);
	        master().isOverdrive = overdrive;
	    }
	}
	public int getTemperatureLevel() {
	    if (master() != null)
	        return master().temperatureLevel*(isOverdrive()?master().overdriveBoost:1);
	    return 1;
	}
	public int getRangeLevel() {
	    if (master() != null)
	        return master().rangeLevel;
		return 1;
	}

}