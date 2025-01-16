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

package com.teammoeg.frostedheart.content.research;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.chorda.util.CRegistries;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.chorda.team.TeamDataClosure;
import com.teammoeg.chorda.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.research.recipe.InspireRecipe;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.api.ResearchDataAPI;
import com.teammoeg.frostedheart.content.research.blocks.RubbingTool;
import com.teammoeg.frostedheart.content.research.data.TeamResearchData;
import com.teammoeg.frostedheart.content.research.inspire.EnergyCore;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.clues.Clue;
import com.teammoeg.frostedheart.content.research.research.clues.ClueClosure;
import com.teammoeg.frostedheart.content.research.research.clues.ItemClue;
import com.teammoeg.frostedheart.content.research.research.clues.KillClue;
import com.teammoeg.frostedheart.content.research.research.clues.MinigameClue;
import com.teammoeg.frostedheart.content.research.research.clues.TickListenerClue;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.utility.OptionalLazy;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.PacketDistributor;

public class ResearchListeners {
    public static class BlockUnlockList extends UnlockList<Block> {
        public BlockUnlockList() {
            super();
        }

        public BlockUnlockList(ListTag nbt) {
            super(nbt);
        }

        @Override
        public Block getObject(String s) {
            return CRegistries.getBlock(new ResourceLocation(s));
        }

        @Override
        public String getString(Block item) {
            return CRegistries.getRegistryName(item).toString();
        }
    }

    public static class CategoryUnlockList extends UnlockList<ResourceLocation> {
        public CategoryUnlockList() {
            super();
        }

        public CategoryUnlockList(ListTag nbt) {
            super(nbt);
        }

        @Override
        public ResourceLocation getObject(String s) {
            return new ResourceLocation(s);
        }

        @Override
        public String getString(ResourceLocation item) {
            return item.toString();
        }

    }

    private static class ListenerInfo<T extends Clue> {
    	ClueClosure<T> listener;
        List<UUID> trigger;

        public ListenerInfo(ClueClosure<T> listener, UUID first) {
            super();
            this.listener = listener;
            if (first != null) {
                trigger = new ArrayList<>();
                trigger.add(first);
            }
        }

        public boolean add(UUID t) {
            if (trigger == null) return false;
            return trigger.add(t);
        }

        public ClueClosure<T> getListener() {
            return listener;
        }

        public boolean remove(UUID t) {
            if (trigger == null) return false;
            return trigger.remove(t);
        }

        public boolean shouldCall(UUID t2) {
            if (trigger == null) return true;
            for (UUID t : trigger)
                if (t.equals(t2))
                    return true;
            return false;
        }
    }

    public static class ListenerList<T extends Clue> extends ArrayList<ListenerInfo<T>> {

        /**
         *
         */
        private static final long serialVersionUID = -5579427246923453321L;

        public boolean add(ClueClosure<T> c, UUID t) {
            if (t != null) {
                for (ListenerInfo<T> cl : this) {
                    if (cl.getListener().equals(c))
                        return cl.add(t);
                }
            } else
                this.removeIf(cl -> cl.getListener() == c);
            return super.add(new ListenerInfo<>(c, t));
        }

        public void call(UUID t, Consumer<ClueClosure<T>> c) {
            for (ListenerInfo<T> cl : this) {
                if (cl.shouldCall(t))
                    c.accept(cl.getListener());
            }
        }
        public void call(UUID t, Predicate<ClueClosure<T>> c) {
            for (ListenerInfo<T> cl : this) {
                if (cl.shouldCall(t)) {
                	
                    if(c.test(cl.getListener()))
                    	cl.remove(t);
                }
            }
        }
        public boolean remove(ClueClosure<T> c, UUID t) {
            if (t != null)
                for (ListenerInfo<T> cl : this) {
                    if (cl.getListener().equals(c))
                        return cl.remove(t);
                }
            else
                this.removeIf(cl -> cl.getListener().equals(c));
            return false;
        }
    }

    public static class MultiblockUnlockList extends UnlockList<IMultiblock> {

        public MultiblockUnlockList() {
            super();
        }

        public MultiblockUnlockList(ListTag nbt) {
            super(nbt);
        }

