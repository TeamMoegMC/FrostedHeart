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

package com.teammoeg.frostedheart.steamenergy;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;

import java.util.Random;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class HeatPipeTileEntity extends IEBaseTileEntity implements EnergyNetworkProvider, ITickableTileEntity, FHBlockInterfaces.IActiveState, IConnectable {
    protected Direction dMaster;
    private SteamEnergyNetwork network;
    private int length = Integer.MAX_VALUE;
    private boolean networkinit;
    private boolean isPathFinding;
    private boolean requireRP;
    public HeatPipeTileEntity() {
        super(FHContent.FHTileTypes.HEATPIPE.get());
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (descPacket) return;
        if (nbt.contains("dm"))
            dMaster = Direction.values()[nbt.getInt("dm")];
        length = nbt.getInt("length");
        requireRP = nbt.getBoolean("rep");
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        if (descPacket) return;
        if (dMaster != null)
            nbt.putInt("dm", dMaster.ordinal());
        nbt.putInt("length", length);
        nbt.putBoolean("rep", requireRP);
    }

    @Override
    public SteamEnergyNetwork getNetwork() {
        if (networkinit) return null;
        try {
            networkinit = true;//avoid recursive calling
            if (hasNetwork() && dMaster != null) {
                TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(dMaster));
                if (te instanceof EnergyNetworkProvider) {
                    SteamEnergyNetwork tnetwork = ((EnergyNetworkProvider) te).getNetwork();
                    if (tnetwork != null&&tnetwork.isValid()) {
                        network = tnetwork;
                    }
                }
            }
        } finally {
            networkinit = false;
        }
        if(hasNetwork())
        	return network;
        return null;
    }
    protected boolean hasNetwork() {
    	return network!=null&&network.isValid();
    }
    protected void propagate(Direction from, SteamEnergyNetwork newNetwork, int lengthx) {
        if (isPathFinding) return;
        //System.out.println(from);
        try {
            isPathFinding = true;
            final SteamEnergyNetwork network = getNetwork();
            if (network != null && newNetwork != null && network != newNetwork) {//network conflict
                return;//disconnect
            }
            if (newNetwork == null) {
                unpropagate(from);
                return;
            }
            //setActive(true);
            if (length <= lengthx) return;
            length = lengthx;
            dMaster = from;
            if (network != newNetwork) {
                this.network = newNetwork;
                for (Direction d : Direction.values()) {
                    if (dMaster == d) continue;
                    BlockPos n = this.getPos().offset(d);
                    TileEntity te = Utils.getExistingTileEntity(this.getWorld(), n);
                    if (te instanceof HeatPipeTileEntity) {
                        ((HeatPipeTileEntity) te).propagate(d.getOpposite(), this.network, length + 1);
                    } else if (te instanceof IConnectable) {
                        ((IConnectable) te).connectAt(d.getOpposite());
                    }
                }
            }
            return;
        } finally {
            isPathFinding = false;
        }
    }

    protected void unpropagate(Direction from) {
        doUnpropagate(from);
    }

    protected void doUnpropagate(Direction from) {
        if (dMaster == null) return;
        if (dMaster == from) {
            network = null;
            dMaster = null;
            length = Integer.MAX_VALUE;
            for (Direction d : Direction.values()) {
                if (d == from) continue;
                BlockPos n = this.getPos().offset(d);
                TileEntity te = Utils.getExistingTileEntity(this.getWorld(), n);
                if (te instanceof HeatPipeTileEntity) {
                    ((HeatPipeTileEntity) te).unpropagate(d.getOpposite());
                } else if (te instanceof IConnectable) {
                    ((IConnectable) te).disconnectAt(d.getOpposite());
                }
            }
            //setActive(false);
        } else {
            requireRP = true;
        }
    }

    /*
    protected int findPathToMaster(Direction from) {
        if(isVisiting)return -1;
        try {
            isVisiting=true;
            int result=-1;
            dMaster.remove(from);//avoid cycle detection
            if(!dMaster.isEmpty()) {
                for(int i:dMaster.values())
                    result=Math.min(result,i);
                return result;
            }
            network=null;//assume no network
            for(Direction d:Direction.values()) {
                if(d==from)continue;
                BlockPos n=this.getPos().add(d.getDirectionVec());
                TileEntity te = Utils.getExistingTileEntity(this.getWorld(),n);
                if (te instanceof HeatPipeTileEntity) {
                    int rs=((HeatPipeTileEntity) te).findPathToMaster(d.getOpposite())+1;
                    if(rs>0){
                        if(result==-1)
                            result=rs;
                        else
                            result=Math.min(rs,result);
                        dMaster.put(d,rs);
                        SteamEnergyNetwork newNetwork=((HeatPipeTileEntity) te).getNetwork();
                        if(network!=null&&network!=newNetwork) {//network conflict
                            dMaster.clear();
                            return -1;//disconnect
                        }
                        network=newNetwork;
                    }
                }else if(te instanceof HeatProvider) {
                    SteamEnergyNetwork newNetwork=((HeatProvider) te).getNetwork();
                    if(network!=null&&network!=newNetwork) {//network conflict
                        dMaster.clear();
                        return -1;//disconnect
                    }
                    network=newNetwork;
                    result=1;
                    dMaster.put(d,result);
                }
            }

            return result;
        }finally {
            isVisiting=false;
        }
    }
    */
    public boolean disconnectAt(Direction to) {
        if (network != null)
            unpropagate(to);//try find new path
        return true;
    }

    public boolean connectAt(Direction to) {
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        final SteamEnergyNetwork network = getNetwork();
        if (te instanceof HeatProvider) {
            SteamEnergyNetwork newNetwork = ((HeatProvider) te).getNetwork();
            if (network == null) {
                this.propagate(to, newNetwork, 1);
            }
            return true;
        }

        if (te instanceof HeatPipeTileEntity) {
            if (network != null)
                if (dMaster != to) {
                    ((HeatPipeTileEntity) te).propagate(to.getOpposite(), network, length);
                    this.getBlockState().with(HeatPipeBlock.FACING_TO_PROPERTY_MAP.get(to), true);
                }
            return true;
        } else if (te instanceof IConnectable) {
            if (network != null) {
                ((IConnectable) te).connectAt(to.getOpposite());
                this.getBlockState().with(HeatPipeBlock.FACING_TO_PROPERTY_MAP.get(to), true);
            }
            return true;
        } else {
            disconnectAt(to);
        }
        return false;
    }

    @Override
    public void tick() {
        if (requireRP) {
            requireRP = false;
            if (dMaster != null) {
                for (Direction d : Direction.values()) {
                    if (d == dMaster) continue;
                    BlockPos n = this.getPos().offset(d);
                    TileEntity te = Utils.getExistingTileEntity(this.getWorld(), n);
                    if (te instanceof HeatPipeTileEntity) {
                        ((HeatPipeTileEntity) te).propagate(d.getOpposite(), network, length + 1);
                    }
                }
            }
        }
    	if(network != null&&network.drainHeat(network.getTemperatureLevel() * 0.15F)>=0.15) {
    		setActive(true);
    	}else {
    		setActive(false);
    	}
    }

	@Override
	public boolean canConnectAt(Direction to) {
		return true;
	}
}
