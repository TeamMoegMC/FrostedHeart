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

package com.teammoeg.frostedheart.content.research.number;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.content.research.data.ResearchData;

import net.minecraft.network.PacketBuffer;

public class ConstResearchNumber implements IResearchNumber {
    private static Cache<Number, ConstResearchNumber> cb = CacheBuilder.newBuilder().expireAfterAccess(20, TimeUnit.SECONDS).build();
    final Number n;

    public static ConstResearchNumber valueOf(JsonObject buffer) {
        return valueOf(buffer.get("value").getAsNumber());
    }

    public static ConstResearchNumber valueOf(Number n) {
        try {
            return cb.get(n, () -> new ConstResearchNumber(n));
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static ConstResearchNumber valueOf(PacketBuffer buffer) {
        return valueOf(buffer.readDouble());
    }

    public ConstResearchNumber(Number n) {
        super();
        this.n = n;

    }

    @Override
    public int getInt(ResearchData rd) {
        return n.intValue();
    }

    @Override
    public long getLong(ResearchData rd) {
        return n.longValue();
    }

    @Override
    public double getVal(ResearchData rd) {
        return n.doubleValue();
    }

    public JsonElement serialize() {
        return new JsonPrimitive(n);
    }

    public void write(PacketBuffer buffer) {
        buffer.writeDouble(n.doubleValue());
    }

}
