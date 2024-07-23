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

package com.teammoeg.frostedheart.content.research.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.research.events.ResearchStatusEvent;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.clues.ClueDatas;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.evaluator.IEnvironment;
import com.teammoeg.frostedheart.util.io.CodecUtil;
import com.teammoeg.frostedheart.util.io.SerializeUtil;
import com.teammoeg.frostedheart.util.io.codec.BooleansCodec;
import com.teammoeg.frostedheart.util.utility.OptionalLazy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.nbt.Tag;

public class ResearchData implements IEnvironment {

    public static final ResearchData EMPTY = new ResearchData(null, null) {

        @Override
        public void announceCompletion() {
        }

        @Override
        public boolean canComplete() {
            return false;
        }

        @Override
        public boolean canResearch() {
            return false;
        }

        @Override
        public void checkComplete() {
        }

        @Override
        public boolean commitItem(ServerPlayer player) {
            return false;
        }

        @Override
        public long commitPoints(long pts) {
            return pts;
        }

        @Override
        public void deserialize(CompoundTag cn) {
            super.deserialize(cn);
        }

        @Override
        public long getCommitted() {
            return 0;
        }

        @Override
        public int getLevel() {
            return 0;
        }

        @Override
        public float getProgress() {
            return 0;
        }

        @Override
        public Research getResearch() {
            return null;
        }

        @Override
        public long getTotalCommitted() {
            return 0;
        }

        @Override
        public boolean isCompleted() {
            return false;
        }

        @Override
        public boolean isInProgress() {
            return false;
        }

        @Override
        public boolean isUnlocked() {
            return false;
        }

        @Override
        public void read(FriendlyByteBuf pb) {
            super.read(pb);
        }

        @Override
        public void sendProgressPacket() {
        }

        @Override
        public CompoundTag serialize() {
            return super.serialize();
        }

        @Override
        public void setActive() {
        }

        @Override
        public void setFinished(boolean finished) {
        }

        @Override
        public void setLevel(int level) {
        }

        @Override
        public void write(FriendlyByteBuf pb) {
            super.write(pb);
        }

    };
    boolean active;// is all items fulfilled?
    boolean finished;
    private Supplier<Research> rs;
    int level;
    private int committed;// points committed
    TeamResearchData parent;

    private Map<Integer, IClueData> data = new HashMap<>();
    public static final Codec<ResearchData> CODEC=RecordCodecBuilder.create(t->t.group(
    	Codec.INT.fieldOf("committed").forGetter(o->o.committed),
    	CodecUtil.<ResearchData>booleans("flags")
    	.flag("active", o->o.active)
    	.flag("finished", o->o.finished).build(),
    	CodecUtil.defaultValue(Codec.INT, 0).fieldOf("level").forGetter(o->o.level),
    	CodecUtil.mapCodec("id", Codec.INT, "data", ClueDatas.CODEC).fieldOf("clues").forGetter(o->o.data)
    	).apply(t, ResearchData::new));
    
    public ResearchData(int committed, boolean[] flags, int level, Map<Integer, IClueData> data) {
		super();
		this.active = flags[0];
		this.finished = flags[1];
		this.level = level;
		this.committed = committed;
		this.data.putAll(data);
	}
    public void setParent(Supplier<Research> r, TeamResearchData parent) {
        this.rs = r;
        this.parent = parent;
    }
    public ResearchData(Supplier<Research> r, CompoundTag nc, TeamResearchData parent) {
        this(r, parent);
        deserialize(nc);
    }
    
    public ResearchData(Supplier<Research> r, TeamResearchData parent) {
        this.rs = r;
        this.parent = parent;
    }

    public void announceCompletion() {
        sendProgressPacket();

        parent.getHolder().getTeam()
                .ifPresent(e -> MinecraftForge.EVENT_BUS.post(new ResearchStatusEvent(getResearch(), e, finished)));
    }

