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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.AutoIDItem;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.content.research.network.FHClueProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.io.registry.TypedCodecRegistry;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * "Clue" for researches, contributes completion percentage for some
 * researches.6
 * Clue can be trigger if any research is researchable(finished item commit)
 */
public abstract class Clue extends AutoIDItem{
	public static class BaseData{
	    float contribution;// percentage, range (0,1]
	    String name = "";
	    String desc = "";
	    String hint = "";
	    String nonce;
	    boolean required = false;
		public BaseData(String name, String desc, String hint, String nonce, boolean required, float contribution) {
			super();
			this.name = name;
			this.desc = desc;
			this.hint = hint;
			this.nonce = nonce;
			this.required = required;
			this.contribution = contribution;
		}
	}
	public static final MapCodec<BaseData> BASE_CODEC=RecordCodecBuilder.mapCodec(t->
	t.group(
		SerializeUtil.nullableCodecValue(Codec.STRING,"").fieldOf("name").forGetter(o->o.name),
		SerializeUtil.nullableCodecValue(Codec.STRING,"").fieldOf("desc").forGetter(o->o.desc),
		SerializeUtil.nullableCodecValue(Codec.STRING,"").fieldOf("hint").forGetter(o->o.hint),
		Codec.STRING.fieldOf("id").forGetter(o->o.nonce),
		Codec.BOOL.fieldOf("required").forGetter(o->o.required),
		Codec.FLOAT.fieldOf("value").forGetter(o->o.contribution)).apply(t, BaseData::new));
    private static TypedCodecRegistry<Clue> registry = new TypedCodecRegistry<>();

    static {
        register(CustomClue.class, "custom", CustomClue.CODEC);
        register(AdvancementClue.class, "advancement", AdvancementClue.CODEC);
        register(ItemClue.class, "item", ItemClue.CODEC);
        register(KillClue.class, "kill", KillClue.CODEC);
        register(MinigameClue.class, "game", MinigameClue.CODEC);
    }
    public static <T extends Clue> void register(Class<T> cls, String id, Codec<T> j) {
        registry.register(cls, id, j);
    }
    public final static Codec<Clue> CODEC=registry.codec();
    float contribution;// percentage, range (0,1]
    String name = "";
    String desc = "";
    String hint = "";
    String nonce;
    public Supplier<Research> parent;
    boolean required = false;

    public Clue() {
        this.nonce = Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }
    public Clue(BaseData data) {
    	this.name=data.name;
    	this.desc=data.desc;
    	this.hint=data.hint;
    	this.nonce=data.nonce;
    	this.required=data.required;
    	this.contribution=data.contribution;
    }
    public BaseData getData() {
    	return new BaseData(name, desc, hint, nonce, required, contribution);
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
        FHTeamDataManager.INSTANCE.getAllData().forEach(this::end);
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
        return FHTextUtil.getOptional(desc, "clue", () -> this.getId() + ".desc");
    }

    public String getDescriptionString() {
        ITextComponent tc = getDescription();

        return tc != null ? tc.getString() : "";
    }

    public ITextComponent getHint() {
        return FHTextUtil.getOptional(hint, "clue", () -> this.getId() + ".hint");
    }

    public abstract String getId();

    public ITextComponent getName() {
        return FHTextUtil.get(name, "clue", () -> this.getId() + ".name");
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
     * should not call manually
     */
    public void sendProgressPacket(TeamDataHolder team) {
        FHClueProgressSyncPacket packet = new FHClueProgressSyncPacket(team, this);
        team.sendToOnline(packet);
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

}
