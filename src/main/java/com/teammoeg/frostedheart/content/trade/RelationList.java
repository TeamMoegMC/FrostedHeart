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

import net.minecraft.network.FriendlyByteBuf;

public class RelationList {
    public final int[] relations = new int[RelationModifier.values().length];
    Integer sum;

    public void copy(RelationList rel) {
        System.arraycopy(rel.relations, 0, relations, 0, relations.length);
        sum = rel.sum;
    }

    public int get(RelationModifier relation) {
        return relations[relation.ordinal()];
    }

    public void put(RelationModifier relation, int val) {
        relations[relation.ordinal()] = val;
        sum = null;
    }

    public void read(FriendlyByteBuf pb) {
        int[] arr = pb.readVarIntArray();
        int minl = Math.min(arr.length, relations.length);
        System.arraycopy(arr, 0, relations, 0, minl);
    }

    public int sum() {
        if (sum == null) {
            sum = 0;
            for (int relation : relations) {
                sum += relation;
            }
        }
        return sum;
    }

    public void write(FriendlyByteBuf pb) {
        pb.writeVarIntArray(relations);
    }
}
