/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.tileentity;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.client.util.FHClientUtils;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import com.teammoeg.frostedheart.content.FHMultiblocks;
import com.teammoeg.frostedheart.content.FHTileTypes;
import com.teammoeg.frostedheart.recipe.GeneratorRecipe;
import com.teammoeg.frostedheart.state.FHBlockInterfaces;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class GeneratorTileEntity extends MultiblockPartTileEntity<GeneratorTileEntity> implements IIEInventory,
        FHBlockInterfaces.IActiveState, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, IEBlockInterfaces.IBlockBounds {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    public int temperatureLevel; //1: +10 , 2: +20 , 3: +30 , 4: +40
    public int rangeLevel; //1: 8, 2: 12, 3: 16, 4: 20
    public boolean rfSupported = false; //todo: future impl
    public boolean euSupported = false;
    public int process = 0;
    public int processMax = 0;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    public GeneratorTileEntity.GeneratorData guiData = new GeneratorTileEntity.GeneratorData();
    private boolean isWorking;
    private boolean isOverdrive;

    public GeneratorTileEntity(int temperatureLevelIn, int rangeLevelIn) {
        super(FHMultiblocks.GENERATOR, getSpecificGeneratorType(temperatureLevelIn, rangeLevelIn), false);
        temperatureLevel = temperatureLevelIn;
        rangeLevel = rangeLevelIn;
    }

    public int getActualRange() {
        return 8 + (getRangeLevel() - 1) * 4;
    }

    public int getActualTemp() {
        return getTemperatureLevel() * 10;
    }

    private static TileEntityType<GeneratorTileEntity> getSpecificGeneratorType(int tLevel, int rLevel) {
        if (rLevel == 1) {
            if (tLevel == 1) {
                return FHTileTypes.GENERATOR_T1.get();
            }
            if (tLevel == 2) {
                return FHTileTypes.GENERATOR_T1.get();
            }
            if (tLevel == 3) {
                return FHTileTypes.GENERATOR_T1.get();
            }
            if (tLevel == 4) {
                return FHTileTypes.GENERATOR_T1.get();
            } else {
                throw new IllegalArgumentException("Level must be within 1 - 4 integers");
            }
        } else {
            // todo: add rest levels
            return null;
        }
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx) {
        return VoxelShapes.fullCube();
    }

    @Override
    public boolean receiveClientEvent(int id, int arg) {
        if (id == 0)
            this.formed = arg == 1;
        markDirty();
        this.markContainingBlockForUpdate(null);
        return true;
    }

    @Override
    public void receiveMessageFromClient(CompoundNBT message) {
        super.receiveMessageFromClient(message);
        if (message.contains("isWorking", Constants.NBT.TAG_BYTE))
            setWorking(message.getBoolean("isWorking"));
        if (message.contains("isOverdrive", Constants.NBT.TAG_BYTE))
            setOverdrive(message.getBoolean("isOverdrive"));
        if (message.contains("temperatureLevel", Constants.NBT.TAG_INT))
            setTemperatureLevel(message.getInt("temperatureLevel"));
        if (message.contains("rangeLevel", Constants.NBT.TAG_INT))
            setRangeLevel(message.getInt("rangeLevel"));
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        setWorking(nbt.getBoolean("isWorking"));
        setOverdrive(nbt.getBoolean("isOverdrive"));
        setTemperatureLevel(nbt.getInt("temperatureLevel"));
        setRangeLevel(nbt.getInt("rangeLevel"));
        if (!descPacket) {
            ItemStackHelper.loadAllItems(nbt, inventory);
            process = nbt.getInt("process");
            processMax = nbt.getInt("processMax");
        }
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putBoolean("isWorking", isWorking());
        nbt.putBoolean("isOverdrive", isOverdrive());
        nbt.putInt("temperatureLevel", getTemperatureLevel());
        nbt.putInt("rangeLevel", getRangeLevel());
        if (!descPacket) {
            nbt.putInt("process", process);
            nbt.putInt("processMax", processMax);
            ItemStackHelper.saveAllItems(nbt, inventory);
        }
    }

    @Nonnull
    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Nullable
    @Override
    public IEBlockInterfaces.IInteractionObjectIE getGuiMaster() {
        return master();
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return formed;
    }

    @Override
    public int[] getCurrentProcessesStep() {
        GeneratorTileEntity master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesStep();
        return new int[]{processMax - process};
    }

    @Override
    public int[] getCurrentProcessesMax() {
        GeneratorTileEntity master = master();
        if (master != this && master != null)
            return master.getCurrentProcessesMax();
        return new int[]{processMax};
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        GeneratorTileEntity master = master();
        if (master != null && master.formed && formed)
            return master.inventory;
        return this.inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return GeneratorRecipe.findRecipe(stack) != null;
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public void doGraphicalUpdates(int slot) {

    }

    @Override
    public void disassemble() {
        super.disassemble();
        ChunkData.resetTempToCube(world, getPos(),getActualRange());
    }

    LazyOptional<IItemHandler> invHandler = registerConstantCap(
            new IEInventoryHandler(2, this, 0, new boolean[]{true, false},
                    new boolean[]{false, true})
    );

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            GeneratorTileEntity master = master();
            if (master != null)
                return master.invHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    public class GeneratorData implements IIntArray {
        public static final int MAX_BURN_TIME = 0;
        public static final int BURN_TIME = 1;

        @Override
        public int get(int index) {
            switch (index) {
                case MAX_BURN_TIME:
                    return processMax;
                case BURN_TIME:
                    return process;
                default:
                    throw new IllegalArgumentException("Unknown index " + index);
            }
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case MAX_BURN_TIME:
                    processMax = value;
                    break;
                case BURN_TIME:
                    process = value;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown index " + index);
            }
        }

        @Override
        public int size() {
            return 2;
        }
    }

    @Nullable
    public GeneratorRecipe getRecipe() {
        if (inventory.get(INPUT_SLOT).isEmpty())
            return null;
        GeneratorRecipe recipe = GeneratorRecipe.findRecipe(inventory.get(INPUT_SLOT));
        if (recipe == null)
            return null;
        if (inventory.get(OUTPUT_SLOT).isEmpty() || (ItemStack.areItemsEqual(inventory.get(OUTPUT_SLOT), recipe.output) &&
                inventory.get(OUTPUT_SLOT).getCount() + recipe.output.getCount() <= getSlotLimit(OUTPUT_SLOT))) {
            return recipe;
        }
        return null;
    }

    @Override
    public void tick() {
        checkForNeedlessTicking();
        int actualTemp = getActualTemp();
        int actualRange = getActualRange();
        // spawn smoke particle
        if (world != null && world.isRemote && formed && !isDummy() && getIsActive()) {
            BlockPos blockpos = this.getPos();
            Random random = world.rand;
            if (random.nextFloat() < 0.4F) {
                for (int i = 0; i < random.nextInt(2) + 2; ++i) {
                    FHClientUtils.spawnSmokeParticles(world, blockpos);
                    FHClientUtils.spawnFireParticles(world, blockpos);
                }
            }
        }

        // logic
        if (!world.isRemote && formed && !isDummy() && !isWorking()) {
            setActive(false);
            process = 0;
            processMax = 0;
            ChunkData.resetTempToCube(world, getPos(),getActualRange());
        }
        if (!world.isRemote && formed && !isDummy() && isWorking()) {
            final boolean activeBeforeTick = getIsActive();
            // just finished process or during process
            if (process > 0) {
                if (inventory.get(INPUT_SLOT).isEmpty()) {
                    process = 0;
                    processMax = 0;
                }
                // during process
                else {
                    GeneratorRecipe recipe = getRecipe();
                    if (recipe == null || recipe.time != processMax) {
                        process = 0;
                        processMax = 0;
                        setActive(false);
                    } else
                        process--;
                }
                this.markContainingBlockForUpdate(null);
            }
            // process not started yet
            else {
                if (activeBeforeTick) {
                    GeneratorRecipe recipe = getRecipe();
                    if (recipe != null) {
                        int overdriveModifier = 1;
                        if (isOverdrive()) overdriveModifier = 4;
                        Utils.modifyInvStackSize(inventory, INPUT_SLOT, -recipe.input.getCount() * overdriveModifier);
                        if (!inventory.get(OUTPUT_SLOT).isEmpty())
                            inventory.get(OUTPUT_SLOT).grow(recipe.output.copy().getCount());
                        else if (inventory.get(OUTPUT_SLOT).isEmpty())
                            inventory.set(OUTPUT_SLOT, recipe.output.copy());
                    }
                    processMax = 0;
                    setActive(false);
                }
                GeneratorRecipe recipe = getRecipe();
                if (recipe != null) {
                    this.process = recipe.time;
                    this.processMax = process;
                    setActive(true);
                }
            }

            // set activity status
            final boolean activeAfterTick = getIsActive();
            if (activeBeforeTick != activeAfterTick) {
                this.markDirty();
                if (activeAfterTick) {
                    ChunkData.addTempToCube(world, getPos(), actualRange, (byte) actualTemp);
                } else {
                    ChunkData.resetTempToCube(world, getPos(),getActualRange());
                }
                // scan 3x4x3
                for (int x = 0; x < 3; ++x)
                    for (int y = 0; y < 4; ++y)
                        for (int z = 0; z < 3; ++z) {
                            BlockPos actualPos = getBlockPosForPos(new BlockPos(x, y, z));
                            TileEntity te = Utils.getExistingTileEntity(world, actualPos);
                            if (te instanceof GeneratorTileEntity)
                                ((GeneratorTileEntity) te).setActive(activeAfterTick);
                        }
            }
        }

    }

    public void setWorking(boolean working) {
        if (master() != null)
            master().isWorking = working;
    }

    public boolean isWorking() {
        if (master() != null)
            return master().isWorking;
        else return false;
    }

    public boolean isOverdrive() {
        if (master() != null)
            return master().isOverdrive;
        else return false;
    }

    public void setOverdrive(boolean overdrive) {
        if (master() != null) {
            master().isOverdrive = overdrive;
            if (overdrive) {
                setTemperatureLevel(getTemperatureLevel() * 2);
            } else {
                setTemperatureLevel(Math.max(1, getTemperatureLevel() / 2));
            }
        }
    }

    public void setTemperatureLevel(int temperatureLevel) {
        if (master() != null)
            master().temperatureLevel = temperatureLevel;
    }

    public int getTemperatureLevel() {
        if (master() != null)
            return master().temperatureLevel;
        else return 1;
    }

    public void setRangeLevel(int rangeLevel) {
        if (master() != null)
            master().rangeLevel = rangeLevel;
    }

    public int getRangeLevel() {
        if (master() != null)
            return master().rangeLevel;
        else return 1;
    }
}
