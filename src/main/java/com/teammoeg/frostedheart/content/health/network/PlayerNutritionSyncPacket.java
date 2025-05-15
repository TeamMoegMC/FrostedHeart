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

package com.teammoeg.frostedheart.content.health.network;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.health.capability.ImmutableNutrition;
import com.teammoeg.frostedheart.content.health.capability.Nutrition;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerNutritionSyncPacket implements CMessage {
    public float fat , carbohydrate, protein,vegetable;

    public PlayerNutritionSyncPacket(FriendlyByteBuf buffer) {
        fat = buffer.readFloat();
        carbohydrate = buffer.readFloat();
        protein = buffer.readFloat();
        vegetable = buffer.readFloat();
    }

    public PlayerNutritionSyncPacket(Nutrition nutrition) {
        this(nutrition.getFat(),nutrition.getCarbohydrate(),nutrition.getProtein(),nutrition.getVegetable());
    }

     public PlayerNutritionSyncPacket(float fat ,float carbohydrate ,float portein,float vegetable) {
        this.fat = fat;
        this.carbohydrate = carbohydrate;
        this.protein = portein;
        this.vegetable = vegetable;
     }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(fat);
        buffer.writeFloat(carbohydrate);
        buffer.writeFloat(protein);
        buffer.writeFloat(vegetable);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> NutritionCapability.getCapability(ClientUtils.getPlayer()).ifPresent(data -> {
            data.set(getNutrition());
        }));
        context.get().setPacketHandled(true);
    }

    public ImmutableNutrition getNutrition(){
        return new ImmutableNutrition(fat,carbohydrate,protein,vegetable);
    }
}
