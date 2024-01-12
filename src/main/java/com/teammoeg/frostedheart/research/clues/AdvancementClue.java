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

package com.teammoeg.frostedheart.research.clues;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.research.data.TeamResearchData;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.multiplayer.ClientAdvancementManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class AdvancementClue extends TickListenerClue {
    ResourceLocation advancement = new ResourceLocation("minecraft:story/root");
    String criterion = "";

    public AdvancementClue() {
        super();
    }

    public AdvancementClue(JsonObject jo) {
        super(jo);
        advancement = new ResourceLocation(jo.get("advancement").getAsString());
        if (jo.has("criterion"))
            criterion = jo.get("criterion").getAsString();
    }

    public AdvancementClue(PacketBuffer pb) {
        super(pb);
        advancement = pb.readResourceLocation();
        criterion = pb.readString();
    }

    public AdvancementClue(String name, float contribution) {
        super(name, contribution);
    }

    public AdvancementClue(String name, String desc, String hint, float contribution) {
        super(name, desc, hint, contribution);
    }

    @Override
    public String getBrief() {
        return "Advancement " + getDescriptionString();
    }

    @Override
    public ITextComponent getDescription() {
        ITextComponent itc = super.getDescription();
        if (itc != null) return itc;
        ClientAdvancementManager cam = ClientUtils.getPlayer().connection.getAdvancementManager();
        Advancement adv = cam.getAdvancementList().getAdvancement(advancement);
        if (adv != null)
            return adv.getDisplayText();
        else
            return null;

    }

    @Override
    public String getId() {
        return "advancement";
    }

    @Override
    public ITextComponent getName() {
        if (name != null && !name.isEmpty())
            return super.getName();
        return GuiUtils.translate("clue." + FHMain.MODID + ".advancement");
    }

    @Override
    public boolean isCompleted(TeamResearchData t, ServerPlayerEntity player) {
        Advancement a = player.server.getAdvancementManager().getAdvancement(advancement);
        if (a == null) {
            return false;
        }

        AdvancementProgress progress = player.getAdvancements().getProgress(a);

        if (criterion.isEmpty()) {
            return progress.isDone();
        }
        CriterionProgress criterionProgress = progress.getCriterionProgress(criterion);
        return criterionProgress != null && criterionProgress.isObtained();
    }

    @Override
    public JsonObject serialize() {
        JsonObject jo = super.serialize();
        jo.addProperty("advancement", advancement.toString());
        if (!criterion.isEmpty())
            jo.addProperty("criterion", criterion);
        return jo;
    }

    @Override
    public void write(PacketBuffer buffer) {
        super.write(buffer);
        buffer.writeResourceLocation(advancement);
        buffer.writeString(criterion);
    }


}