        @Override
        public IMultiblock getObject(String s) {
            return MultiblockHandler.getByUniqueName(new ResourceLocation(s));
        }

        @Override
        public String getString(IMultiblock item) {
            return item.getUniqueName().toString();
        }


    }

    public static class RecipeUnlockList extends UnlockList<Recipe<?>> {

        public RecipeUnlockList() {
            super();
        }

        public RecipeUnlockList(ListTag nbt) {
            super(nbt);
        }

        @Override
        public Recipe<?> getObject(String s) {
            return CTeamDataManager.getRecipeManager().byKey(new ResourceLocation(s)).orElse(null);
        }

        @Override
        public String getString(Recipe<?> item) {
            return item.getId().toString();
        }

    }

    public static RecipeUnlockList recipe = new RecipeUnlockList();
    public static MultiblockUnlockList multiblock = new MultiblockUnlockList();
    public static BlockUnlockList block = new BlockUnlockList();
    public static CategoryUnlockList categories = new CategoryUnlockList();
    private static ListenerList<TickListenerClue> tickClues = new ListenerList<>();
    private static ListenerList<KillClue> killClues = new ListenerList<>();
    public static UUID te;

    @OnlyIn(Dist.CLIENT)
    public static boolean canExamine(ItemStack i) {
        if (i.isEmpty()) return false;
        for (InspireRecipe ir : CUtils.filterRecipes(null, InspireRecipe.TYPE)) {
            if (ir.item.test(i)) {
                return true;
            }
        }
        return true;
    }

    public static boolean canUseBlock(Player player, Block b) {
        if (block.has(b)) {
            if (player instanceof FakePlayer) return false;
            if (player.getCommandSenderWorld().isClientSide)
                return ClientResearchDataAPI.getData().get().block.has(b);
            return ResearchDataAPI.getData(player).get().block.has(b);
        }
        return true;

    }

    public static boolean canUseRecipe(Recipe<?> r) {
        if (recipe.has(r)) {
            return ClientResearchDataAPI.getData().get().crafting.has(r);
        }
        return true;
    }

    public static boolean canUseRecipe(Player s, Recipe<?> r) {
        if (s == null)
            return canUseRecipe(r);
        if (recipe.has(r)) {
            if (s.getCommandSenderWorld().isClientSide)
                return ClientResearchDataAPI.getData().get().crafting.has(r);
            return ResearchDataAPI.getData(s).get().crafting.has(r);
        }
        return true;
    }

    public static boolean canUseRecipe(UUID team, Recipe<?> r) {
        if (recipe.has(r)) {
            if (team == null) return false;
            return ResearchDataAPI.getData(team).map(t->t.get().crafting.has(r)).orElse(false) ;
        }
        return true;
    }

