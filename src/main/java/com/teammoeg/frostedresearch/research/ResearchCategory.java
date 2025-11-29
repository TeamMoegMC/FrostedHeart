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

package com.teammoeg.frostedresearch.research;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.codec.CompressDifferCodec;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FRMain;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public enum ResearchCategory {

    RESCUE("rescue"),
    LIVING("living"),
    PRODUCTION("production"),
    ARS("ars"),
    EXPLORATION("exploration");

    public static final Map<ResourceLocation, ResearchCategory> ALL = new LinkedHashMap<>();
    public static final Codec<ResearchCategory> CODEC = new CompressDifferCodec<>(ResourceLocation.CODEC.xmap(
            o -> ALL.get(o),
            o -> o.getId()),
            Codec.BYTE.xmap(i -> ResearchCategory.values()[i], i -> (byte) i.ordinal()));

    static {
        for (ResearchCategory rc : ResearchCategory.values()) {
            ResearchCategory.ALL.put(rc.id, rc);
            ResearchCategory.ALL.put(new ResourceLocation("frostedheart",rc.id.getPath()), rc);
        }
    }

    private final ResourceLocation id;
    private final Component name;
    private final Component desc;

    private final ResourceLocation icon;

    ResearchCategory(String id) {
        this.id = FRMain.rl(id);
        this.name = Lang.translateResearchCategoryName(id);
        this.desc = Lang.translateResearchCategoryDesc(id);
        this.icon = FRMain.rl("textures/gui/research/category/" + id + ".png");
        //FHMain.rl("textures/gui/research/category/background/" + id + ".png");

    }

    public Component getDesc() {
        return desc;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Component getName() {
        return name;
    }

}
