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

import net.minecraft.network.PacketBuffer;

public class RelationList {
    public final int[] relations = new int[RelationModifier.values().length];
    Integer sum;

    public void copy(RelationList rel) {
        for (int i = 0; i < relations.length; i++) {
            relations[i] = rel.relations[i];
        }
        sum = rel.sum;
    }

    public int get(RelationModifier relation) {
        return relations[relation.ordinal()];
    }

    public void put(RelationModifier relation, int val) {
        relations[relation.ordinal()] = val;
        sum = null;
    }

    public void read(PacketBuffer pb) {
        int[] arr = pb.readVarIntArray();
        int minl = Math.min(arr.length, relations.length);
        for (int i = 0; i < minl; i++) {
            relations[i] = arr[i];
        }
    }

    public int sum() {
        if (sum == null) {
            sum = 0;
            for (int i = 0; i < relations.length; i++) {
                sum += relations[i];
            }
        }
        return sum;
    }

    public void write(PacketBuffer pb) {
        pb.writeVarIntArray(relations);
    }
}
