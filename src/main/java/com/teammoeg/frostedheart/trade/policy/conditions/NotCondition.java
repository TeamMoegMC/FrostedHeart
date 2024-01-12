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

package com.teammoeg.frostedheart.trade.policy.conditions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.Conditions;
import com.teammoeg.frostedheart.trade.policy.PolicyCondition;
import net.minecraft.network.PacketBuffer;

public class NotCondition implements PolicyCondition{
	PolicyCondition nested;
	
	public NotCondition(PolicyCondition n) {
		super();
		this.nested=n;
	}
	public NotCondition(JsonObject jo) {
		this(Conditions.deserialize(jo.get("condition").getAsJsonObject()));
	}
	public NotCondition(PacketBuffer buffer) {
		this(Conditions.deserialize(buffer));
	}
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		Conditions.writeType(this, jo);
		jo.add("condition",nested.serialize());
		return jo;
	}

	@Override
	public void write(PacketBuffer buffer) {
		Conditions.writeId(this, buffer);
		nested.write(buffer);
	}

	@Override
	public boolean test(FHVillagerData ve) {
		return !nested.test(ve);
	}

}
