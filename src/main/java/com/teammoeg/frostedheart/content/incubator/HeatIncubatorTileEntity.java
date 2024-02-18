/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.incubator;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.steamenergy.HeatEnergyNetwork;
import com.teammoeg.frostedheart.content.steamenergy.INetworkConsumer;
import com.teammoeg.frostedheart.content.steamenergy.SteamNetworkConsumer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;

public class HeatIncubatorTileEntity extends IncubatorTileEntity implements INetworkConsumer {
    SteamNetworkConsumer network = new SteamNetworkConsumer(80, 5);

    public HeatIncubatorTileEntity() {
        super(FHTileTypes.INCUBATOR2.get());
    }


    @Override
    public boolean canConnectAt(Direction to) {
        return to == this.getBlockState().get(IncubatorBlock.HORIZONTAL_FACING);
    }

    @Override
    public boolean connect(HeatEnergyNetwork manager,Direction to, int dist) {
        return network.reciveConnection(world, pos,manager, to, dist);
    }

    @Override
    protected boolean fetchFuel() {


        if (network.tryDrainHeat(5)) {
            fuel = fuelMax = 400;
            return true;
        }

        return false;
    }
    @Override
    protected float getMaxEfficiency() {
        return 2f;
    }

    @Override
    public boolean isStackValid(int i, ItemStack itemStack) {
        if (i == 0) return false;
        return super.isStackValid(i, itemStack);
    }


    @Override
    public void readCustomNBT(CompoundNBT compound, boolean client) {
        super.readCustomNBT(compound, client);
        network.load(compound);
    }


    @Override
    public void tick() {
        super.tick();

    }


    @Override
    public void writeCustomNBT(CompoundNBT compound, boolean client) {
        super.writeCustomNBT(compound, client);
        network.save(compound);
    }


}
