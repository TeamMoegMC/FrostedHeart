package com.teammoeg.frostedheart.research.data;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;

import com.teammoeg.frostedheart.research.Research;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.events.ResearchStatusEvent;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.LazyOptional;
import com.teammoeg.frostedheart.util.SerializeUtil;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ResearchData {
    boolean active;// is all items fulfilled?
    boolean finished;
    private Supplier<Research> rs;
    int level;
    private long committed;// points committed
    final TeamResearchData parent;

    public ResearchData(Supplier<Research> r, TeamResearchData parent) {
        this.rs = r;
        this.parent = parent;
    }

    /**
     * @return Research points committed
     */
    public long getCommitted() {
        return committed;
    }

    public long getTotalCommitted() {
        Research r = getResearch();
        long currentProgress = committed;
        float contribution = 0;
        for (Clue ac : r.getClues())
            if (ac.isCompleted(parent))
                contribution += ac.getResearchContribution();
        if(contribution>=0.98)
        	return r.getRequiredPoints();
        currentProgress += contribution * r.getRequiredPoints();
        return currentProgress;
    }

    public long commitPoints(long pts) {
        if (!active)
            return pts;
        long tocommit = Math.min(pts, getResearch().getRequiredPoints() - committed);
        committed += tocommit;
        checkComplete();
        return pts - tocommit;
    }

    public void checkComplete() {
        if (finished)
            return;
        Research r = getResearch();
        if (getTotalCommitted() >= r.getRequiredPoints()) {
            setFinished(true);
            this.announceCompletion();

        }
    }

    public void announceCompletion() {
        sendProgressPacket();

        parent.getTeam()
                .ifPresent(e -> MinecraftForge.EVENT_BUS.post(new ResearchStatusEvent(getResearch(), e, finished)));
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            Research r = rs.get();
            parent.clearCurrentResearch(r);
            r.grantEffects(parent, null);
            
        }
    }

    public void sendProgressPacket() {
        parent.getTeam().ifPresent(t -> getResearch().sendProgressPacket(t, this));
    }

    public ResearchData(Supplier<Research> r, CompoundNBT nc, TeamResearchData parent) {
        this(r, parent);
        deserialize(nc);
    }

    /**
     * @return Research associated with this data
     */
    public Research getResearch() {
        return rs.get();
    }

    public void write(PacketBuffer pb) {
        pb.writeVarLong(committed);
        SerializeUtil.writeBooleans(pb, active,finished);
    }

    public void read(PacketBuffer pb) {
        committed = pb.readVarLong();
        boolean[] bs=SerializeUtil.readBooleans(pb);
        active = bs[0];
        finished = bs[1];
    }

    public CompoundNBT serialize() {
        CompoundNBT cnbt = new CompoundNBT();
        cnbt.putLong("committed", committed);
        cnbt.putBoolean("active", active);
        cnbt.putBoolean("finished", finished);
        if(level>0)
        	cnbt.putInt("level", level);
        // cnbt.putInt("research",getResearch().getRId());
        return cnbt;

    }

    public void deserialize(CompoundNBT cn) {
        committed = cn.getLong("committed");
        active = cn.getBoolean("active");
        finished = cn.getBoolean("finished");
        if(cn.contains("level"))
        	level=cn.getInt("level");
        // rs=FHResearch.getResearch(cn.getInt("research"));
    }

    /**
     * @return research finished
     */
    public boolean isCompleted() {
        return finished;
    }

    public boolean isInProgress() {
        LazyOptional<Research> r = parent.getCurrentResearch();
        if (r.isPresent()) {
            return r.resolve().get().equals(getResearch());
        }
        return false;
    }

    /**
     * @return whether all required items are committed and is ready to do research
     * through clues
     */
    public boolean canResearch() {
        return active;
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

    public boolean commitItem(ServerPlayerEntity player) {
        Research research = getResearch();
        if(research.isInCompletable())return false;
        for (Research par : research.getParents()) {
            if (!parent.getData(par).isCompleted()) {
                return false;
            }
        }
        if (research.getRequiredItems().isEmpty()) {
            setActive();
            return true;
        }
        // first do simple verify
        for (IngredientWithSize iws : research.getRequiredItems()) {
            int count = iws.getCount();
            for (ItemStack it : player.inventory.mainInventory) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0)
                return false;
        }
        // then really consume item
        List<ItemStack> ret = new ArrayList<>();
        for (IngredientWithSize iws : research.getRequiredItems()) {
            int count = iws.getCount();
            for (ItemStack it : player.inventory.mainInventory) {
                if (iws.testIgnoringSize(it)) {
                    int redcount = Math.min(count, it.getCount());
                    ret.add(it.split(redcount));
                    count -= redcount;
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {// wrong, revert.
                for (ItemStack it : ret)
                    FHUtils.giveItem(player, it);
                return false;
            }
        }
        setActive();
        return true;
    }

    public void setActive() {
        if (active)
            return;
        active = true;
        sendProgressPacket();
    }

    /**
     * Current research progress
     *
     * @return 0.0F-1.0F fraction
     */
    public float getProgress() {
        return getTotalCommitted() * 1f / getResearch().getRequiredPoints();
    }

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
