/*
 * Copyright (c) 2022 TeamMoeg
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

package com.teammoeg.frostedheart.research;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.recipes.InspireRecipe;
import com.teammoeg.frostedheart.network.PacketHandler;
import com.teammoeg.frostedheart.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.research.clues.Clue;
import com.teammoeg.frostedheart.research.clues.ItemClue;
import com.teammoeg.frostedheart.research.clues.KillClue;
import com.teammoeg.frostedheart.research.clues.MinigameClue;
import com.teammoeg.frostedheart.research.clues.TickListenerClue;
import com.teammoeg.frostedheart.research.data.FHResearchDataManager;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.research.machines.RubbingTool;
import com.teammoeg.frostedheart.research.network.FHResearchRegistrtySyncPacket;
import com.teammoeg.frostedheart.research.research.Research;
import com.teammoeg.frostedheart.util.LazyOptional;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

public class ResearchListeners {
    public static class RecipeUnlockList extends UnlockList<IRecipe<?>> {

        public RecipeUnlockList() {
            super();
        }

        public RecipeUnlockList(ListNBT nbt) {
            super(nbt);
        }

        @Override
        public String getString(IRecipe<?> item) {
            return item.getId().toString();
        }

        @Override
        public IRecipe<?> getObject(String s) {
            return FHResearchDataManager.getRecipeManager().getRecipe(new ResourceLocation(s)).orElse(null);
        }

    }

    public static class MultiblockUnlockList extends UnlockList<IMultiblock> {

        public MultiblockUnlockList() {
            super();
        }

        public MultiblockUnlockList(ListNBT nbt) {
            super(nbt);
        }

        @Override
        public String getString(IMultiblock item) {
            return item.getUniqueName().toString();
        }

        @Override
        public IMultiblock getObject(String s) {
            return MultiblockHandler.getByUniqueName(new ResourceLocation(s));
        }


    }

    public static class BlockUnlockList extends UnlockList<Block> {
        public BlockUnlockList() {
            super();
        }

        public BlockUnlockList(ListNBT nbt) {
            super(nbt);
        }

        @Override
        public String getString(Block item) {
            return item.getRegistryName().toString();
        }

        @Override
        public Block getObject(String s) {
            return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(s));
        }
    }

    public static class CategoryUnlockList extends UnlockList<ResourceLocation> {
        public CategoryUnlockList() {
            super();
        }

        public CategoryUnlockList(ListNBT nbt) {
            super(nbt);
        }

        @Override
        public String getString(ResourceLocation item) {
            return item.toString();
        }

        @Override
        public ResourceLocation getObject(String s) {
            return new ResourceLocation(s);
        }

    }

    private static class ListenerInfo<T extends Clue> {
        T listener;
        List<Team> trigger;

        public ListenerInfo(T listener, Team first) {
            super();
            this.listener = listener;
            if (first != null) {
                trigger = new ArrayList<>();
                trigger.add(first);
            }
        }

        public boolean add(Team t) {
            if (trigger == null) return false;
            return trigger.add(t);
        }

        public boolean remove(Team t) {
            if (trigger == null) return false;
            return trigger.remove(t);
        }

        public boolean shouldCall(Team t2) {
            if (trigger == null) return true;
            for (Team t : trigger)
                if (t.equals(t2))
                    return true;
            return false;
        }

        public T getListener() {
            return listener;
        }
    }

    public static class ListenerList<T extends Clue> extends ArrayList<ListenerInfo<T>> {

        /**
         *
         */
        private static final long serialVersionUID = -5579427246923453321L;

        public boolean add(T c, Team t) {
            if (t != null) {
                for (ListenerInfo<T> cl : this) {
                    if (cl.getListener() == c)
                        return cl.add(t);
                }
            } else
                this.removeIf(cl -> cl.getListener() == c);
            return super.add(new ListenerInfo<T>(c, t));
        }

        public boolean remove(T c, Team t) {
            if (t != null)
                for (ListenerInfo<T> cl : this) {
                    if (cl.getListener() == c)
                        return cl.remove(t);
                }
            else
                this.removeIf(cl -> cl.getListener() == c);
            return false;
        }

        public void call(Team t, Consumer<T> c) {
            for (ListenerInfo<T> cl : this) {
                if (cl.shouldCall(t))
                    c.accept(cl.getListener());
            }
        }
    }

    public static RecipeUnlockList recipe = new RecipeUnlockList();
    public static MultiblockUnlockList multiblock = new MultiblockUnlockList();
    public static BlockUnlockList block = new BlockUnlockList();
    public static CategoryUnlockList categories = new CategoryUnlockList();
    private static ListenerList<TickListenerClue> tickClues = new ListenerList<>();
    private static ListenerList<KillClue> killClues = new ListenerList<>();
	public static UUID te;

    private ResearchListeners() {

    }

    public static void tick(ServerPlayerEntity s) {
        Team t = FTBTeamsAPI.getPlayerTeam(s);
        TeamResearchData trd = ResearchDataAPI.getData(s);
        tickClues.call(t, e -> e.tick(trd, s));
    }

    public static ListenerList<TickListenerClue> getTickClues() {
        return tickClues;
    }
    public static void reload() {
    	recipe.clear();
    	multiblock.clear();
    	block.clear();
    	categories.clear();
    	tickClues.clear();
    	killClues.clear();
    	te=null;
    }
    public static void ServerReload() {
    	if(FHResearchDataManager.INSTANCE==null)return;
    	FHMain.LOGGER.info("reloading research system");
    	FHResearchDataManager.INSTANCE.save();
    	FHResearchDataManager.INSTANCE.load();
    	FHResearchRegistrtySyncPacket packet=new FHResearchRegistrtySyncPacket();
    	PacketHandler.send(PacketDistributor.ALL.noArg(),packet);
    	FHResearchDataManager.INSTANCE.getAllData().forEach(t->t.sendUpdate());
    }
    @OnlyIn(Dist.CLIENT)
    public static void reloadEditor() {
    	if(!Minecraft.getInstance().isSingleplayer())
    		FHResearch.editor=false;
    }
    @OnlyIn(Dist.CLIENT)
    public static boolean canExamine(ItemStack i) {
    	if(i.isEmpty())return false;
    	for(InspireRecipe ir:InspireRecipe.recipes) {
    		if(ir.item.test(i)) {
    			return EnergyCore.hasExtraEnergy(ClientUtils.getPlayer(),ir.inspire);
    		}
    	}
    	return true;
    }
    public static ItemStack submitItem(ServerPlayerEntity s, ItemStack i) {
        TeamResearchData trd = ResearchDataAPI.getData(s);
        LazyOptional<Research> cur = trd.getCurrentResearch();
        if (cur.isPresent())
            for (Clue c : cur.orElse(null).getClues())
                if (c instanceof ItemClue)
                    i.shrink(((ItemClue) c).test(trd, i));
        if(!i.isEmpty()&&i.getCount()>0) {
        	if(i.getItem() instanceof RubbingTool&&ResearchDataAPI.isResearchComplete(s,"rubbing_tool")) {
        		if(RubbingTool.hasResearch(i)) {
        			int pts=RubbingTool.getPoint(i);
        			if(pts>0) {
	        			Research rs=FHResearch.getResearch(RubbingTool.getResearch(i)).get();
	        			if(rs!=null&&pts>0) {
	        				ResearchData rd=trd.getData(rs);
	        				rd.commitPoints(pts);
	        				rd.sendProgressPacket();
	        			}
        			}
        			return new ItemStack(FHItems.rubbing_pad);
        		}
				trd.getCurrentResearch().ifPresent(r->RubbingTool.setResearch(i,r.getLId()));
        	}
        	for(InspireRecipe ir:InspireRecipe.recipes) {
        		if(ir.item.test(i)) {
        			if(EnergyCore.useExtraEnergy(s,ir.inspire)) {
        				i.shrink(1);
        				EnergyCore.addPersistentEnergy(s, ir.inspire);
        			}
        			return i;
        		}
        	}
        	
        }
        return i;
    }

    public static int fetchGameLevel(ServerPlayerEntity s) {
        TeamResearchData trd = ResearchDataAPI.getData(s);
        LazyOptional<Research> cur = trd.getCurrentResearch();
        if (cur.isPresent()) {
            Research rs = cur.orElse(null);
            if (rs != null) {
                for (Clue cl : rs.getClues()) {
                    if (trd.isClueTriggered(cl)) continue;
                    if (cl instanceof MinigameClue) {
                        return ((MinigameClue) cl).getLevel();
                    }
                }
            }
        }
        return -1;
    }

    @OnlyIn(Dist.CLIENT)
    public static int fetchGameLevel() {
        TeamResearchData trd = ClientResearchDataAPI.getData();
        LazyOptional<Research> cur = trd.getCurrentResearch();
        if (cur.isPresent()) {
            Research rs = cur.orElse(null);
            if (rs != null) {
                for (Clue cl : rs.getClues()) {
                    if (trd.isClueTriggered(cl)) continue;
                    if (cl instanceof MinigameClue) {
                        return ((MinigameClue) cl).getLevel();
                    }
                }
            }
        }
        return -1;
    }

    public static boolean commitGameLevel(ServerPlayerEntity s, int lvl) {
        TeamResearchData trd = ResearchDataAPI.getData(s);
        LazyOptional<Research> cur = trd.getCurrentResearch();
        if (cur.isPresent()) {
            Research rs = cur.orElse(null);
            if (rs != null) {
                for (Clue cl : rs.getClues()) {
                    if (trd.isClueTriggered(cl)) continue;
                    if (cl instanceof MinigameClue) {
                        if (((MinigameClue) cl).getLevel() <= lvl) {
                            cl.setCompleted(trd, true);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("resource")
    public static boolean canUseRecipe(PlayerEntity s, IRecipe<?> r) {
        if (recipe.has(r)) {
            if (s.getEntityWorld().isRemote)
                return ClientResearchDataAPI.getData().crafting.has(r);
            return ResearchDataAPI.getData((ServerPlayerEntity) s).crafting.has(r);
        }
        return true;
    }

    public static boolean canUseRecipe(UUID team, IRecipe<?> r) {
        if (recipe.has(r)) {
            if (team == null) return false;
            return ResearchDataAPI.getData(team).crafting.has(r);
        }
        return true;
    }

    public static ListenerList<KillClue> getKillClues() {
        return killClues;
    }

    public static void kill(ServerPlayerEntity s, LivingEntity e) {
        Team t = FTBTeamsAPI.getPlayerTeam(s);
        TeamResearchData trd = ResearchDataAPI.getData(s);
        killClues.call(t, c -> c.isCompleted(trd, e));
    }

    public static boolean canUseRecipe(IRecipe<?> r) {
        if (recipe.has(r)) {
            return ClientResearchDataAPI.getData().crafting.has(r);
        }
        return true;
    }

    public static boolean canUseBlock(PlayerEntity player, Block b) {
        if (block.has(b)) {
        	if(player instanceof FakePlayer)return false;
            if (player.getEntityWorld().isRemote)
                return ClientResearchDataAPI.getData().block.has(b);
            return ResearchDataAPI.getData((ServerPlayerEntity) player).block.has(b);
        }
        return true;

    }
}
