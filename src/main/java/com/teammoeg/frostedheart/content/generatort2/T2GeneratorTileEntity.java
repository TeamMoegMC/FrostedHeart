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

package com.teammoeg.frostedheart.content.generatort2;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.generator.BurnerGeneratorTileEntity;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.steamenergy.HeatProvider;
import com.teammoeg.frostedheart.steamenergy.IConnectable;
import com.teammoeg.frostedheart.steamenergy.SteamEnergyNetwork;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import java.util.Random;

public class T2GeneratorTileEntity extends BurnerGeneratorTileEntity<T2GeneratorTileEntity> implements HeatProvider, IConnectable {
    public T2GeneratorTileEntity.GeneratorData guiData = new T2GeneratorTileEntity.GeneratorData();

    public T2GeneratorTileEntity(int temperatureLevelIn, int overdriveBoostIn, int rangeLevelIn) {
        super(FHContent.FHMultiblocks.GENERATOR_T2, FHContent.FHTileTypes.GENERATOR_T2.get(), false, temperatureLevelIn, overdriveBoostIn, rangeLevelIn);
    }

    float power = 0;
    SteamEnergyNetwork sen = new SteamEnergyNetwork(this.master());
    GeneratorSteamRecipe steam;
    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        power = nbt.getFloat("steam_power");
        tank.readFromNBT(nbt.getCompound("fluid"));

    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putFloat("steam_power", power);
        CompoundNBT tankx = new CompoundNBT();
        tank.writeToNBT(tankx);
        nbt.put("fluid", tankx);
    }

    public FluidTank tank = new FluidTank(20 * FluidAttributes.BUCKET_VOLUME,f->GeneratorSteamRecipe.findRecipe(f)!=null);

    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        T2GeneratorTileEntity master = master();
        if (master != null && side == Direction.DOWN&&this.offsetToMaster.getX()==0&&this.offsetToMaster.getZ()==0)
            return new FluidTank[]{master.tank};
        return new FluidTank[0];
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        if (side == Direction.DOWN)
            return true;
        return false;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }
    //we have to override all 
    @Override
    protected void tickFuel() {
        // just finished process or during process
        if (process > 0) {
            if (isOverdrive() && !isActualOverdrive()) {
                GeneratorRecipe recipe = getRecipe();
                if (recipe != null) {
                    int count = recipe.input.getCount();
                    if (inventory.get(INPUT_SLOT).getCount() >= 4 * count) {
                        Utils.modifyInvStackSize(inventory, INPUT_SLOT, -4 * count);
                        if (currentItem != null) {
                            if (!inventory.get(OUTPUT_SLOT).isEmpty())
                                inventory.get(OUTPUT_SLOT).grow(currentItem.getCount());
                            else
                                inventory.set(OUTPUT_SLOT, currentItem);
                            currentItem = null;
                        }
                        currentItem = recipe.output.copy();
                        currentItem.setCount(4 * currentItem.getCount());
                        this.process += recipe.time * 4;
                        this.processMax += recipe.time * 4;
                        this.steam=null;
                        GeneratorSteamRecipe sgr=GeneratorSteamRecipe.findRecipe(this.tank.getFluid());
                        if(sgr!=null) {
                        	int actualDrain=recipe.time*this.getTemperatureLevel()*sgr.input.getAmount();
                        	FluidStack fs=this.tank.drain(actualDrain,FluidAction.SIMULATE);
                        	if(fs.getAmount()>=actualDrain) {
	                        	this.steam=sgr;
	                        	this.tank.drain(actualDrain,FluidAction.EXECUTE);
                        	}
                        }
                        setActualOverdrive(true);
                    }
                }
            }
            if (isActualOverdrive())
                process -= 4;
            else
                process--;
            if(steam!=null) {
            	this.power+=steam.power*this.getTemperatureLevel();
	            if(power>=getMaxPower())
	            	power=getMaxPower();
            }
            this.markContainingBlockForUpdate(null);
        }
        // process not started yet
        else {
            if (currentItem != null) {
                if (!inventory.get(OUTPUT_SLOT).isEmpty())
                    inventory.get(OUTPUT_SLOT).grow(currentItem.getCount());
                else
                    inventory.set(OUTPUT_SLOT, currentItem);
                currentItem = null;
            }
            GeneratorRecipe recipe = getRecipe();
            if (recipe != null) {
                int modifier = 1;
                if (isOverdrive() && inventory.get(INPUT_SLOT).getCount() >= 4 * recipe.input.getCount()) {
                    if (!isActualOverdrive())
                        this.setActualOverdrive(true);
                    modifier = 4;
                } else if (isActualOverdrive()) {
                    this.setActualOverdrive(false);
                }
                int count = recipe.input.getCount() * modifier;
                Utils.modifyInvStackSize(inventory, INPUT_SLOT, -count);
                currentItem = recipe.output.copy();
                currentItem.setCount(currentItem.getCount() * modifier);
                this.process = recipe.time * modifier;
                this.processMax = process;
                GeneratorSteamRecipe sgr=GeneratorSteamRecipe.findRecipe(this.tank.getFluid());
                if(sgr!=null) {
                	int actualDrain=recipe.time*this.getTemperatureLevel()*sgr.input.getAmount();
                	FluidStack fs=this.tank.drain(actualDrain,FluidAction.SIMULATE);
                	if(fs.getAmount()>=actualDrain) {
                		if(this.steam!=sgr)
                			this.markChanged(true);
                    	this.steam=sgr;
                    	this.tank.drain(actualDrain,FluidAction.EXECUTE);
                	}
                }
                setActive(true);
            } else {
                this.process = 0;
                processMax = 0;
                setActive(false);
            }
        }
    }

    @Override
    protected void setAllActive(boolean state) {
        for (int x = 0; x < 3; ++x)
            for (int y = 0; y < 7; ++y)
                for (int z = 0; z < 3; ++z) {
                    BlockPos actualPos = getBlockPosForPos(new BlockPos(x, y, z));
                    TileEntity te = Utils.getExistingTileEntity(world, actualPos);
                    if (te instanceof BurnerGeneratorTileEntity)
                        ((BurnerGeneratorTileEntity) te).setActive(state);
                }
    }

    @Override
    protected void tickEffects(boolean isActive) {
        if (isActive) {
            BlockPos blockpos = this.getPos().offset(Direction.UP, 5);
            Random random = world.rand;
            if (isActualOverdrive()) {
                if (random.nextFloat() < 0.9F) {
                    for (int i = 0; i < random.nextInt(2) + 2; ++i) {
                        ClientUtils.spawnSteamParticles(world, blockpos);
                        ClientUtils.spawnT2FireParticles(world, blockpos);
                    }
                }
            } else {
                if (random.nextFloat() < 0.5F) {
                    for (int i = 0; i < random.nextInt(2) + 2; ++i) {
                        ClientUtils.spawnSteamParticles(world, blockpos);
                        ClientUtils.spawnT2FireParticles(world, blockpos);
                    }
                }
            }

        }
    }

    @Override
    public SteamEnergyNetwork getNetwork() {
        return master().sen != null ? master().sen : new SteamEnergyNetwork(this.master());
    }

    @Override
    public float getMaxHeat() {
        return power;
    }

    public float getMaxPower() {
        return 20000;
    }

    @Override
    public float drainHeat(float value) {
        float actual = Math.min(value, power);
        power -= actual;
        return actual;
    }

    @Override
    public int getTemperatureLevel() {
    	if(master().steam!=null) {
    		return (int) (master().steam.tempMod*super.getTemperatureLevel());
    	}
        return super.getTemperatureLevel();
    }

    @Override
	public int getRangeLevel() {
    	if(master().steam!=null) {
    		return (int) (master().steam.rangeMod*super.getRangeLevel());
    	}
		return super.getRangeLevel();
	}

	@Override
    public boolean disconnectAt(Direction to) {
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof IConnectable && !(te instanceof HeatProvider)) {
            ((IConnectable) te).disconnectAt(to.getOpposite());
        }
        return true;
    }

    @Override
    public boolean connectAt(Direction to) {
    	if (to != Direction.DOWN ||this.offsetToMaster.getY() != 0||(this.offsetToMaster.getX()!=0&&this.offsetToMaster.getZ()!=0)) return false;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof IConnectable && !(te instanceof HeatProvider)) {
            ((IConnectable) te).connectAt(to.getOpposite());
        }
        return true;
    }

	@Override
	public boolean canConnectAt(Direction to) {
		return to == Direction.UP &&(this.offsetToMaster.getX()==0||this.offsetToMaster.getZ()==0);
	}


}
