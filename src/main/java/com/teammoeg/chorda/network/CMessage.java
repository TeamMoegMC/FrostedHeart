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

package com.teammoeg.chorda.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;
/**
 * A Message should:<br/>
 * 1. Have a Constructor with one parameter of PacketBuffer as deserializer (IMPORTANT)<br/>
 * 2. Implements methods below<br/>
 * 3. Register class in your network handler. Example: ChordaNetwork<br/>
 * 
 * */
public interface CMessage {

	void encode(FriendlyByteBuf buffer);

	void handle(Supplier<Context> context);
}