package com.teammoeg.frostedheart.common;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.blocks.stone.CokeOvenTileEntity;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IEInventoryHandler;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GeneratorTileEntity extends MultiblockPartTileEntity<GeneratorTileEntity> implements IIEInventory,
        IEBlockInterfaces.IActiveState, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, IEBlockInterfaces.IBlockBounds {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    public int process = 0;
    public int processMax = 0;
    private NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    public GeneratorTileEntity.GeneratorData guiData = new GeneratorTileEntity.GeneratorData();

    public GeneratorTileEntity() {
        super(FHMultiblocks.GENERATOR, FHTileTypes.GENERATOR.get(), false);
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
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);

        if (!descPacket) {
            ItemStackHelper.loadAllItems(nbt, inventory);
            process = nbt.getInt("process");
            processMax = nbt.getInt("processMax");
        }
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);

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

    LazyOptional<IItemHandler> invHandler = registerConstantCap(
            new IEInventoryHandler(4, this, 0, new boolean[]{true, false, true, false},
                    new boolean[]{false, true, false, true})
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
                inventory.get(OUTPUT_SLOT).getCount() + recipe.output.getCount() <= getSlotLimit(OUTPUT_SLOT)))
            return recipe;
        return null;
    }

    @Override
    public void tick() {
        checkForNeedlessTicking();
        if (!world.isRemote && formed && !isDummy()) {
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
                        Utils.modifyInvStackSize(inventory, INPUT_SLOT, -recipe.input.getCount());
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
                // scan 3x3x3
                for (int x = 0; x < 3; ++x)
                    for (int y = 0; y < 3; ++y)
                        for (int z = 0; z < 3; ++z) {
                            BlockPos actualPos = getBlockPosForPos(new BlockPos(x, y, z));
                            TileEntity te = Utils.getExistingTileEntity(world, actualPos);
                            if (te instanceof GeneratorTileEntity)
                                ((GeneratorTileEntity) te).setActive(activeAfterTick);
                        }
            }
        }

    }
}
