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
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.base.team.SpecialDataTypes;
import com.teammoeg.frostedheart.base.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.research.AutoIDItem;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.gui.FHTextUtil;
import com.teammoeg.frostedheart.content.research.network.FHS2CClueProgressSyncPacket;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.registry.TypedCodecRegistry;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * "Clue" for researches, contributes completion percentage for some
 * researches.6
 * Clue can be trigger if any research is researchable(finished item commit)
 */
public abstract class Clue{
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
		CodecUtil.defaultValue(Codec.STRING,"").fieldOf("name").forGetter(o->o.name),
		CodecUtil.defaultValue(Codec.STRING,"").fieldOf("desc").forGetter(o->o.desc),
		CodecUtil.defaultValue(Codec.STRING,"").fieldOf("hint").forGetter(o->o.hint),
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
    /**
     * Stop detection when clue is completed
     */
    public abstract void end(TeamDataHolder team,Research parent);

    /**
     * Get brief string describe this clue for show in editor.
     *
     * @return brief<br>
     */
    public abstract String getBrief(Research parent);

    public String getBriefDesc() {
        return "   " + (this.required ? "required " : "") + "+" + (int) (this.contribution * 100) + "%";
    }

	public Component getDescription(Research parent) {
		if(parent==null||parent.getId()==null)
			return null;
        return FHTextUtil.getOptional(desc, "clue", () -> parent.getId()+".clue."+ this.getNonce() + ".desc");
    }

    public String getDescriptionString(Research parent) {
		if(parent==null||parent.getId()==null)
			return null;
        Component tc = getDescription(parent);

        return tc != null ? tc.getString() : "";
    }

    public Component getHint(Research parent) {
		if(parent==null||parent.getId()==null)
			return null;
        return FHTextUtil.getOptional(hint, "clue", () -> parent.getId()+".clue."+ this.getNonce() + ".hint");
    }

    public Component getName(Research parent) {
		if(parent==null||parent.getId()==null)
			return null;
        return FHTextUtil.get(name, "clue", () -> parent.getId()+".clue."+ this.getNonce() + ".name");
    }

    public String getNonce() {
        return nonce;
    }

    public float getResearchContribution() {
        return contribution;
    }


    /**
     * called when researches load finish
     */
    public abstract void init(Research parent);

    public boolean isRequired() {
        return required;
    }
    ClueClosure cache;
    public final ClueClosure getClueClosure(Research parent) {
    	if(cache==null||cache.research()!=parent)
    		cache=new ClueClosure(parent,this);
    	return cache;
    }

    /**
     * called when this clue's research has started
     */
    public abstract void start(TeamDataHolder team,Research parent);
	public void setNonce(String text) {
		this.nonce=text;
		
	}

}
