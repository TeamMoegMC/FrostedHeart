package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.effects.Effect;
import com.teammoeg.frostedheart.research.events.ResearchStatusEvent;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.LazyOptional;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;

public class ResearchData {
    boolean active;//is all items fulfilled?
    boolean finished;
    private Supplier<Research> rs;
    int committed;//points committed
    final TeamResearchData parent;
    public ResearchData(Supplier<Research> r, TeamResearchData parent) {
        this.rs = r;
        this.parent = parent;
    }

	/**
	 * @return Research points committed
	 */
	public int getCommitted() {
        return committed;
    }

    public int getTotalCommitted() {
        Research r = getResearch();
        int currentProgress = committed;
        for (Clue ac : r.getClues())
            if (ac.isCompleted(parent))
                currentProgress += r.getRequiredPoints() * ac.getResearchContribution();
        return currentProgress;
    }

    public void checkComplete() {
        if (finished) return;
        Research r=getResearch();
        Team t=parent.team.get();
        if (getTotalCommitted() >= r.getRequiredPoints()) {
            finished = true;
            this.announceCompletion();
            for(Effect e:r.getEffects())
            	parent.grantEffect(e);
            for(Clue c:r.getClues())
            	c.end(t);
        }
    }

    public void announceCompletion() {
        sendProgressPacket();
        MinecraftForge.EVENT_BUS.post(new ResearchStatusEvent(getResearch(), parent.team.get(), finished));
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void sendProgressPacket() {
        parent.getTeam().ifPresent(t -> getResearch().sendProgressPacket(t,this));
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
		pb.writeVarInt(committed);
		pb.writeBoolean(active);
		pb.writeBoolean(finished);
	}
	public void read(PacketBuffer pb) {
		committed=pb.readVarInt();
		active=pb.readBoolean();
		finished=pb.readBoolean();
	}
    public CompoundNBT serialize() {
        CompoundNBT cnbt = new CompoundNBT();
        cnbt.putInt("committed", committed);
        cnbt.putBoolean("active", active);
        cnbt.putBoolean("finished", finished);
        //cnbt.putInt("research",getResearch().getRId());
        return cnbt;

    }

    public void deserialize(CompoundNBT cn) {
        committed = cn.getInt("committed");
        active = cn.getBoolean("active");
        finished = cn.getBoolean("finished");
        //rs=FHResearch.getResearch(cn.getInt("research"));
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
            return r.resolve().get().equals(this.rs);
        }
        return false;
    }

    /**
     * @return whether all required items are committed and is ready to do research through clues
     */
    public boolean canResearch() {
        return active;
    }

	/**
	 * Add research points to current research
	 * @param points to commit
	 */
	public void doResearch(int points) {
        if (active) {
            committed += points;
            checkComplete();
        }
    }

    public boolean commitItem(ServerPlayerEntity player) {
        Research research = getResearch();
        //first do simple verify
        for(IngredientWithSize iws:research.getRequiredItems()) {
        	int count=iws.getCount();
        	for(ItemStack it:player.inventory.mainInventory) {
        		count-=it.getCount();
        		if(count<=0)break;
        	}
        	if(count>0)return false;
        }
        //then really consume item
        List<ItemStack> ret = new ArrayList<>();
        for(IngredientWithSize iws:research.getRequiredItems()) {
        	int count=iws.getCount();
        	for(ItemStack it:player.inventory.mainInventory) {
        		int redcount=Math.min(count, it.getCount());
        		ret.add(it.split(redcount));
        		count-=redcount;
        		if(count<=0)break;
        	}
        	if(count>0) {//wrong, revert.
        		for(ItemStack it:ret)
        			FHUtils.giveItem(player, it);
        		return false;
        	}
        }
        setActive();
        return true;
    }
    public void setActive() {
    	if(active)return;
    	active = true;
    	for(Clue c:getResearch().getClues())
    		c.start(parent.team.get());
    	sendProgressPacket();
    }
	/**
	 * Current research progress
	 * @return 0.0F-1.0F fraction
	 */
	public float getProgress() {
        return getTotalCommitted() / getResearch().getRequiredPoints();
    }
}
