/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.climate.heatdevice.generator;

import java.util.Optional;

import com.teammoeg.frostedheart.base.menu.FHBaseContainer;
import com.teammoeg.frostedheart.base.team.FHTeamDataManager;
import com.teammoeg.frostedheart.util.FHContainerData;
import com.teammoeg.frostedheart.util.FHContainerData.FHDataSlot;
import com.teammoeg.frostedheart.compat.ie.FHMultiblockHelper;
import com.teammoeg.frostedheart.util.client.Point;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu.MultiblockMenuContext;
import blusunrize.immersiveengineering.common.gui.IESlot.NewOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public abstract class GeneratorContainer<R extends GeneratorState, T extends GeneratorLogic<T, R>> extends FHBaseContainer {
    public FHDataSlot<Integer> process = FHContainerData.SLOT_INT.create(this);
    public FHDataSlot<Integer> processMax = FHContainerData.SLOT_INT.create(this);
    public FHDataSlot<Float> overdrive = FHContainerData.SLOT_FIXED.create(this);
    public FHDataSlot<Float> power = FHContainerData.SLOT_FIXED.create(this);
    public FHDataSlot<Float> tempLevel = FHContainerData.SLOT_FIXED.create(this);
    public FHDataSlot<Float> rangeLevel = FHContainerData.SLOT_FIXED.create(this);
    public FHDataSlot<Integer> tempDegree = FHContainerData.SLOT_INT.create(this);
    public FHDataSlot<Integer> rangeBlock = FHContainerData.SLOT_INT.create(this);
    public FHDataSlot<Boolean> isBroken = FHContainerData.SLOT_BOOL.create(this);
    public FHDataSlot<Boolean> isWorking = FHContainerData.SLOT_BOOL.create(this);
    public FHDataSlot<Boolean> isOverdrive = FHContainerData.SLOT_BOOL.create(this);
    public FHDataSlot<BlockPos> pos = FHContainerData.SLOT_BLOCKPOS.create(this);

    public GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer, MultiblockMenuContext<R> ctx) {
        super(type, id, inventoryPlayer.player, 2);
        R state = ctx.mbContext().getState();
        BlockPos master=FHMultiblockHelper.getAbsoluteMaster(ctx.mbContext().getLevel());
        if (state.getOwner() == null) {
            state.setOwner(FHTeamDataManager.get(inventoryPlayer.player).getId());
            state.regist(inventoryPlayer.player.level(),master);
        }
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
            isWorking.bind(() -> data.isWorking, t -> {data.isWorking = t;System.out.println("set working "+t);});
            isOverdrive.bind(() -> data.isOverdrive, t -> data.isOverdrive = t);
            System.out.println(" binded ");
        });
        System.out.println(optdata);
        pos.bind(() -> ctx.clickedPos());
        this.validator = new Validator(ctx.clickedPos(), 8).and(ctx.mbContext().isValid());
        IItemHandler handler = state.getData(FHMultiblockHelper.getAbsoluteMaster(ctx.mbContext().getLevel())).map(t -> t.inventory).orElseGet(() -> new ItemStackHandler(2));
        createSlots(handler, inventoryPlayer);
    }

    public GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer) {
        super(type, id, inventoryPlayer.player, 2);
        createSlots(new ItemStackHandler(2), inventoryPlayer);
    }

    protected void createSlots(IItemHandler handler, Inventory inventoryPlayer) {
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

    public abstract Point getSlotIn();

    public abstract Point getSlotOut();

    public abstract int getTier();

    public abstract FluidTank getTank();
    @Override
    public void receiveMessage(short btn, int state) {
    	//System.out.println("id "+btn+" state "+state);
        switch (btn) {
            case 1:
                isWorking.setValue(state > 0);
                break;
            case 2:
                isOverdrive.setValue(state > 0);
                break;
        }
        
       /* if (message.contains("temperatureLevel", Tag.TAG_INT))
            setTemperatureLevel(message.getInt("temperatureLevel"));
        if (message.contains("rangeLevel", Tag.TAG_INT))
            setRangeLevel(message.getInt("rangeLevel"));*/
    }
}

