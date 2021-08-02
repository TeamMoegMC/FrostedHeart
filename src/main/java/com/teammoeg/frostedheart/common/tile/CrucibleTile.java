package com.teammoeg.frostedheart.common.tile;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.items.IEItems;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.common.recipe.CrucibleRecipe;
import com.teammoeg.frostedheart.util.FHBlockInterfaces;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CrucibleTile extends MultiblockPartTileEntity<CrucibleTile> implements IIEInventory,
        FHBlockInterfaces.IActiveState, IEBlockInterfaces.IInteractionObjectIE, IEBlockInterfaces.IProcessTile, IEBlockInterfaces.IBlockBounds {

    private NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);
    public int temperature;
    public int burntime;
    public int process = 0;
    public int processMax = 0;

    public CrucibleTile() {
        super(FHMultiblocks.CRUCIBLE, FHTileTypes.CRUCIBLE.get(), false);
    }

    @Nonnull
    @Override
    public IFluidTank[] getAccessibleFluidTanks(Direction side) {
        return new IFluidTank[0];
    }

    @Override
    public boolean canFillTankFrom(int iTank, Direction side, FluidStack resource) {
        return false;
    }

    @Override
    public boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx) {
        return VoxelShapes.fullCube();
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
        return new int[0];
    }

    @Override
    public int[] getCurrentProcessesMax() {
        return new int[0];
    }

    @Nullable
    @Override
    public NonNullList<ItemStack> getInventory() {
        CrucibleTile master = master();
        if (master != null && master.formed && formed)
            return master.inventory;
        return this.inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
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
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        setTemperature(nbt.getInt("temperature"));
        setBurntime(nbt.getInt("burntime"));
        if (!descPacket) {
            ItemStackHelper.loadAllItems(nbt, inventory);
            process = nbt.getInt("process");
            processMax = nbt.getInt("processMax");
        }
    }

    public void setTemperature(int temperature) {
        if (master() != null)
            master().temperature = temperature;
    }

    public void setBurntime(int burntime) {
        if (master() != null)
            master().burntime = burntime;
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.putInt("temperature", temperature);
        nbt.putInt("burntime", burntime);
        if (!descPacket) {
            nbt.putInt("process", process);
            nbt.putInt("processMax", processMax);
            ItemStackHelper.saveAllItems(nbt, inventory);
        }
    }

    @Override
    public void tick() {
        checkForNeedlessTicking();
        if (world.isRemote && formed && !isDummy()) {
            CrucibleRecipe recipe = getRecipe();
            System.out.println(process);
            if (burntime > 0 && temperature < 1600) {
                burntime--;
                temperature++;
            }
            if (burntime <= 0 && temperature > 0) {
                temperature--;
            }
            if (burntime <= 0) {
                if (inventory.get(2).getItem() == IEItems.Ingredients.coalCoke) {
                    burntime = 400;
                    inventory.get(2).shrink(1);
                    this.markDirty();
                }
            }
            if (process > 0) {
                if (inventory.get(0).isEmpty()) {
                    process = 0;
                    processMax = 0;
                }
                // during process
                else {
                    if (recipe == null || recipe.time != processMax) {
                        process = 0;
                        processMax = 0;
                    } else {
                        process--;
                    }
                }
                this.markContainingBlockForUpdate(null);
            } else {
                if (recipe != null && processMax != 0) {
                    Utils.modifyInvStackSize(inventory, 0, -1);
                    if (!inventory.get(1).isEmpty())
                        inventory.get(1).grow(recipe.output.copy().getCount());
                    else if (inventory.get(1).isEmpty())
                        inventory.set(1, recipe.output.copy());
                    processMax = 0;
                }
                if (recipe != null) {
                    this.process = recipe.time;
                    this.processMax = process;
                }
            }
        }
    }

    @Nullable
    public CrucibleRecipe getRecipe() {
        if (inventory.get(0).isEmpty())
            return null;
        CrucibleRecipe recipe = CrucibleRecipe.findRecipe(inventory.get(0));
        if (recipe == null)
            return null;
        if (inventory.get(1).isEmpty() || (ItemStack.areItemsEqual(inventory.get(1), recipe.output) &&
                inventory.get(1).getCount() + recipe.output.getCount() <= getSlotLimit(1))) {
            return recipe;
        }
        return null;
    }
}