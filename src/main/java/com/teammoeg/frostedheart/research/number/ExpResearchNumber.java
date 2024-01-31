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

package com.teammoeg.frostedheart.research.number;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.util.evaluator.Evaluator;
import com.teammoeg.frostedheart.util.evaluator.Node;

import net.minecraft.network.PacketBuffer;

public class ExpResearchNumber implements IResearchNumber {
    Node calc;
    String exp;

    public ExpResearchNumber(JsonObject buffer) {
        this(buffer.get("exp").getAsString());
    }

    public ExpResearchNumber(PacketBuffer buffer) {
        this(buffer.readString());
    }

    public ExpResearchNumber(String exp) {
        super();
        this.exp = exp;
        calc = Evaluator.eval(exp);
    }

    @Override
    public double getVal(ResearchData rd) {
        return calc.eval(rd);
    }

    public JsonElement serialize() {
        return new JsonPrimitive(exp);
    }

    public void write(PacketBuffer buffer) {
        buffer.writeString(exp);
    }

}
