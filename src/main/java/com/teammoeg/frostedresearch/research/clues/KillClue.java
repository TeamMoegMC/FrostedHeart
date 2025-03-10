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

package com.teammoeg.frostedresearch.research.clues;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.util.Lang;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.ResearchListeners;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.research.Research;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class KillClue extends ListenerClue {
    public static final MapCodec<KillClue> CODEC = RecordCodecBuilder.mapCodec(t -> t.group(
            ListenerClue.BASE_CODEC.forGetter(o -> o.getData()),
            ForgeRegistries.ENTITY_TYPES.getCodec().fieldOf("entity").forGetter(o -> o.type)
    ).apply(t, KillClue::new));
    EntityType<?> type;

    KillClue() {
        super();
    }



    public KillClue(String nonce, String name, String desc, String hint, float contribution, boolean required, boolean alwaysOn, EntityType<?> type) {
		super(nonce, name, desc, hint, contribution, required, alwaysOn);
		this.type = type;
	}



	public KillClue(BaseData data, EntityType<?> type) {
        super(data);
        this.type = type;
    }

    @Override
    public String getBrief() {
    	String entityDesc="none";
    	if(type!=null)
    		entityDesc=type.getDescription().getString();
        return "Kill " + entityDesc;
    }

    @Override
    public Component getDescription(Research parent) {
        Component itc = super.getDescription(parent);
        if (itc != null || type == null) return itc;
        return type.getDescription();
    }


    @Override
    public Component getName(Research parent) {
        if (name != null && !name.isEmpty())
            return super.getName(parent);
        return Lang.translateKey("clue." + FRMain.MODID + ".kill");
    }

    @Override
    public void initListener(TeamDataHolder t, Research parent) {
        ResearchListeners.getKillClues().add(super.getClueClosure(parent), t.getId());
    }

    public boolean isCompleted(TeamResearchData trd, LivingEntity e) {
        if (type != null && type.equals(e.getType())) {
            return true;
        }
        return false;
    }

    @Override
    public void removeListener(TeamDataHolder t, Research parent) {
        ResearchListeners.getKillClues().remove(super.getClueClosure(parent), t.getId());
    }

}
