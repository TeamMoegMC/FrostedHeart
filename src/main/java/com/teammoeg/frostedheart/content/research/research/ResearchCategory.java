/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.research.research;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.io.codec.CompressDifferCodec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public enum ResearchCategory {

    RESCUE("rescue"),
    LIVING("living"),
    PRODUCTION("production"),
    ARS("ars"),
    EXPLORATION("exploration");
	
    public static final Map<ResourceLocation, ResearchCategory> ALL = new HashMap<>();
    static {
        for (ResearchCategory rc : ResearchCategory.values())
            ResearchCategory.ALL.put(rc.id, rc);
    }
    public static final Codec<ResearchCategory> CODEC=new CompressDifferCodec<>(ResourceLocation.CODEC.xmap(
    		o->ALL.get(o),
    		o->o.getId()),
    	Codec.BYTE.xmap(i->ResearchCategory.values()[i], i->(byte)i.ordinal()));

    private final ResourceLocation id;
    private final Component name;
    private final Component desc;

    private final ResourceLocation icon;

    ResearchCategory(String id) {
        this.id = FHMain.rl(id);
        this.name = Lang.translateResearchCategoryName(id);
        this.desc = Lang.translateResearchCategoryDesc(id);
        this.icon = FHMain.rl("textures/gui/research/category/" + id + ".png");
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
