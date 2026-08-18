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

package com.teammoeg.frostedheart.content.robotics.logistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.teammoeg.frostedheart.content.robotics.logistics.data.ItemKey;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticChest;
import com.teammoeg.frostedheart.content.robotics.logistics.grid.LogisticInterfaceChestInGrid;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticPushTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTask;
import com.teammoeg.frostedheart.content.robotics.logistics.tasks.LogisticTaskKey;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.LazyOptional;

class LogisticNetworkTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void explicitlyQueuedPushTaskMovesSourceInventoryIntoStorage() {
		LogisticNetwork network=new LogisticNetwork(null,BlockPos.ZERO);
		LogisticInterfaceChestInGrid supplier=new LogisticInterfaceChestInGrid(null,new BlockPos(1,0,0));
		LogisticChest storage=new LogisticChest(null,new BlockPos(2,0,0));
		LazyOptional<LogisticInterfaceChestInGrid> supplierCap=LazyOptional.of(()->supplier);
		LazyOptional<LogisticChest> storageCap=LazyOptional.of(()->storage);
		network.getHub().addElement(supplierCap.cast());
		network.getHub().addElement(storageCap.cast());
		supplier.insertItem(0,new ItemStack(Items.STONE,32),false);

		LogisticTaskKey taskKey=new LogisticTaskKey(supplier.getPos(),0);
		network.addTask(taskKey,new LogisticPushTask(supplier.getPos(),supplierCap.cast(),0));
		for(int tick=0;tick<22;tick++)
			network.tick();

		assertTrue(supplier.getStackInSlot(0).isEmpty());
		assertEquals(32,storage.getAllItems().get(new ItemKey(new ItemStack(Items.STONE))).getTotalCount());
	}

	@Test
	void queuedTasksPrepareInFifoOrder() {
		LogisticNetwork network=new LogisticNetwork(null,BlockPos.ZERO);
		List<Integer> order=new ArrayList<>();
		for(int id=0;id<5;id++)
			network.addTask(new LogisticTaskKey(new BlockPos(id,0,0),0),new RecordingTask(id,order));

		network.tick();

		assertEquals(List.of(0,1,2,3,4),order);
	}

	@Test
	void canceledTaskReturnsItsCarriedStackToStorage() {
		LogisticNetwork network=new LogisticNetwork(null,BlockPos.ZERO);
		LogisticChest storage=new LogisticChest(null,new BlockPos(2,0,0));
		network.getHub().addElement(LazyOptional.<LogisticChest>of(()->storage).cast());
		BlockPos owner=new BlockPos(4,0,0);
		network.addTask(new LogisticTaskKey(owner,0),new CarryingTask(new ItemStack(Items.IRON_INGOT,7)));
		network.tick();

		network.cancelTasksAt(owner);

		assertEquals(7,storage.getAllItems().get(new ItemKey(new ItemStack(Items.IRON_INGOT))).getTotalCount());
		assertTrue(network.canAddTask(new LogisticTaskKey(owner,0)));
	}

	@Test
	void loadingWorkingTasksRestoresDeduplicationKeys() {
		LogisticTaskKey key=new LogisticTaskKey(new BlockPos(8,0,0),3);
		ItemStack carried=new ItemStack(Items.COPPER_INGOT,5);
		LogisticNetwork original=new LogisticNetwork(null,BlockPos.ZERO);
		original.working.add(new LogisticPushTask(key,7,BlockPos.ZERO,Optional.empty(),carried,new ItemKey(carried)));
		CompoundTag saved=new CompoundTag();
		original.save(saved);

		LogisticNetwork restored=new LogisticNetwork(null,BlockPos.ZERO);
		restored.load(saved);

		assertFalse(restored.canAddTask(key));
		assertEquals(1,restored.working.size());
	}

	@Test
	void sourceOnlyChestIsNeverSelectedAsPlacementTarget() {
		LogisticNetwork network=new LogisticNetwork(null,BlockPos.ZERO);
		LogisticInterfaceChestInGrid supplier=new LogisticInterfaceChestInGrid(null,new BlockPos(1,0,0));
		LogisticChest storage=new LogisticChest(null,new BlockPos(2,0,0));
		LazyOptional<LogisticInterfaceChestInGrid> supplierCap=LazyOptional.of(()->supplier);
		LazyOptional<LogisticChest> storageCap=LazyOptional.of(()->storage);
		network.getHub().addElement(supplierCap.cast());
		network.getHub().addElement(storageCap.cast());

		assertSame(storage,network.getHub().findGridForPlace(
			new ItemKey(new ItemStack(Items.STONE)),new ItemStack(Items.STONE)
		).grid().resolve().get());
	}

	@Test
	void periodicRevalidationRepairsChildAndHubCaches() {
		LogisticNetwork network=new LogisticNetwork(null,BlockPos.ZERO);
		LogisticChest storage=new LogisticChest(null,new BlockPos(2,0,0));
		network.getHub().addElement(LazyOptional.<LogisticChest>of(()->storage).cast());
		storage.getChest().setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,9));

		assertFalse(network.getHub().getAllItems().containsKey(new ItemKey(new ItemStack(Items.GOLD_INGOT))));
		for(int tick=0;tick<200;tick++)
			network.tick();

		assertEquals(9,network.getHub().getAllItems().get(new ItemKey(new ItemStack(Items.GOLD_INGOT))).getTotalCount());
	}

	private static final class RecordingTask extends LogisticTask {
		private final int id;
		private final List<Integer> order;

		private RecordingTask(int id,List<Integer> order) {
			this.id=id;
			this.order=order;
		}

		@Override
		public LogisticTask prepare(LogisticNetwork network) {
			order.add(id);
			return null;
		}

		@Override
		public LogisticTask work(LogisticNetwork network) {
			return null;
		}
	}

	private static final class CarryingTask extends LogisticTask {
		private ItemStack carried;

		private CarryingTask(ItemStack carried) {
			this.carried=carried;
		}

		@Override
		public LogisticTask prepare(LogisticNetwork network) {
			return this;
		}

		@Override
		public LogisticTask work(LogisticNetwork network) {
			return null;
		}

		@Override
		public ItemStack takeCarriedStack() {
			ItemStack result=carried;
			carried=ItemStack.EMPTY;
			return result;
		}
	}
}
