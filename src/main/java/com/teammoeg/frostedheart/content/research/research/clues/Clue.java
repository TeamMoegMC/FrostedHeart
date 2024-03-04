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

import java.util.UUID;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.research.AutoIDItem;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.content.research.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.team.SpecialDataManager;
import com.teammoeg.frostedheart.team.SpecialDataTypes;
import com.teammoeg.frostedheart.team.TeamDataHolder;
import com.teammoeg.frostedheart.util.io.Writeable;

import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * "Clue" for researches, contributes completion percentage for some
 * researches.6
 * Clue can be trigger if any research is researchable(finished item commit)
 */
public abstract class Clue extends AutoIDItem implements Writeable{
    float contribution;// percentage, range (0,1]
    String name = "";
    String desc = "";
    String hint = "";
    String nonce;
    boolean showContribute;
    public Supplier<Research> parent;
    boolean required = false;

    public Clue() {
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    public Clue(JsonObject jo) {
        super();
        if (jo.has("name"))
            this.name = jo.get("name").getAsString();
        if (jo.has("desc"))
            this.desc = jo.get("desc").getAsString();
        if (jo.has("hint"))
            this.hint = jo.get("hint").getAsString();
        this.contribution = jo.get("value").getAsFloat();
        this.nonce = jo.get("id").getAsString();
        if (jo.has("required"))
            this.required = jo.get("required").getAsBoolean();
    }

    public Clue(PacketBuffer pb) {
        super();
        this.name = pb.readString();
        this.desc = pb.readString();
        this.hint = pb.readString();
        this.contribution = pb.readFloat();
        this.nonce = pb.readString();
        this.required = pb.readBoolean();
    }

    public Clue(String name, float contribution) {
        this(name, "", "", contribution);
    }

    public Clue(String name, String desc, String hint, float contribution) {
        super();
        this.contribution = contribution;
        this.name = name;
        this.desc = desc;
        this.hint = hint;
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    public void delete() {
        deleteSelf();
        if (parent != null) {
            Research r = parent.get();
            if (r != null) {
                r.getClues().remove(this);
            }
        }
    }

    private void deleteInTree() {
        SpecialDataManager.INSTANCE.getAllData().forEach(this::end);
    }

    public void deleteSelf() {
        deleteInTree();
        FHResearch.clues.remove(this);
    }

    public void edit() {
        deleteInTree();
    }

    /**
     * Stop detection when clue is completed
     */
    public abstract void end(TeamDataHolder team);

    /**
     * Get brief string describe this clue for show in editor.
     *
     * @return brief<br>
     */
    public abstract String getBrief();

    public String getBriefDesc() {
        return "   " + (this.required ? "required " : "") + "+" + (int) (this.contribution * 100) + "%";
    }

    public ITextComponent getDescription() {
        return FHTextUtil.getOptional(desc, "clue", () -> this.getLId() + ".desc");
    }

    public String getDescriptionString() {
        ITextComponent tc = getDescription();

        return tc != null ? tc.getString() : "";
    }

    public ITextComponent getHint() {
        return FHTextUtil.getOptional(hint, "clue", () -> this.getLId() + ".hint");
    }

    public abstract String getId();

    public ITextComponent getName() {
        return FHTextUtil.get(name, "clue", () -> this.getLId() + ".name");
    }

    @Override
    public String getNonce() {
        return nonce;
    }

    public float getResearchContribution() {
        return contribution;
    }

    @Override
    public final String getType() {
        return "clue";
    }

    /**
     * called when researches load finish
     */
    public abstract void init();

    @OnlyIn(Dist.CLIENT)
    public boolean isCompleted() {
        return ClientResearchDataAPI.getData().isClueTriggered(this);
    }


    public boolean isCompleted(TeamDataHolder team) {
        return team.getData(SpecialDataTypes.RESEARCH_DATA).isClueTriggered(this);
    }

    public boolean isCompleted(TeamResearchData data) {
        return data.isClueTriggered(this);
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * send progress packet to client
     * should not called manually
     */
    public void sendProgressPacket(TeamDataHolder team) {
        FHClueProgressSyncPacket packet = new FHClueProgressSyncPacket(team, this);
        team.sendToOnline(packet);
    }

	public JsonObject serialize() {
        JsonObject jo = new JsonObject();
        if (!name.isEmpty())
            jo.addProperty("name", name);
        if (!desc.isEmpty())
            jo.addProperty("desc", desc);
        if (!hint.isEmpty())
            jo.addProperty("hint", hint);
        jo.addProperty("value", contribution);
        jo.addProperty("id", nonce);
        if (required)
            jo.addProperty("required", required);
        return jo;
    }

    @OnlyIn(Dist.CLIENT)
    public void setCompleted(boolean trig) {
    	ClientResearchDataAPI.getData().setClueTriggered(this, trig);
    }

    public void setCompleted(TeamDataHolder team, boolean trig) {
    	team.getData(SpecialDataTypes.RESEARCH_DATA).setClueTriggered(this, trig);
        if (trig)
            end(team);
        else
            start(team);
        this.sendProgressPacket(team);
    }

    public void setCompleted(TeamResearchData trd, boolean trig) {
        trd.setClueTriggered(this, trig);

        if (trig)
            end(trd.getHolder());
        else
            start(trd.getHolder());
        this.sendProgressPacket(trd.getHolder());
    }

    void setNewId(String id) {
        if (!id.equals(this.nonce)) {
            delete();
            this.nonce = id;
            FHResearch.clues.register(this);
            if (parent != null) {
                Research r = parent.get();
                if (r != null) {
                    r.attachClue(this);
                    r.doIndex();
                }
            }
        }
    }

    /**
     * called when this clue's research has started
     */
    public abstract void start(TeamDataHolder team);

    ;

    public void write(PacketBuffer buffer) {
        buffer.writeString(name);
        buffer.writeString(desc);
        buffer.writeString(hint);
        buffer.writeFloat(contribution);
        buffer.writeString(nonce);
        buffer.writeBoolean(required);
    }
}