    public static boolean commitGameLevel(ServerPlayer s, int lvl) {
    	TeamDataClosure<TeamResearchData> data=ResearchDataAPI.getData(s);
        OptionalLazy<Research> cur = data.get().getCurrentResearch();
        if (cur.isPresent()) {
            Research rs = cur.orElse(null);
            if (rs != null) {
                for (Clue cl : rs.getClues()) {
                    if (data.get().isClueCompleted(rs,cl)) continue;
                    if (cl instanceof MinigameClue) {
                        if (((MinigameClue) cl).getLevel() <= lvl) {
                            data.get().setClueCompleted(data.team(),rs,cl, true);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static int fetchGameLevel() {
        TeamDataClosure<TeamResearchData> trd = ClientResearchDataAPI.getData();
        OptionalLazy<Research> cur = trd.get().getCurrentResearch();
        if (cur.isPresent()) {
            Research rs = cur.orElse(null);
            if (rs != null) {
                for (Clue cl : rs.getClues()) {
                    if (trd.get().isClueCompleted(rs,cl)) continue;
                    if (cl instanceof MinigameClue) {
                        return ((MinigameClue) cl).getLevel();
                    }
                }
            }
        }
        return -1;
    }

    public static int fetchGameLevel(ServerPlayer s) {
    	TeamDataHolder data= CTeamDataManager.get(s);
        TeamResearchData trd = data.getData(FHSpecialDataTypes.RESEARCH_DATA);
        OptionalLazy<Research> cur = trd.getCurrentResearch();
        if (cur.isPresent()) {
            Research rs = cur.orElse(null);
            if (rs != null) {
                for (Clue cl : rs.getClues()) {
                    if (trd.isClueCompleted(rs,cl)) continue;
                    if (cl instanceof MinigameClue mgc) {
                        return mgc.getLevel();
                    }
                }
            }
        }
        return -1;
    }

    public static ListenerList<KillClue> getKillClues() {
        return killClues;
    }

    public static ListenerList<TickListenerClue> getTickClues() {
        return tickClues;
    }

    public static void kill(ServerPlayer s, LivingEntity e) {
    	TeamDataClosure<TeamResearchData> data=ResearchDataAPI.getData(s);
        killClues.call(data.getId(), c -> {
        	
        	if(data.get().isClueCompleted(c.research(), c.clue())) {
        		data.get().setClueCompleted(data.team(), c.research(), c.clue(), true);
        		return true;
        	}
        	return false;
        });
    }

    public static void reload() {
        recipe.clear();
        multiblock.clear();
        block.clear();
        categories.clear();
        tickClues.clear();
        killClues.clear();
        te = null;
    }
    public static boolean hasMultiblock(UUID rid,IETemplateMultiblock mb) {
        if (ResearchListeners.multiblock.has(mb))
            if (rid==null) {
                if (!ClientResearchDataAPI.getData().get().building.has(mb)) {
                    return false;
                }
            } else {
                if (!ResearchDataAPI.getData(rid).map(t->t.get().building.has(mb)).orElse(false)) {
                    return false;
                }

            }
        return true;
    }
    @OnlyIn(Dist.CLIENT)
    public static void reloadEditor() {
        if (!Minecraft.getInstance().hasSingleplayerServer())
            FHResearch.editor = false;
    }

    public static void ServerReload() {
        if (CTeamDataManager.INSTANCE == null) return;
        FHMain.LOGGER.info("reloading research system");
        CTeamDataManager.INSTANCE.save();
        CTeamDataManager.INSTANCE.load();
        FHResearch.sendSyncPacket(PacketDistributor.ALL.noArg());
        CTeamDataManager.INSTANCE.forAllData(FHSpecialDataTypes.RESEARCH_DATA,TeamResearchData::sendUpdate);
    }

    public static ItemStack submitItem(ServerPlayer s, ItemStack i) {
        TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(s);
        OptionalLazy<Research> cur = trd.get().getCurrentResearch();
        
        if (cur.isPresent())
            for (Clue c : cur.get().getClues())
                if (c instanceof ItemClue ic)
                    i.shrink(ic.test(trd.team(), cur.get(), i));
        if (!i.isEmpty() && i.getCount() > 0) {
            if (i.getItem() instanceof RubbingTool && ResearchDataAPI.isResearchComplete(s, "rubbing_tool")) {
                if (RubbingTool.hasResearch(i)) {
                    int pts = RubbingTool.getPoint(i);
                    if (pts > 0) {
                        Research rs = FHResearch.getResearch(RubbingTool.getResearch(i));
                        if (rs != null && pts > 0) {
                            trd.get().doResearch(trd.team(), pts);
                        }
                    }
                    return new ItemStack(FHItems.rubbing_pad.get());
                }
                cur.ifPresent(r -> RubbingTool.setResearch(i, r.getId()));
            }
            for (InspireRecipe ir : CUtils.filterRecipes(s.level().getRecipeManager(), InspireRecipe.TYPE)) {
                if (ir.item.test(i)) {
                    i.shrink(1);
                    EnergyCore.addPersistentEnergy(s, ir.inspire);
                    
                    return i;
                }
            }

        }
        return i;
    }

    public static void tick(ServerPlayer s) {
    	TeamDataClosure<TeamResearchData> data=ResearchDataAPI.getData(s);
        tickClues.call(data.team().getId(), e -> {
        	if(e.clue().isCompleted(data.get(), s)) {
        		data.get().setClueCompleted(data.team(), e.research(), e.clue(), true);
        		return true;
        	}
        	return false;
        });
    }

    private ResearchListeners() {

    }
}
