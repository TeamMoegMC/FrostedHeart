package com.teammoeg.frostedheart.tileentity;

import com.teammoeg.frostedheart.content.FHMultiblocks;
import com.teammoeg.frostedheart.content.FHTileTypes;
import com.teammoeg.frostedheart.steamenergy.HeatProvider;
import com.teammoeg.frostedheart.steamenergy.IConnectable;
import com.teammoeg.frostedheart.steamenergy.SteamEnergyNetwork;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorTileEntity extends BurnerGeneratorTileEntity<T2GeneratorTileEntity> implements HeatProvider,IConnectable {
	public T2GeneratorTileEntity.GeneratorData guiData = new T2GeneratorTileEntity.GeneratorData();
	public T2GeneratorTileEntity(int temperatureLevelIn, int overdriveBoostIn, int rangeLevelIn) {
		super(FHMultiblocks.GENERATOR_T2,FHTileTypes.GENERATOR_T2.get(),false,temperatureLevelIn, overdriveBoostIn, rangeLevelIn);
	}
	float power=0;
	SteamEnergyNetwork sen=new SteamEnergyNetwork(this);
	@Override
	public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
		super.readCustomNBT(nbt, descPacket);
		power=nbt.getFloat("steam_power");
		tank.readFromNBT(nbt.getCompound("fluid"));
		
	}
	@Override
	public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
		super.writeCustomNBT(nbt, descPacket);
		nbt.putFloat("steam_power",power);
		CompoundNBT tankx=new CompoundNBT();
		tank.writeToNBT(tankx);
		nbt.put("fluid",tankx);
	}
	public FluidTank tank = new FluidTank(20*FluidAttributes.BUCKET_VOLUME);
	@Override
	protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
		T2GeneratorTileEntity master = master();
		if(master!=null&&side==Direction.DOWN)
			return new FluidTank[]{master.tank};
		return new FluidTank[0];
	}
	@Override
	protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
		if(side==Direction.DOWN)
			return true;
		return false;
	}
	@Override
	protected boolean canDrainTankFrom(int iTank, Direction side) {
		return false;
	}
	@Override
	protected void tickFuel() {
		super.tickFuel();
		if(this.getIsActive()) {
			power+=25*this.getTemperatureLevel();
		}
	}
	@Override
	public SteamEnergyNetwork getNetwork() {
		return sen;
	}
	@Override
	public float getMaxHeat() {
		return 100000;
	}
	@Override
	public float drainHeat(float value) {
		float actual=Math.min(value,power);
		power-=actual;
		return actual;
	}
	@Override
	public float getTemperatureLevel() {
		return super.getTemperatureLevel();
	}
	@Override
	public boolean disconnectAt(Direction to) {
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof IConnectable&&!(te instanceof HeatProvider)) {
			((IConnectable) te).disconnectAt(to.getOpposite());
		}
		return true;
	}
	@Override
	public boolean connectAt(Direction to) {
		if(to!=Direction.DOWN&&this.offsetToMaster.getY()!=0)return false;
		TileEntity te=Utils.getExistingTileEntity(this.getWorld(),this.getPos().offset(to));
		if(te instanceof IConnectable&&!(te instanceof HeatProvider)) {
			((IConnectable) te).connectAt(to.getOpposite());
		}
		return true;
	}
	
	
}
