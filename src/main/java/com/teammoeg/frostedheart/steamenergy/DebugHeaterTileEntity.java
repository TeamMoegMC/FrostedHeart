package com.teammoeg.frostedheart.steamenergy;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHContent;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;

public class DebugHeaterTileEntity extends IEBaseTileEntity implements HeatProvider, IConnectable {
    public DebugHeaterTileEntity() {
        super(FHContent.FHTileTypes.DEBUGHEATER.get());
    }

    SteamEnergyNetwork network = new SteamEnergyNetwork(this);

    @Override
    public SteamEnergyNetwork getNetwork() {
        return network;
    }

    @Override
    public float getMaxHeat() {
        return Float.MAX_VALUE;
    }

    @Override
    public float drainHeat(float value) {
        return value;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
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
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof IConnectable && !(te instanceof HeatProvider)) {
            ((IConnectable) te).connectAt(to.getOpposite());
        }
        return true;
    }


    @Override
    public int getTemperatureLevel() {
        return 1;
    }

}
