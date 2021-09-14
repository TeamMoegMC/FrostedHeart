package com.teammoeg.frostedheart.steamenergy;

import com.teammoeg.frostedheart.content.FHTileTypes;
import com.teammoeg.frostedheart.recipe.GeneratorRecipe;
import com.teammoeg.frostedheart.tileentity.BurnerGeneratorTileEntity;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;

public class ChargerTileEntity extends IEBaseTileEntity implements
	IConnectable,IIEInventory,IEBlockInterfaces.IInteractionObjectIE,ITickableTileEntity{
	NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
	public float power=0;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
	public ChargerTileEntity() {
		super(FHTileTypes.CHARGER.get());
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
		if(nbt.contains("dir"))
			last=Direction.values()[nbt.getInt("dir")];
	}

	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
		ItemStackHelper.saveAllItems(nbt, inventory);
		nbt.putFloat("power",power);
		if(last!=null)
			nbt.putInt("dir",last.ordinal());
	}

	@Override
	public void disconnectAt(Direction to) {
		if(last==to)
			for(Direction d:Direction.values()) {
				if(d==to)continue;
				TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(d));
				if(te instanceof EnergyNetworkProvider) {
					last=d;
					network=((EnergyNetworkProvider) te).getNetwork();
					break;
				}
			}
	}

	@Override
	public void connectAt(Direction to) {
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof EnergyNetworkProvider) {
			last=to;
			network=((EnergyNetworkProvider) te).getNetwork();
		}
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
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return (stack.getItem() instanceof IChargable);
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
		if(inventory.get(INPUT_SLOT)!=null) {
			Item i=inventory.get(INPUT_SLOT).getItem();
			if(i instanceof IChargable) {
				float actual=((IChargable)i).charge(inventory.get(INPUT_SLOT),Math.min(power,20000));
				power-=actual;
				if(power>0&&actual<=0) {
					ItemStack is=inventory.get(INPUT_SLOT);
					inventory.set(INPUT_SLOT,ItemStack.EMPTY);
					inventory.set(OUTPUT_SLOT,is);
				}
				isDirty=true;
			}
		}
		if(isDirty) {
			markDirty();
			this.markContainingBlockForUpdate(null);
		}
	}

}
