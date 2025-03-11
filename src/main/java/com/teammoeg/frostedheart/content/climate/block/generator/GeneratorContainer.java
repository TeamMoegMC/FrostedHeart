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

package com.teammoeg.frostedheart.content.climate.block.generator;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

import com.teammoeg.chorda.client.ui.Point;
import com.teammoeg.chorda.menu.CCustomMenuSlot;
import com.teammoeg.chorda.menu.CCustomMenuSlot.CDataSlot;
import com.teammoeg.chorda.menu.CMultiblockMenu;
import com.teammoeg.chorda.multiblock.CMultiblockHelper;
import com.teammoeg.chorda.util.IERecipeUtils;
import com.teammoeg.chorda.util.struct.LazyTickWorker;
import com.teammoeg.frostedresearch.ResearchListeners;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import blusunrize.immersiveengineering.common.gui.IESlot.NewOutput;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
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
	public CDataSlot<Boolean> hasResearch = CCustomMenuSlot.SLOT_BOOL.create(this);
	public CDataSlot<BlockPos> pos = CCustomMenuSlot.SLOT_BLOCKPOS.create(this);
	public CDataSlot<BitSet> material = CCustomMenuSlot.SLOT_VAR_BITSET.create(this);

	GeneratorLogic<T, R> tile;

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
		Optional<IMultiblockLogic<?>> otile = CMultiblockHelper.getMultiblockLogic(ctx.mbContext());
		tile = (GeneratorLogic<T, R>) otile.get();
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
			material.bind(() -> {

				if (data.isBroken)
					return IERecipeUtils.checkItemList(inventoryPlayer.player, tile.getRepairCost());
				else if (tile.getNextLevelMultiblock() != null) {
					List<IngredientWithSize> upgcost = tile.getUpgradeCost(inventoryPlayer.player.level(), ctx.mbContext());
					return IERecipeUtils.checkItemList(inventoryPlayer.player, upgcost);
				}
				return new BitSet();
			});
			// System.out.println(" binded ");
		});
		state.getTeamData().ifPresent(team -> {
			hasResearch.bind(() -> {
				if (tile.getNextLevelMultiblock() != null)
					return ResearchListeners.hasMultiblock(state.getOwner(), tile.getNextLevelMultiblock());
				else
					return false;
			});
		});

		// System.out.println(optdata);
		pos.setValue(master);

		IItemHandler handler = state.getData(master).map(t -> t.inventory).orElseGet(() -> null);
		createSlots(handler, inventoryPlayer);
		updateStructureState();
	}

	public void updateStructureState() {
		validStructure.setValue(tile.nextLevelHasValidStructure(Minecraft.getInstance().level, getMenuContext().mbContext()));
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
					return GeneratorData.isStackValid(inventoryPlayer.player.level(), 0, itemStack);
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
		}
	}
}
