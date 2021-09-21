package com.teammoeg.frostedheart.steamenergy;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.content.FHTileTypes;
import com.teammoeg.frostedheart.state.FHBlockInterfaces;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;

public class RadiatorTileEntity extends IEBaseTileEntity implements
	IConnectable,IIEInventory,IEBlockInterfaces.IInteractionObjectIE,IEBlockInterfaces.IProcessTile,FHBlockInterfaces.IActiveState,ITickableTileEntity{
	NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
	public float power=0;
	public int process = 0;
    public int processMax = 0;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
	public RadiatorTileEntity() {
		super(FHTileTypes.RADIATOR.get());
	}

	SteamEnergyNetwork network;
	Direction last;
	SteamEnergyNetwork getNetwork() {
		if(network!=null)return network;
		if(last==null)return null;
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(last));
		if(te instanceof EnergyNetworkProvider) {
			network=((EnergyNetworkProvider) te).getNetwork();
		}else {
			disconnectAt(last);
		}
		return network;
	}
	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
		ItemStackHelper.loadAllItems(nbt, inventory);
		power=nbt.getFloat("power");
        process = nbt.getInt("process");
        processMax = nbt.getInt("processMax");
		if(nbt.contains("dir"))
			last=Direction.values()[nbt.getInt("dir")];
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
		ItemStackHelper.saveAllItems(nbt, inventory);
		nbt.putFloat("power",power);
        nbt.putInt("process", process);
        nbt.putInt("processMax", processMax);
		if(last!=null)
			nbt.putInt("dir",last.ordinal());
	}

	@Override
	public boolean disconnectAt(Direction to) {
		if(last==to) {
			network=null;
			for(Direction d:Direction.values()) {
				if(d==to)continue;
				if(connectAt(d))
					break;
			}
		}
		return true;
	}

	@Override
	public boolean connectAt(Direction to) {
		if(to==Direction.UP)return false;
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof EnergyNetworkProvider) {
			last=to;
			network=((EnergyNetworkProvider) te).getNetwork();
			return true;
		}else
			disconnectAt(to);
		return false;
	}


	@Override
	public IInteractionObjectIE getGuiMaster() {
		return this;
	}

	@Override
	public boolean canUseGui(PlayerEntity player) {
		return true;
	}

	@Override
	public NonNullList<ItemStack> getInventory() {
		return inventory;
	}

	@Override
	public boolean isStackValid(int slot, ItemStack stack) {
        return false;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public void doGraphicalUpdates(int slot) {
	}

	@Override
	public void tick() {
		SteamEnergyNetwork network=getNetwork();
		boolean isDirty=false;
		if(network!=null) {
			float actual=network.drainHeat(Math.min(2000,500000-power));
			if(actual>0) {
				power+=actual;
				isDirty=true;
				//world.notifyBlockUpdate(this.getPos(),this.getBlockState(),this.getBlockState(),3);
			}
		}
		boolean beforeState=this.getIsActive();
		boolean afterState=false;
        if (process > 0) {
        	if (network!=null)
        		process-=network.getTemperatureLevel();
        	else
        		process--;
            isDirty=true;
            afterState=true;
        }else if(power>=100000) {
        	power-=4000*network.getTemperatureLevel();
        	process=(int) (1000*network.getTemperatureLevel());
        	processMax=(int) (1000*network.getTemperatureLevel());
			isDirty=true;
			afterState=true;
		}
        if(beforeState!=afterState) {
        	this.setActive(afterState);
        	if(afterState) {
        		if(network!=null)
        			ChunkData.addCubicTempAdjust(this.getWorld(),this.getPos(),8,(byte) (8*network.getTemperatureLevel()));
        		else
        			ChunkData.addCubicTempAdjust(this.getWorld(),this.getPos(),8,(byte) 8);
        	}else
        		ChunkData.removeTempAdjust(this.getWorld(),this.getPos(),8);
        }
		if(isDirty) {
			markDirty();
			this.markContainingBlockForUpdate(null);
		}
	}
    @Override
    public int[] getCurrentProcessesStep() {
        return new int[]{processMax - process};
    }

    @Override
    public int[] getCurrentProcessesMax() {
        return new int[]{processMax};
    }

}
