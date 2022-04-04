package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.research.clues.AbstractClue;
import com.teammoeg.frostedheart.research.events.ResearchStatusEvent;
import com.teammoeg.frostedheart.util.LazyOptional;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.ItemHandlerHelper;

public class ResearchData {
    boolean active;//is all items fulfilled?
    boolean finished;
    Supplier<Research> rs;
    int committed;//points committed
    final TeamResearchData parent;
    ArrayList<ItemStack> committedItems = new ArrayList<>();//items comitted
    UnlockList unlockedrecipes;
    
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
        Research r = rs.get();
        int currentProgress = committed;
        for (AbstractClue ac : r.getClues())
            if (ac.isCompleted(parent))
                currentProgress += r.getRequiredPoints() * ac.getResearchContribution();
        return currentProgress;
    }

    public void checkComplete() {
        if (finished) return;
        if (getTotalCommitted() == rs.get().getRequiredPoints()) {
            finished = true;
            this.announceCompletion();
        }
    }

    public void announceCompletion() {
        sendProgressPacket();
        MinecraftForge.EVENT_BUS.post(new ResearchStatusEvent(rs.get(), parent.team.get(), finished));
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void sendProgressPacket() {
        parent.getTeam().ifPresent(t -> rs.get().sendProgressPacket(t));
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

    public CompoundNBT serialize() {
        CompoundNBT cnbt = new CompoundNBT();
        cnbt.putInt("committed", committed);
        cnbt.putBoolean("active", active);
        cnbt.putBoolean("finished", finished);
        ListNBT items = new ListNBT();
        for (ItemStack is : committedItems) {
            items.add(is.serializeNBT());
        }
        cnbt.put("items", items);
        //cnbt.putInt("research",getResearch().getRId());
        return cnbt;

    }

    public void deserialize(CompoundNBT cn) {
        committed = cn.getInt("committed");
        active = cn.getBoolean("active");
        finished = cn.getBoolean("finished");
        ListNBT items = cn.getList("items", 10);
        items.stream().map(e -> ItemStack.read((CompoundNBT) e)).forEach(e -> committedItems.add(e));
        //rs=FHResearch.getResearch(cn.getInt("research"));
    }

	/**
	 * @return research finished
	 */
	public boolean isCompleted() {
        return finished;
    }

    public boolean isInProgress() {
        LazyOptional<Research> r = parent.getActiveResearch();
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

    /**
     * @param stackToCommit ItemStack to commit
     * @return
     */
    public List<ItemStack> commitItem(ItemStack stackToCommit) {
        Research research = rs.get();
        List<IngredientWithSize> requiredItems = new ArrayList<>(research.getRequiredItems());
        boolean alreadyExists = false;
        for (ItemStack committedStack : committedItems) {
            if (ItemStack.areItemsEqual(committedStack, stackToCommit)) {
                committedStack.setCount(committedStack.getCount() + stackToCommit.getCount());
                alreadyExists = true;
            }
        }
        if (!alreadyExists)
            committedItems.add(stackToCommit);

        List<ItemStack> cur = committedItems;//copy
        committedItems = new ArrayList<>();//replace

        List<ItemStack> ret = new ArrayList<>();
        for (Iterator<ItemStack> it0 = cur.iterator(); it0.hasNext(); ) {
            ItemStack cs = it0.next();
            for (Iterator<IngredientWithSize> it = requiredItems.iterator(); it.hasNext(); ) {
                IngredientWithSize iws = it.next();
                if (iws.testIgnoringSize(cs)) {
                    if (cs.getCount() <= iws.getCount()) {
                        committedItems.add(cs);
                        if (iws.getCount() == cs.getCount())
                            it.remove();
                    } else {
                        committedItems.add(ItemHandlerHelper.copyStackWithSize(cs, iws.getCount()));
                        ret.add(ItemHandlerHelper.copyStackWithSize(cs, cs.getCount() - iws.getCount()));//excess output
                        it.remove();
                    }
                }
            }
        }
        if (requiredItems.isEmpty())//all requirements fulfilled
            active = true;
        return ret;
    }

    /**
     * @return Already committed items
     */
    public List<ItemStack> getItemStored() {
        return Collections.unmodifiableList(committedItems);
    }

	/**
	 * Current research progress
	 * @return 0.0F-1.0F fraction
	 */
	public float getProgress() {
        return getTotalCommitted() / rs.get().getRequiredPoints();
    }
}
