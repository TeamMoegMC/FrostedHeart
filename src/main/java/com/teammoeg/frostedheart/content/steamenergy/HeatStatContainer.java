/*
 * Copyright (c) 2026 TeamMoeg
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

import com.teammoeg.chorda.io.SerializeUtil;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHMenuTypes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;

public class HeatStatContainer extends AbstractContainerMenu {
    public Collection<HeatEndpoint> data;
    HeatNetwork network;
    int counter;
    ServerPlayer openedPlayer;

    public HeatStatContainer(int id, Inventory inventoryPlayer, FriendlyByteBuf pb) {
        this(id);
        data = SerializeUtil.readList(pb, HeatEndpoint::readNetwork);
    }

    public HeatStatContainer(int id) {
        super(FHMenuTypes.HEAT_STAT.get(), id);
    }

    public HeatStatContainer(int id, Player opener, HeatNetwork mng) {
        super(FHMenuTypes.HEAT_STAT.get(), id);
        network = mng;
        if (opener instanceof ServerPlayer)
            openedPlayer = (ServerPlayer) opener;
    }


    public boolean stillValid(Player playerIn) {
        return true;
    }

    public void tick() {
        counter++;
        if (counter >= 20) {
            counter = 0;
            EndPointDataPacket epp = new EndPointDataPacket(network);
            FHNetwork.INSTANCE.sendPlayer(openedPlayer, epp);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        // TODO Auto-generated method stub
        return null;
    }
}
