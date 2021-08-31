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

import blusunrize.immersiveengineering.api.utils.CapabilityReference;
import blusunrize.immersiveengineering.api.utils.DirectionalBlockPos;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import blusunrize.immersiveengineering.common.blocks.generic.MultiblockPartTileEntity;
import blusunrize.immersiveengineering.common.util.EnergyHelper;
import com.google.common.collect.ImmutableSet;
import com.teammoeg.frostedheart.content.FHMultiblocks;
import com.teammoeg.frostedheart.content.FHTileTypes;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SteamTurbineTileEntity extends MultiblockPartTileEntity<SteamTurbineTileEntity> implements IEBlockInterfaces.IBlockBounds {
    public FluidTank[] tanks = new FluidTank[]{new FluidTank(24 * FluidAttributes.BUCKET_VOLUME)};
    public boolean active = false;
    public static Fluid steam = ForgeRegistries.FLUIDS.getValue(new ResourceLocation("steampowered", "steam"));

    //public static Fluid steam = Fluids.WATER;
    public SteamTurbineTileEntity() {
        super(FHMultiblocks.STEAMTURBINE, FHTileTypes.STEAMTURBINE.get(), true);
    }

    @Override
    public void readCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.readCustomNBT(nbt, descPacket);
        tanks[0].readFromNBT(nbt.getCompound("tank0"));
        active = nbt.getBoolean("active");

    }

    @Override
    public void writeCustomNBT(CompoundNBT nbt, boolean descPacket) {
        super.writeCustomNBT(nbt, descPacket);
        nbt.put("tank0", tanks[0].writeToNBT(new CompoundNBT()));
        nbt.putBoolean("active", active);
    }

    @Nonnull
    @Override
    protected IFluidTank[] getAccessibleFluidTanks(Direction side) {
        SteamTurbineTileEntity master = master();
        if (master != null && (posInMultiblock.getZ() == 0 && posInMultiblock.getY() == 1 && posInMultiblock.getX() == 2)
                && (side == null || side == getFacing().getOpposite()))
            return master.tanks;
        return new FluidTank[0];
    }

    @Override
    public void tick() {
        checkForNeedlessTicking();

        if (!isRSDisabled() && !tanks[0].getFluid().isEmpty()) {
            List<IEnergyStorage> presentOutputs = outputs.stream()
                    .map(CapabilityReference::getNullable)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!presentOutputs.isEmpty() && EnergyHelper.distributeFlux(presentOutputs, 256, false) < 256) {
                tanks[0].drain(10, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    protected boolean canFillTankFrom(int iTank, Direction side, FluidStack resources) {
        return resources.getFluid() == steam;
    }

    @Override
    protected boolean canDrainTankFrom(int iTank, Direction side) {
        return false;
    }

    @Nonnull
    @Override
    public VoxelShape getBlockBounds(@Nullable ISelectionContext ctx) {
        return VoxelShapes.fullCube();
    }

    @Override
    public Set<BlockPos> getRedstonePos() {
        return ImmutableSet.of(
                new BlockPos(0, 1, 0)
        );
    }

    private final List<CapabilityReference<IEnergyStorage>> outputs = Arrays.asList(
            CapabilityReference.forTileEntityAt(this,
                    () -> new DirectionalBlockPos(this.getBlockPosForPos(new BlockPos(0, 1, 6)).add(0, 1, 0), Direction.DOWN),
                    CapabilityEnergy.ENERGY),
            CapabilityReference.forTileEntityAt(this,
                    () -> new DirectionalBlockPos(this.getBlockPosForPos(new BlockPos(2, 1, 6)).add(0, 1, 0), Direction.DOWN),
                    CapabilityEnergy.ENERGY)
    );
}