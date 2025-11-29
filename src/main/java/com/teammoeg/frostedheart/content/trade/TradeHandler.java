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

package com.teammoeg.frostedheart.content.trade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.nbt.CompoundTag;

public class TradeHandler {
    public static void openTradeScreen(ServerPlayer spe, FHVillagerData vd) {
        vd.update(spe.serverLevel(), spe);
        NetworkHooks.openScreen(spe, vd, e -> {
            e.writeVarInt(vd.parent.getId());
            CompoundTag tag = new CompoundTag();
            e.writeNbt(vd.serializeForSend(tag));
            tag = new CompoundTag();
            e.writeNbt(vd.getRelationDataForRead(spe).serialize(tag));
            vd.getRelationShip(spe).write(e);
        });
    }

}
