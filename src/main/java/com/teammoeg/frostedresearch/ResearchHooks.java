/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedresearch;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler;
import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.CDistHelper;
import com.teammoeg.chorda.util.struct.OptionalLazy;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.api.ResearchDataAPI;
import com.teammoeg.frostedresearch.blocks.RubbingTool;
import com.teammoeg.frostedresearch.data.TeamResearchData;
import com.teammoeg.frostedresearch.data.UnlockListType;
import com.teammoeg.frostedresearch.events.PopulateUnlockListEvent;
import com.teammoeg.frostedresearch.recipe.InspireRecipe;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.clues.*;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ResearchHooks {
	public final static Map<UnlockListType<?>,UnlockList<?>> locklists=new IdentityHashMap<>(10);

	public static UUID te;
	private static ListenerList<TickListenerClue> tickClues = new ListenerList<>();
	private static ListenerList<KillClue> killClues = new ListenerList<>();

	private ResearchHooks() {

	}
	@SuppressWarnings("unchecked")
	public static <T> UnlockList<T> getLockList(UnlockListType<T> name){
		return (UnlockList<T>) locklists.get(name);
	}
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
		if (getLockList(BLOCK_UNLOCK_LIST).has(b)) {
			if (player instanceof FakePlayer) return false;
			if (player.getCommandSenderWorld().isClientSide)
				return ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.BLOCK_UNLOCK_LIST).has(b);
			return ResearchDataAPI.getData(player).get().getUnlockList(ResearchHooks.BLOCK_UNLOCK_LIST).has(b);
		}
		return true;

	}

	public static boolean canUseRecipe(Recipe<?> r) {
		if (getLockList(RECIPE_UNLOCK_LIST).has(r)) {
			return ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.RECIPE_UNLOCK_LIST).has(r);
		}
		return true;
	}

	public static boolean canUseRecipe(Player s, Recipe<?> r) {
		if (s == null)
			return canUseRecipe(r);
		if (getLockList(RECIPE_UNLOCK_LIST).has(r)) {
			if (s.getCommandSenderWorld().isClientSide)
				return ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.RECIPE_UNLOCK_LIST).has(r);
			return ResearchDataAPI.getData(s).get().getUnlockList(ResearchHooks.RECIPE_UNLOCK_LIST).has(r);
		}
		return true;
	}

	public static boolean canUseRecipe(UUID team, Recipe<?> r) {
		if (getLockList(RECIPE_UNLOCK_LIST).has(r)) {
			if (team == null) return false;
			return ResearchDataAPI.getData(team).map(t -> t.get().getUnlockList(ResearchHooks.RECIPE_UNLOCK_LIST).has(r)).orElse(false);
		}
		return true;
	}

	public static boolean commitGameLevel(ServerPlayer s, int lvl) {
		TeamDataClosure<TeamResearchData> data = ResearchDataAPI.getData(s);
		Supplier<Research> cur = data.get().getCurrentResearch();
		Research rs = cur.get();
		if (rs != null) {
			for (Clue cl : rs.getClues()) {
				if (data.get().isClueCompleted(rs, cl)) continue;
				if (cl instanceof MinigameClue) {
					if (((MinigameClue) cl).getLevel() <= lvl) {
						data.get().setClueCompleted(data.team(), rs, cl, true);
						return true;
					}
				}
			}
		}

		return false;
	}

	@OnlyIn(Dist.CLIENT)
	public static int fetchGameLevel() {
		TeamDataClosure<TeamResearchData> trd = ClientResearchDataAPI.getData();
		Supplier<Research> cur = trd.get().getCurrentResearch();
		Research rs = cur.get();
		if (rs != null) {
			for (Clue cl : rs.getClues()) {
				if (trd.get().isClueCompleted(rs, cl)) continue;
				if (cl instanceof MinigameClue) {
					return ((MinigameClue) cl).getLevel();
				}
			}
		}

		return -1;
	}

	public static int fetchGameLevel(ServerPlayer s) {
		TeamDataHolder data = CTeamDataManager.get(s);
		TeamResearchData trd = data.getData(FRSpecialDataTypes.RESEARCH_DATA);
		Supplier<Research> cur = trd.getCurrentResearch();
		Research rs = cur.get();
		if (rs != null) {
			for (Clue cl : rs.getClues()) {
				if (trd.isClueCompleted(rs, cl)) continue;
				if (cl instanceof MinigameClue mgc) {
					return mgc.getLevel();
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
		TeamDataClosure<TeamResearchData> data = ResearchDataAPI.getData(s);
		killClues.call(data.getId(), c -> {

			if (data.get().isClueCompleted(c.research(), c.clue())) {
				data.get().setClueCompleted(data.team(), c.research(), c.clue(), true);
				return true;
			}
			return false;
		});
	}

	public static void reload() {
		locklists.clear();
		MinecraftForge.EVENT_BUS.post(new PopulateUnlockListEvent(locklists));
		tickClues.clear();
		killClues.clear();
		te = null;
	}

	public static boolean hasMultiblock(UUID rid, IETemplateMultiblock mb) {
		if (getLockList(MULTIBLOCK_UNLOCK_LIST).has(mb))
			if (rid == null) {
				if (!ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.MULTIBLOCK_UNLOCK_LIST).has(mb)) {
					return false;
				}
			} else {
				if (!ResearchDataAPI.getData(rid).map(t -> t.get().getUnlockList(ResearchHooks.MULTIBLOCK_UNLOCK_LIST).has(mb)).orElse(false)) {
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
		FRMain.LOGGER.info("reloading research system");
		CTeamDataManager.INSTANCE.save();
		CTeamDataManager.INSTANCE.load();
		FHResearch.sendSyncPacket(PacketDistributor.ALL.noArg());
		CTeamDataManager.INSTANCE.forAllData(FRSpecialDataTypes.RESEARCH_DATA, TeamResearchData::sendUpdate);
	}
	public static void onAreaVisited(ServerPlayer s,int index) {
		TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(s);
		trd.get().onAreaVisited(trd.team(), index);
	}

	public static ItemStack submitItem(ServerPlayer s, ItemStack i) {
		TeamDataClosure<TeamResearchData> trd = ResearchDataAPI.getData(s);
		Supplier<Research> curr = trd.get().getCurrentResearch();
		Research rs=curr.get();
		if (rs!=null)
			for (Clue c : rs.getClues())
				if (c instanceof ItemClue ic)
					i.shrink(ic.test(trd.team(), rs, i));
		if (!i.isEmpty() && i.getCount() > 0) {
			if (i.getItem() instanceof RubbingTool) {
				if (RubbingTool.hasResearch(i)) {
					int pts = RubbingTool.getPoint(i);
					if (pts > 0) {
						Research rssubmit = FHResearch.getResearch(RubbingTool.getResearch(i));
						if (rssubmit != null && pts > 0) {
							trd.get().doResearch(trd.team(),rssubmit, pts);
						}
					}
					return new ItemStack(FRContents.Items.rubbing_pad.get());
				}
				if(rs!=null)
					RubbingTool.setResearch(i, rs.getId());
			}
			for (InspireRecipe ir : CUtils.filterRecipes(s.level().getRecipeManager(), InspireRecipe.TYPE)) {
				if (ir.item.test(i)) {
					i.shrink(1);
					trd.get().addInsight(trd.team(), ir.inspire);

					return i;
				}
			}

		}
		return i;
	}

	public static void tick(ServerPlayer s) {
		TeamDataClosure<TeamResearchData> data = ResearchDataAPI.getData(s);
		tickClues.call(data.team().getId(), e -> {
			if (e.clue().isCompleted(data.get(), s)) {
				data.get().setClueCompleted(data.team(), e.research(), e.clue(), true);
				return true;
			}
			return false;
		});
	}
	public static final UnlockListType<Block> BLOCK_UNLOCK_LIST=new UnlockListType<>("block",Block.class);
	public static final UnlockListType<ResourceLocation> CATEGORY_UNLOCK_LIST=new UnlockListType<>("category",ResourceLocation.class);
	public static final UnlockListType<Recipe> RECIPE_UNLOCK_LIST=new UnlockListType<>("recipe",Recipe.class);
	public static final UnlockListType<IMultiblock> MULTIBLOCK_UNLOCK_LIST=new UnlockListType<>("multiblock",IMultiblock.class);
	public static class BlockUnlockList extends UnlockList<Block> {
		public static final Codec<BlockUnlockList> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.list(Codec.STRING).fieldOf("unlocked").forGetter(BlockUnlockList::getStrings)).apply(instance, BlockUnlockList::new));

		public BlockUnlockList() {
			super();
		}

		public BlockUnlockList(List<String> strings) {
			super(strings);
		}

		public BlockUnlockList(ListTag nbt) {
			super(nbt);
		}

		@Override
		public Block getObject(String s) {
			return CRegistryHelper.getBlock(new ResourceLocation(s));
		}

		@Override
		public String getString(Block item) {
			return CRegistryHelper.getRegistryName(item).toString();
		}
	}

	public static class CategoryUnlockList extends UnlockList<ResourceLocation> {
		public static final Codec<CategoryUnlockList> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.list(Codec.STRING).fieldOf("unlocked").forGetter(CategoryUnlockList::getStrings)).apply(instance, CategoryUnlockList::new));

		public CategoryUnlockList() {
			super();
		}

		public CategoryUnlockList(ListTag nbt) {
			super(nbt);
		}

		public CategoryUnlockList(List<String> strings) {
			super(strings);
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

					if (c.test(cl.getListener()))
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
		public static final Codec<MultiblockUnlockList> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.list(Codec.STRING).fieldOf("unlocked").forGetter(MultiblockUnlockList::getStrings)).apply(instance, MultiblockUnlockList::new));

		public MultiblockUnlockList() {
			super();
		}

		public MultiblockUnlockList(ListTag nbt) {
			super(nbt);
		}

		public MultiblockUnlockList(List<String> strings) {
			super(strings);
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

	public static class RecipeUnlockList extends UnlockList<Recipe> {
		public static final Codec<RecipeUnlockList> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.list(Codec.STRING).fieldOf("unlocked").forGetter(RecipeUnlockList::getStrings)).apply(instance, RecipeUnlockList::new));

		public RecipeUnlockList() {
			super();
		}

		public RecipeUnlockList(ListTag nbt) {
			super(nbt);
		}

		public RecipeUnlockList(List<String> strings) {
			super(strings);
		}

		@Override
		public Recipe getObject(String s) {
			return CDistHelper.getRecipeManager().byKey(new ResourceLocation(s)).orElse(null);
		}

		@Override
		public String getString(Recipe item) {
			return item.getId().toString();
		}

	}
}
