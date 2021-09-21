package com.teammoeg.frostedheart.tileentity;

import com.teammoeg.frostedheart.content.FHMultiblocks;
import com.teammoeg.frostedheart.content.FHTileTypes;
import com.teammoeg.frostedheart.steamenergy.HeatProvider;
import com.teammoeg.frostedheart.steamenergy.SteamEnergyNetwork;

import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class T2GeneratorTileEntity extends BurnerGeneratorTileEntity<T2GeneratorTileEntity> implements HeatProvider {
	public T2GeneratorTileEntity.GeneratorData guiData = new T2GeneratorTileEntity.GeneratorData();
	public T2GeneratorTileEntity(int temperatureLevelIn, int overdriveBoostIn, int rangeLevelIn) {
		super(FHMultiblocks.GENERATOR_T2,FHTileTypes.GENERATOR_T2.get(),false,temperatureLevelIn, overdriveBoostIn, rangeLevelIn);
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
	}
	@Override
	public SteamEnergyNetwork getNetwork() {
		return null;
	}
	@Override
	public float getMaxHeat() {
		return 100000;
	}
	@Override
	public float drainHeat(float value) {
		return 0;
	}
	@Override
	public float getTemperatureLevel() {
		return this.getTemperatureLevel();
	}
	
	
}