    public boolean canComplete() {
        for (Clue cl : getResearch().getClues()) {
            if (cl.isRequired() && !cl.isCompleted(parent)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return whether all required items are committed and is ready to do research
     * through clues
     */
    public boolean canResearch() {
        return active;
    }

    public void checkComplete() {
        if (finished)
            return;
        Research r = getResearch();
        boolean flag = true;
        for (Clue cl : r.getClues()) {
            if (cl.isRequired() && !cl.isCompleted(parent)) {
                flag = false;
                break;
            }
        }
        if (getTotalCommitted() >= r.getRequiredPoints() && flag) {
            setFinished(true);
            this.announceCompletion();

        }
    }

    public boolean commitItem(ServerPlayer player) {
        Research research = getResearch();
        if (research.isInCompletable()) return false;
        for (Research par : research.getParents()) {
            if (!parent.getData(par).isCompleted()) {
                return false;
            }
        }
        if(!research.getRequiredItems().isEmpty()&&!FHUtils.costItems(player,this.getResearch().getRequiredItems()))
        	return false;
        setActive();
        return true;
    }

    public long commitPoints(long pts) {
        if (!active || finished)
            return pts;
        long tocommit = Math.min(pts, getResearch().getRequiredPoints() - committed);
        if (tocommit > 0) {
            committed += tocommit;
            checkComplete();
            return pts - tocommit;
        }
        return pts;
    }

    public void deserialize(CompoundTag cn) {
        committed = cn.getInt("committed");
        active = cn.getBoolean("active");
        finished = cn.getBoolean("finished");
        if (cn.contains("level"))
            level = cn.getInt("level");
        data.clear();
        cn.getList("clues", Tag.TAG_COMPOUND).stream().map(t -> (CompoundTag) t).forEach(e -> data.put(e.getInt("id"), ClueDatas.read(e.getCompound("data"))));
        // rs=FHResearch.getResearch(cn.getInt("research"));
    }



    /**
     * @return Research points committed
     */
    public long getCommitted() {
        return committed;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Current research progress
     *
     * @return 0.0F-1.0F fraction
     */
    public float getProgress() {
        return getTotalCommitted() * 1f / getResearch().getRequiredPoints();
    }

    /**
     * @return Research associated with this data
     */
    public Research getResearch() {
        return rs.get();
    }

    public long getTotalCommitted() {
        Research r = getResearch();
        long currentProgress = committed;
        float contribution = 0;
        for (Clue ac : r.getClues())
            if (ac.isCompleted(parent))
                contribution += ac.getResearchContribution();
        if (contribution >= 0.999)
            return r.getRequiredPoints();
        currentProgress += (long) (contribution * r.getRequiredPoints());
        return Math.min(currentProgress, r.getRequiredPoints());
    }

    /**
     * @return research finished
     */
    public boolean isCompleted() {
        return finished;
    }

    public boolean isInProgress() {
        OptionalLazy<Research> r = parent.getCurrentResearch();
        if (r.isPresent()) {
            return r.resolve().get().equals(getResearch());
        }
        return false;
    }

    public boolean isUnlocked() {
        Research research = getResearch();
        for (Research par : research.getParents()) {
            if (!parent.getData(par).isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public void read(FriendlyByteBuf pb) {
        committed = pb.readVarInt();
        boolean[] bs = SerializeUtil.readBooleans(pb);
        active = bs[0];
        finished = bs[1];
    }

    public void sendProgressPacket() {
        getResearch().sendProgressPacket(parent.getHolder(), this);
    }

    public CompoundTag serialize() {
        CompoundTag cnbt = new CompoundTag();
        cnbt.putLong("committed", committed);
        cnbt.putBoolean("active", active);
        cnbt.putBoolean("finished", finished);
        if (level > 0)
            cnbt.putInt("level", level);
        cnbt.put("clues", SerializeUtil.toNBTList(data.entrySet(), (t,c) -> c.compound().putInt("id", t.getKey()).put("data", ClueDatas.write(t.getValue()))));
        // cnbt.putInt("research",getResearch().getRId());
        return cnbt;

    }

    public void setActive() {
        if (active)
            return;
        active = true;
        sendProgressPacket();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            data.clear();
            Research r = rs.get();
            parent.clearCurrentResearch(r);
            r.grantEffects(parent, null);

        }
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void write(FriendlyByteBuf pb) {
        pb.writeVarInt(committed);
        SerializeUtil.writeBooleans(pb, active, finished);
    }

	@Override
	public Double getOptional(String key) {
        if (key.equals("level"))
            return (double) level;
        return null;
	}

	@Override
	public void set(String key, double v) {
		if (key.equals("level"))
			this.level=(int) v;
	}

	@Override
	public double get(String key) {
		if (key.equals("level"))
            return level;
		return 0;
	}


}
