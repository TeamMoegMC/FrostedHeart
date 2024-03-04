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

package com.teammoeg.frostedheart.content.research.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.team.TeamDataHolder;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;

public class KillClue extends ListenerClue {
    EntityType<?> type;

    KillClue() {
        super();
    }


    public KillClue(EntityType<?> t, float contribution) {
        super("", "", "", contribution);
    }

    public KillClue(JsonObject jo) {
        super(jo);
        type = RegistryUtils.getEntity(new ResourceLocation(jo.get("entity").getAsString()));
    }

    public KillClue(PacketBuffer pb) {
        super(pb);
        type = pb.readRegistryIdUnsafe(ForgeRegistries.ENTITIES);
    }

    @Override
    public String getBrief() {
        return "Kill " + getDescriptionString();
    }

    @Override
    public ITextComponent getDescription() {
        ITextComponent itc = super.getDescription();
        if (itc != null || type == null) return itc;
        return type.getName();
    }

    @Override
    public String getId() {
        return "kill";
    }

    @Override
    public ITextComponent getName() {
        if (name != null && !name.isEmpty())
            return super.getName();
        return GuiUtils.translate("clue." + FHMain.MODID + ".kill");
    }

    @Override
    public void initListener(TeamDataHolder t) {
        ResearchListeners.getKillClues().add(this, t.getId());
    }

    public boolean isCompleted(TeamResearchData trd, LivingEntity e) {
        if (type != null && type.equals(e.getType())) {
            this.setCompleted(trd, true);
            return true;
        }
        return false;
    }

    @Override
    public void removeListener(TeamDataHolder t) {
        ResearchListeners.getKillClues().remove(this, t.getId());
    }


    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("entity", RegistryUtils.getRegistryName(type).toString());
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeRegistryIdUnsafe(ForgeRegistries.ENTITIES, type);
    }


}
