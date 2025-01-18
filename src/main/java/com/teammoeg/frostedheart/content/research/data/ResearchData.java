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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.chorda.util.evaluator.IEnvironment;
import com.teammoeg.chorda.util.io.CodecUtil;
import lombok.Getter;

public class ResearchData implements IEnvironment {

    public static final ResearchData EMPTY = new ResearchData() {

        @Override
        public boolean canResearch() {
            return false;
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
        public boolean isCompleted() {
            return false;
        }


        @Override
        public void setActive() {
        }

        @Override
        public void setLevel(int level) {
        }

		@Override
		public void reset() {
		}

		@Override
		public boolean canComplete(Research rs) {
			return false;
		}

		@Override
		public float getProgress(Research r) {
			return 0;
		}

		@Override
		public long getTotalCommitted(Research r) {
			return 0;
		}

		@Override
		public void setFinished(boolean finished) {
		}

		@Override
		public Double getOptional(String key) {
			return null;
		}

		@Override
		public void set(String key, double v) {
		}

		@Override
		public long commitPoints(Research r, long pts, Runnable onSuccess) {
			return pts;
		}

		@Override
		public double get(String key) {
			return 0;
		}

		@Override
		public void setClueTriggered(String id, boolean trig) {
		}

		@Override
		public void setClueTriggered(Clue c, boolean trig) {
		}

		@Override
		public boolean isClueTriggered(Clue clue) {
			return false;
		}

		@Override
		public boolean isClueTriggered(String id) {
			return false;
		}

		@Override
		public void setEffectGranted(String id, boolean trig) {
		}

		@Override
		public void setEffectGranted(Effect e, boolean trig) {
		}

		@Override
		public boolean isEffectGranted(Effect effect) {
			return false;
		}

		@Override
		public boolean isEffectGranted(String id) {
			return false;
		}

    };
    public static record ResearchDataPacket(boolean active,boolean finished,int level,int committed,List<ClueData> clueData,BitSet effectData) {
        public ResearchDataPacket(int committed, boolean[] flags, int level, List<ClueData> clueData,byte[] effectData) {
    		this(flags[0],flags[1],level,committed,clueData,BitSet.valueOf(effectData));
    	}
    }
	@Getter
    boolean active;// is all items fulfilled?
	@Getter
    boolean finished;
    int level;
    private int committed;// points committed
	@Getter
    private Map<String, ClueData> clueData = new HashMap<>();
	@Getter
    private Map<String, Boolean> effectData = new HashMap<>();


	public List<String> getFieldNames() {
		return List.of("active","finished","level","committed","clueData","effectData");
	}

	public Object getField(String key) {
        return switch (key) {
            case "active" -> active;
            case "finished" -> finished;
            case "level" -> level;
            case "committed" -> committed;
            case "clueData" -> clueData;
            case "effectData" -> effectData;
            default -> "no such field";
        };
    }

	@Override
	public String toString() {
		return "ResearchData [active=" + active + ", finished=" + finished + ", level=" + level + ", committed=" + committed
				+ ", clueData=" + clueData + ", effectData=" + effectData + "]";
	}

	public static final Codec<ResearchData> CODEC=RecordCodecBuilder.create(t->t.group(
    	Codec.INT.fieldOf("committed").forGetter(o->o.committed),
    	CodecUtil.<ResearchData>booleans("flags")
    	.flag("active", o->o.active)
    	.flag("finished", o->o.finished).build(),
    	CodecUtil.defaultValue(Codec.INT, 0).fieldOf("level").forGetter(o->o.level),
    	CodecUtil.mapCodec("id", Codec.STRING, "data", ClueData.CODEC).fieldOf("clueData").forGetter(o->o.clueData),
    	CodecUtil.mapCodec("id", Codec.STRING, "data", Codec.BOOL).fieldOf("effectData").forGetter(o->o.effectData)
    	).apply(t, ResearchData::new));
    public static final Codec<ResearchDataPacket> NETWORK_CODEC=RecordCodecBuilder.create(t->t.group(
    	Codec.INT.fieldOf("committed").forGetter(o->o.committed()),
    	CodecUtil.<ResearchDataPacket>booleans("flags")
    	.flag("active", o->o.active())
    	.flag("finished", o->o.finished()).build(),
    	CodecUtil.defaultValue(Codec.INT, 0).fieldOf("level").forGetter(o->o.level()),
    	CodecUtil.discreteList(ClueData.CODEC).fieldOf("clueData").forGetter(o->o.clueData()),
    	CodecUtil.BYTE_ARRAY_CODEC.fieldOf("effectData").forGetter(o->o.effectData().toByteArray())
    	).apply(t, ResearchDataPacket::new));
    public ResearchData(int committed, boolean[] flags, int level, Map<String, ClueData> clueData,Map<String, Boolean> effectData) {
		super();
		this.active = flags[0];
		this.finished = flags[1];
		this.level = level;
		this.committed = committed;
		this.clueData.putAll(clueData);
		this.effectData.putAll(effectData);
	}

    public ResearchDataPacket write(Research r) {
    	List<ClueData> clueData=new ArrayList<>(r.getClues().size());
    	BitSet effectData=new BitSet(r.getEffects().size());
    	for(Clue c:r.getClues())
    		clueData.add(this.clueData.get(c.getNonce()));
    	int i=0;
    	for(Effect e:r.getEffects()) {
    		effectData.set(i++,this.effectData.getOrDefault(e.getNonce(), false));
    	}
    	return new ResearchDataPacket(active,finished,level,committed,clueData,effectData);
    }
    public void read(Research r,ResearchDataPacket packet) {
    	active=packet.active();
    	finished=packet.finished();
    	level=packet.level();
    	committed=packet.committed();
    	int i=0;
    	for(Clue c:r.getClues()) {
    		if(i>=packet.clueData().size())break;
    		this.clueData.put(c.getNonce(), packet.clueData().get(i++));
    	}
    	i=0;
    	for(Effect e:r.getEffects()) {
    		if(i>=packet.effectData().size())break;
    		this.effectData.put(e.getNonce(), packet.effectData().get(i++));
    	}
    }
    public ResearchData() {
    	
    }
    public void reset() {
    	active=false;
    	finished=false;
    	committed=0;
    	clueData.clear();
    	effectData.clear();
    }
    public boolean canComplete(Research rs) {
        for (Clue cl : rs.getClues()) {
            if (cl.isRequired() && !clueData.get(cl.getNonce()).completed) {
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
    public float getProgress(Research r) {
        return getTotalCommitted(r) * 1f /r.getRequiredPoints();
    }

    public long getTotalCommitted(Research r) {
        long currentProgress = committed;
        float contribution = 0;
        for (Clue ac : r.getClues())
            if (this.isClueTriggered(ac))
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





    public void setActive() {
        if (active)
            return;
        active = true;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (!finished){
        	clueData.clear();
        }
    }

    public void setLevel(int level) {
        this.level = level;
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
    public long commitPoints(Research r,long pts,Runnable onSuccess) {
        long tocommit = Math.min(pts, r.getRequiredPoints() - committed);
        if (tocommit > 0) {
            committed += tocommit;
            if(onSuccess!=null)
            	onSuccess.run();
            return pts - tocommit;
        }
        return pts;
    }
	@Override
	public double get(String key) {
		if (key.equals("level"))
            return level;
		return 0;
	}

	public void setClueTriggered(String id, boolean trig) {
		clueData.computeIfAbsent(id, s->new ClueData()).completed=trig;
	}
	public void setClueTriggered(Clue c, boolean trig) {
		setClueTriggered(c.getNonce(),trig);
	}
	public boolean isClueTriggered(Clue clue) {
		return isClueTriggered(clue.getNonce());
	}
	public boolean isClueTriggered(String id) {
		ClueData data=clueData.get(id);
		if(data==null)return false;
		return data.completed;
	}
	public void setEffectGranted(String id, boolean trig) {
		effectData.put(id, trig);
	}
	public void setEffectGranted(Effect e, boolean trig) {
		setEffectGranted(e.getNonce(),trig);
	}
	public boolean isEffectGranted(Effect effect) {
		return isEffectGranted(effect.getNonce());
	}
	public boolean isEffectGranted(String id) {
		return effectData.getOrDefault(id, false);
	}
}
