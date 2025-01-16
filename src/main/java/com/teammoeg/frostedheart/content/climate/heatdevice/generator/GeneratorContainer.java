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

import com.teammoeg.chorda.menu.CContainer;
import com.teammoeg.chorda.util.utility.CContainerData;
import com.teammoeg.chorda.util.utility.CContainerData.CDataSlot;
import com.teammoeg.chorda.util.ie.CMultiblockHelper;
import com.teammoeg.chorda.util.client.Point;

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

public abstract class GeneratorContainer<R extends GeneratorState, T extends GeneratorLogic<T, R>> extends CContainer {
    public CDataSlot<Integer> process = CContainerData.SLOT_INT.create(this);
    public CDataSlot<Integer> processMax = CContainerData.SLOT_INT.create(this);
    public CDataSlot<Float> overdrive = CContainerData.SLOT_FIXED.create(this);
    public CDataSlot<Float> power = CContainerData.SLOT_FIXED.create(this);
    public CDataSlot<Float> tempLevel = CContainerData.SLOT_FIXED.create(this);
    public CDataSlot<Float> rangeLevel = CContainerData.SLOT_FIXED.create(this);
    public CDataSlot<Integer> tempDegree = CContainerData.SLOT_INT.create(this);
    public CDataSlot<Integer> rangeBlock = CContainerData.SLOT_INT.create(this);
    public CDataSlot<Boolean> isBroken = CContainerData.SLOT_BOOL.create(this);
    public CDataSlot<Boolean> isWorking = CContainerData.SLOT_BOOL.create(this);
    public CDataSlot<Boolean> isOverdrive = CContainerData.SLOT_BOOL.create(this);
    public CDataSlot<BlockPos> pos = CContainerData.SLOT_BLOCKPOS.create(this);

    public GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer, MultiblockMenuContext<R> ctx) {
        super(type, id, inventoryPlayer.player, 2);
        R state = ctx.mbContext().getState();
        BlockPos master= CMultiblockHelper.getAbsoluteMaster(ctx.mbContext().getLevel());
        /*if (state.getOwner() == null) {
            state.setOwner(CTeamDataManager.get(inventoryPlayer.player).getId());
            state.regist(inventoryPlayer.player.level(),master);
        }*/
        state.tryRegist(inventoryPlayer.player.level(),master);
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
            isWorking.bind(() -> data.isWorking, t -> {data.isWorking = t;});
            isOverdrive.bind(() -> data.isOverdrive, t -> data.isOverdrive = t);
            //System.out.println(" binded ");
        });
        //System.out.println(optdata);
        pos.bind(() -> ctx.clickedPos());
        this.validator = new Validator(ctx.clickedPos(), 8).and(ctx.mbContext().isValid());
        IItemHandler handler = state.getData(CMultiblockHelper.getAbsoluteMaster(ctx.mbContext().getLevel())).map(t -> t.inventory).orElseGet(() -> null);
        createSlots(handler, inventoryPlayer);
    }

    public GeneratorContainer(MenuType<?> type, int id, Inventory inventoryPlayer) {
        super(type, id, inventoryPlayer.player, 2);
        createSlots(new ItemStackHandler(2), inventoryPlayer);
    }

    protected void createSlots(IItemHandler handler, Inventory inventoryPlayer) {
        
        if(handler!=null) {
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

