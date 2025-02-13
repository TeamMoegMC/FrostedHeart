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

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.util.function.Supplier;

import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.network.CMessage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent.Context;

/**
 * Packet to upgrade the generator with the maintenance button.
 */
public class GeneratorModifyPacket implements CMessage {
    public GeneratorModifyPacket() {
    }

    public GeneratorModifyPacket(FriendlyByteBuf buffer) {
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
    }

    @Override
    public void handle(Supplier<Context> context) {

        context.get().enqueueWork(() -> {
            ServerPlayer spe = context.get().getSender();
            AbstractContainerMenu container = spe.containerMenu;
            if (container instanceof GeneratorContainer<?, ?> crncontainer) {
                CMultiblockHelper.getBEHelperOptional(spe.level(), crncontainer.pos.getValue()).ifPresent(helper -> {
                    if (helper.getMultiblock().logic() instanceof GeneratorLogic mas) {
                        mas.onUpgradeMaintainClicked(helper.getContext(), spe);
                    }
                });

            }
        });
        context.get().setPacketHandled(true);
    }

}
