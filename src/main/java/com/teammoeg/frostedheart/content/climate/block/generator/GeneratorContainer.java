/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.teammoeg.chorda.math.Point;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.CMultiblockMenu;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.multiblock.MultiBlockAccess;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.chorda.util.IERecipeUtils;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedresearch.ResearchHooks;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.multiblocks.TemplateMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import blusunrize.immersiveengineering.common.gui.IESlot.NewOutput;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public abstract class GeneratorContainer<R extends GeneratorState, T extends GeneratorLogic<T, R>> extends CMultiblockMenu<R> {
	public CDataSlot<Integer> process = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Integer> processMax = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Float> overdrive = CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> power = CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> tempLevel = CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Float> rangeLevel = CCustomMenuSlot.SLOT_FIXED.create(this);
	public CDataSlot<Integer> tempDegree = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Integer> rangeBlock = CCustomMenuSlot.SLOT_INT.create(this);
	public CDataSlot<Boolean> isBroken = CCustomMenuSlot.SLOT_BOOL.create(this);
	public CDataSlot<Boolean> isWorking = CCustomMenuSlot.SLOT_BOOL.create(this);
	public CDataSlot<Boolean> isOverdrive = CCustomMenuSlot.SLOT_BOOL.create(this);
	public CDataSlot<Boolean> validStructure = CCustomMenuSlot.SLOT_BOOL.create(this);
	private final List<IngredientWithSize> repair = Arrays.asList(
			new IngredientWithSize(Ingredient.of(ItemTags.create(new ResourceLocation("forge", "ingots/copper"))), 32),
			new IngredientWithSize(Ingredient.of(ItemTags.create(new ResourceLocation("forge", "stone"))), 8));

	public GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer) {
		super(type, id, inventoryPlayer.player, 2);
		createSlots(new ItemStackHandler(2), inventoryPlayer);
	}
	public GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer, MultiblockMenuContext<R> ctx) {
		super(type, id, inventoryPlayer.player,ctx, 2);
		R state = ctx.mbContext().getState();
		BlockPos master = CMultiblockHelper.getAbsoluteMaster(ctx.mbContext());
		/*
		 * if (state.getOwner() == null) {
		 * state.setOwner(CTeamDataManager.get(inventoryPlayer.player).getId());
		 * state.regist(inventoryPlayer.player.level(),master); }
		 */
		state.tryRegist(inventoryPlayer.player.level(), master);
		Optional<GeneratorData> optdata = state.getData(master);

		optdata.ifPresent(data -> {
			process.bind(() -> data.process);
			processMax.bind(() -> data.processMax);
			overdrive.bind(() -> data.overdriveLevel * 1f / data.getMaxOverdrive());
			power.bind(() -> data.power);
			tempLevel.bind(() -> data.TLevel);
			rangeLevel.bind(() -> data.RLevel);
			tempDegree.bind(() -> data.getTempMod());
			rangeBlock.bind(() -> data.getRadius());
			isBroken.bind(() -> data.isBroken);
			isWorking.bind(() -> data.isWorking, t -> {
				data.isWorking = t;
			});
			isOverdrive.bind(() -> data.isOverdrive, t -> data.isOverdrive = t);
			/*material.bind(() -> {

				if (data.isBroken)
					return IERecipeUtils.checkItemList(inventoryPlayer.player, getRepairCost());
				else if (getNextLevelMultiblock() != null) {
					List<IngredientWithSize> upgcost = getUpgradeCost(inventoryPlayer.player.level(), ctx.mbContext());
					return IERecipeUtils.checkItemList(inventoryPlayer.player, upgcost);
				}
				return new BitSet();
			});*/
			// System.out.println(" binded ");
		});

		// System.out.println(optdata);

		IItemHandler handler = state.getData(master).map(t -> t.inventory).orElseGet(() -> null);
		createSlots(handler, inventoryPlayer);
		updateStructureState();
	}

	public void updateStructureState() {
		//System.out.println("update structure valid");
		validStructure.setValue(nextLevelHasValidStructure(getPlayer().level(), getMenuContext().mbContext()));
	}

	LazyTickWorker worker = new LazyTickWorker(10, () -> updateStructureState());

	@Override
	public void broadcastChanges() {
		worker.tick();
		super.broadcastChanges();
	}

	@Override
	public void broadcastFullState() {
		updateStructureState();
		super.broadcastFullState();
	}




	protected void createSlots(IItemHandler handler, Inventory inventoryPlayer) {

		if (handler != null) {
			Point in = getSlotIn();
			this.addSlot(new SlotItemHandler(handler, 0, in.getX(), in.getY()) {
				@Override
				public boolean mayPlace(ItemStack itemStack) {
					return GeneratorData.isStackValid( 0, itemStack);
				}
			});
			Point out = getSlotOut();
			this.addSlot(new NewOutput(handler, 1, out.getX(), out.getY()));
			super.addPlayerInventory(inventoryPlayer, 8, 140, 198);
		}
	}

	public abstract Point getSlotIn();

	public abstract Point getSlotOut();

	public abstract int getTier();

	public abstract IFluidTank getTank();

	@Override
	public void receiveMessage(short btn, int state) {
		switch (btn) {
		case 1:
			isWorking.setValue(state > 0);
			break;
		case 2:
			isOverdrive.setValue(state > 0);
			break;
		case 3:
			onUpgradeMaintainClicked(super.getMenuContext().mbContext(),(ServerPlayer) this.getPlayer());
		}
	}
	public final List<IngredientWithSize> getRepairCost() {
		return repair;
	}
	List<IngredientWithSize> upgrade;
	public List<IngredientWithSize> getUpgradeCost(Level level) {
		IETemplateMultiblock ietm = getNextLevelMultiblock();
		if (ietm != null) {
			if (upgrade == null) {
				List<StructureBlockInfo> structure = ietm.getStructure(level);
				NonNullList<ItemStack> materials = NonNullList.create();
				for (StructureBlockInfo info : structure) {
					// Skip dummy blocks in total
					if (info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE)
							&& info.state().getValue(IEProperties.MULTIBLOCKSLAVE))
						continue;
					ItemStack picked = Utils.getPickBlock(info.state());
					boolean added = false;
					for (ItemStack existing : materials)
						if (ItemStack.isSameItem(existing, picked)) {
							existing.grow(picked.getCount());
							added = true;
							break;
						}
					if (!added)
						materials.add(picked.copy());
				}

				upgrade = materials.stream().filter(Ingredient.of(FHBlocks.GENERATOR_CORE_T1.get()).negate())
						.map(IngredientWithSize::of).collect(Collectors.toList());
			}
			return upgrade;
		}
		return null;
	}
	List<ItemStack> price;
	public List<ItemStack> getPrice(Level level) {
		TemplateMultiblock ietm = getMultiblock();
		if (ietm != null) {
			if (price == null) {
				List<StructureBlockInfo> structure = ietm.getStructure(level);
				NonNullList<ItemStack> materials = NonNullList.create();
				for (StructureBlockInfo info : structure) {
					// Skip dummy blocks in total
					if (info.state().hasProperty(IEProperties.MULTIBLOCKSLAVE)
							&& info.state().getValue(IEProperties.MULTIBLOCKSLAVE))
						continue;
					ItemStack picked = Utils.getPickBlock(info.state());
					boolean added = false;
					for (ItemStack existing : materials)
						if (ItemStack.isSameItem(existing, picked)) {
							existing.grow(1);
							added = true;
							break;
						}
					if (!added)
						materials.add(picked.copy());
				}
				if (materials.isEmpty())
					return null;
				price = materials.stream().filter(Ingredient.of(FHBlocks.GENERATOR_CORE_T1.get()).negate())
						.collect(Collectors.toList());
			}
			return price;
		}
		return null;
	}

	protected abstract TemplateMultiblock getMultiblock();
	public abstract IETemplateMultiblock getNextLevelMultiblock();

	public boolean nextLevelHasValidStructure(Level level, IMultiblockContext<R> ctx) {
		IETemplateMultiblock ietm = getNextLevelMultiblock();
		MultiblockRegistration<?> curmb = CMultiblockHelper.getMultiblock(ctx);
		if (ietm == null)
			return true;
		Vec3i csize = curmb.size(level);
		BlockPos masterOrigin = curmb.masterPosInMB();
		Vec3i nsize = ietm.getSize(level);
		BlockPos masterOffset = ietm.getMasterFromOriginOffset().subtract(masterOrigin);
		BlockPos negMasterOffset = masterOrigin.subtract(ietm.getMasterFromOriginOffset());
		AABB aabb = new AABB(masterOffset, masterOffset.offset(csize));

		for (int x = 0; x < nsize.getX(); x++) {
			for (int y = 0; y < nsize.getY(); y++) {
				for (int z = 0; z < nsize.getZ(); z++) {
					if (aabb.contains(x, y, z))
						continue;
					BlockPos cpos = negMasterOffset.offset(x, y, z);
					Block blk=ctx.getLevel().getBlockState(cpos).getBlock();
					//System.out.println(cpos+":"+blk);
					if (blk != Blocks.AIR) {
						return false;
					}
				}
			}
		}
		return true;
	}
	/**
	 * Get the GeneratorData from context
	 */
	public Optional<GeneratorData> getData(IMultiblockContext<R> ctx) {
		return ctx.getState().getData(CMultiblockHelper.getAbsoluteMaster(ctx));
	}

	/**
	 * Upgrading Generator logic
	 */
	public void onUpgradeMaintainClicked(IMultiblockContext<R> ctx, ServerPlayer player) {
		if (getData(ctx).map(t -> t.isBroken).orElse(false)) {
			repairStructure(ctx, player);
		} else {
			upgradeStructure(ctx, player);
		}
	}

	public void upgradeStructure(IMultiblockContext<R> ctx, ServerPlayer entityplayer) {
		if (!nextLevelHasValidStructure(ctx.getLevel().getRawLevel(), ctx))
			return;
		if (!ResearchHooks.hasMultiblock(ctx.getState().getOwner(), getNextLevelMultiblock()))
			return;
		List<IngredientWithSize> upgradecost = getUpgradeCost(ctx.getLevel().getRawLevel());
		if (!IERecipeUtils.costItems(entityplayer, upgradecost))
			return;
		// System.out.println(upgradecost);

		for (ItemStack is : this.getPrice(ctx.getLevel().getRawLevel()))
			CUtils.giveItem(entityplayer, is.copy());
		BlockPos negMasterOffset = CMultiblockHelper.getMasterPos(ctx)
				.subtract(getNextLevelMultiblock().getMasterFromOriginOffset());
		Rotation rot = DirectionUtils.getRotationBetweenFacings(Direction.NORTH,
				ctx.getLevel().getOrientation().front());
		((MultiBlockAccess) getNextLevelMultiblock()).setUUID(ctx.getState().getOwner());
		((MultiBlockAccess) getNextLevelMultiblock()).callForm(ctx.getLevel().getRawLevel(),
				ctx.getLevel().toAbsolute(negMasterOffset), rot, Mirror.NONE,
				ctx.getLevel().getOrientation().front().getOpposite());
	}


	public void repairStructure(IMultiblockContext<R> ctx, ServerPlayer entityplayer) {
		if (!getData(ctx).map(t -> t.isBroken).orElse(false))
			return;
		if (!IERecipeUtils.costItems(entityplayer, getRepairCost()))
			return;
		getData(ctx).ifPresent(t -> {
			t.isBroken = false;
			t.overdriveLevel = 0;
		});

	}

}
