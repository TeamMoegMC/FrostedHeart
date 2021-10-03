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

package com.teammoeg.frostedheart.content.charger;

import blusunrize.immersiveengineering.common.blocks.IEBaseTileEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.IIEInventory;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.steamenergy.IChargable;
import com.teammoeg.frostedheart.steamenergy.IConnectable;
import com.teammoeg.frostedheart.steamenergy.SteamEnergyNetwork;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.SmokingRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;

import java.util.List;

public class ChargerTileEntity extends IEBaseTileEntity implements
        IConnectable, IIEInventory, IEBlockInterfaces.IInteractionObjectIE, ITickableTileEntity , FHBlockInterfaces.IActiveState{
    NonNullList<ItemStack> inventory = NonNullList.withSize(2, ItemStack.EMPTY);
    public float power = 0;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    public ChargerTileEntity() {
        super(FHContent.FHTileTypes.CHARGER.get());
    }

    SteamEnergyNetwork network;
    Direction last;

    SteamEnergyNetwork getNetwork() {
        if (network != null) return network;
        if (last == null) return null;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(last));
        if (te instanceof EnergyNetworkProvider) {
            network = ((EnergyNetworkProvider) te).getNetwork();
        } else {
            disconnectAt(last);
        }
        return network;
    }

    public ActionResultType onClick(PlayerEntity pe, ItemStack is) {
        if (is != null) {
            if (world != null && world.isRemote) {
                ClientUtils.spawnSteamParticles(world, this.getPos());
            }
            Item it = is.getItem();
            if (it instanceof IChargable) {
                power -= ((IChargable) it).charge(is, power);
                return ActionResultType.SUCCESS;
            }
            ChargerRecipe cr = ChargerRecipe.findRecipe(is);
            if (cr != null) {
                if (power >= cr.cost && is.getCount() >= cr.input.getCount()) {
                    power -= cr.cost;
                    is.setCount(is.getCount() - cr.input.getCount());
                    ItemStack gain = cr.output.copy();

                    if (!pe.inventory.addItemStackToInventory(gain)) {
                        pe.getEntityWorld().addEntity(new ItemEntity(pe.getEntityWorld(), pe.getPosX(), pe.getPosY(), pe.getPosZ(), gain));
                    }
                }
            }
            if (power >= 100) {
                List<SmokingRecipe> irs = this.world.getRecipeManager().getRecipesForType(IRecipeType.SMOKING);
                for (SmokingRecipe sr : irs) {
                    if (sr.getIngredients().iterator().next().test(is)) {
                        //if(pe instanceof ServerPlayerEntity) {
                        power -= sr.getCookTime() / 20;
                        pe.giveExperiencePoints((int) sr.getExperience());
                        is.setCount(is.getCount() - 1);
                        ItemStack gain = sr.getRecipeOutput().copy();

                        if (!pe.inventory.addItemStackToInventory(gain)) {
                            pe.getEntityWorld().addEntity(new ItemEntity(pe.getEntityWorld(), pe.getPosX(), pe.getPosY(), pe.getPosZ(), gain));
                        }
                        markDirty();
                        this.markContainingBlockForUpdate(null);
                        //}
                        return ActionResultType.SUCCESS;
                    }
                }
            }
        }
        return ActionResultType.FAIL;
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        ItemStackHelper.loadAllItems(nbt, inventory);
        power = nbt.getFloat("power");
        if (nbt.contains("dir"))
            last = Direction.values()[nbt.getInt("dir")];
    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        ItemStackHelper.saveAllItems(nbt, inventory);
        nbt.putFloat("power", power);
        if (last != null)
            nbt.putInt("dir", last.ordinal());
    }

    @Override
    public boolean disconnectAt(Direction to) {
        if (last == to) {
            network = null;
            for (Direction d : Direction.values()) {
                if (d == to) continue;
                if (connectAt(d))
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean connectAt(Direction to) {
        Direction bd = this.getWorld().getBlockState(this.getPos()).get(BlockStateProperties.FACING);
        if (to != bd &&
                !((bd != Direction.DOWN && to == Direction.DOWN)
                        || (bd == Direction.UP && to == Direction.NORTH)
                        || (bd == Direction.DOWN && to == Direction.SOUTH))) return false;
        TileEntity te = Utils.getExistingTileEntity(this.getWorld(), this.getPos().offset(to));
        if (te instanceof EnergyNetworkProvider) {
            last = to;
            network = ((EnergyNetworkProvider) te).getNetwork();
            return true;
        } else
            disconnectAt(to);
        return false;
    }


    @Override
    public IInteractionObjectIE getGuiMaster() {
        return this;
    }

    @Override
    public boolean canUseGui(PlayerEntity player) {
        return true;
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public boolean isStackValid(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (slot == INPUT_SLOT)
            return (stack.getItem() instanceof IChargable);
        return false;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public void doGraphicalUpdates(int slot) {
    }

    public float getMaxPower() {
        return 20000F;
    }

    public Direction getDirection() {
        return this.getBlockState().get(BlockStateProperties.FACING);
    }

    @Override
    public void tick() {
        if (!world.isRemote) {
            SteamEnergyNetwork network = getNetwork();
            if (network != null) {
                float actual = network.drainHeat(Math.min(200, getMaxPower() - power));
                if (actual > 0) {
                    power += actual * 0.8;
                    this.setActive(true);
                    markDirty();
                    this.markContainingBlockForUpdate(null);
                } else
                    this.setActive(false);
            } else this.setActive(false);
        } else if (getIsActive()) {
            ClientUtils.spawnSteamParticles(this.getWorld(), pos);
            ClientUtils.spawnSteamParticles(this.getWorld(), pos);
            ClientUtils.spawnSteamParticles(this.getWorld(), pos);
            ClientUtils.spawnSteamParticles(this.getWorld(), pos);
        }
    }

	@Override
	public boolean canConnectAt(Direction dir) {
		Direction bd = this.getBlockState().get(BlockStateProperties.FACING);
        return dir == bd.getOpposite() || (bd != Direction.DOWN && dir == Direction.UP) || (bd == Direction.UP && dir == Direction.SOUTH) || (bd == Direction.DOWN && dir == Direction.NORTH);
	}

}
