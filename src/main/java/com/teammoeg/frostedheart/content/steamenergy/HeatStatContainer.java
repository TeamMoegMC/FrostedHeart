/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy;

import java.util.Collection;
import com.teammoeg.frostedheart.FHContainer;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.thermopolium.data.recipes.SerializeUtil;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.PacketDistributor;

public class HeatStatContainer extends Container {
    public static final int RELATION_TO_TRADE = -30;
    public Collection<EndPointData> data;
    HeatEnergyNetwork network;
    int counter;
    ServerPlayerEntity openedPlayer;
    public HeatStatContainer(int id, PlayerInventory inventoryPlayer, PacketBuffer pb) {
        this(id);
        data=SerializeUtil.readList(pb, EndPointData::readNetwork);
    }

    public HeatStatContainer(int id) {
        super(FHContainer.HEAT_STAT.get(), id);
    }
    public HeatStatContainer(int id,PlayerEntity opener,HeatEnergyNetwork mng) {
        super(FHContainer.HEAT_STAT.get(), id);
        network=mng;
        if(opener instanceof ServerPlayerEntity)
        	openedPlayer=(ServerPlayerEntity) opener;
    }
    

    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
    }

    public void tick() {
    	counter++;
    	if(counter>=20) {
    		counter=0;
    		EndPointDataPacket epp=new EndPointDataPacket(network);
    		FHNetwork.send(PacketDistributor.PLAYER.with(()->openedPlayer), epp);
    	}
    }
}
