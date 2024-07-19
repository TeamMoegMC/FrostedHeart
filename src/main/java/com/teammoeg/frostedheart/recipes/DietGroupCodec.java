/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.network.PacketBuffer;
import top.theillusivec4.diet.api.IDietGroup;
import top.theillusivec4.diet.common.group.DietGroups;

public class DietGroupCodec {
    private static List<IDietGroup> codecs = new ArrayList<>();
    public final static String[] groups = new String[]{"grains", "vegetables", "plant_oil", "proteins", "sugars", "vitamin"};

    public static void clearCodec() {
        codecs.clear();
    }

    public static void genCodec() {
        codecs.clear();
        Set<IDietGroup> idgs = DietGroups.get();
        outer:
        for (String group : groups) {
            for (IDietGroup idg : idgs)
                if (idg.getName().equalsIgnoreCase(group)) {
                    codecs.add(idg);
                    continue outer;
                }
            throw new IllegalArgumentException("Failed to load diet groups. "
                    + "Please delete the \"serverconfig\" folder in the corresponding world's save. "
                    + "If the problem persists, reinstall the modpack!");
        }
    }

    public static IDietGroup getGroup(int i) {
        if (codecs.isEmpty())
            genCodec();
        return codecs.get(i);
    }

    public static IDietGroup getGroup(String i) {
        if (codecs.isEmpty())
            genCodec();
        return codecs.get(getId(i));
    }

    public static int getId(IDietGroup idg) {
        if (codecs.isEmpty())
            genCodec();
        return codecs.indexOf(idg);
    }

    public static int getId(String idg) {
        for (int i = 0; i < groups.length; i++)
            if (idg.equals(groups[i]))
                return i;
        return -1;
    }

    public static Map<String, Float> read(PacketBuffer pb) {
        int size = pb.readVarInt();
        Map<String, Float> m = new HashMap<>();
        if (size > 0)
            for (int i = 0; i < size; i++) {
                m.put(groups[pb.readVarInt()], pb.readFloat());
            }
        return m;
    }

    public static void write(PacketBuffer pb, Map<String, Float> f) {
        pb.writeVarInt(f.size());
        if (!f.isEmpty())
            f.forEach((key, value) -> {
                pb.writeVarInt(getId(key));
                pb.writeFloat(value);
            });
    }

    private DietGroupCodec() {
    }
}
