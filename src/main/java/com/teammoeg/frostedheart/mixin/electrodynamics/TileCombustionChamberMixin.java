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

package com.teammoeg.frostedheart.mixin.electrodynamics;

import electrodynamics.DeferredRegisters;
import electrodynamics.common.network.ElectricityUtilities;
import electrodynamics.common.tile.TileCombustionChamber;
import electrodynamics.prefab.tile.GenericTileTicking;
import electrodynamics.prefab.tile.components.ComponentType;
import electrodynamics.prefab.tile.components.type.*;
import electrodynamics.prefab.utilities.object.CachedTileOutput;
import electrodynamics.prefab.utilities.object.TransferPack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileCombustionChamber.class)
public class TileCombustionChamberMixin extends GenericTileTicking {
    @Shadow(remap = false)
    private int burnTime;
    @Shadow(remap = false)
    public boolean running;
    @Shadow(remap = false)
    private CachedTileOutput output;

    protected TileCombustionChamberMixin(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    /**
     * @author yuesha-yc
     * @reason change burn time
     */
    @Overwrite(remap = false)
    protected void tickServer(ComponentTickable tickable) {
        ComponentDirection direction = getComponent(ComponentType.Direction);
        Direction facing = direction.getDirection();
        if (output == null) {
            output = new CachedTileOutput(world, pos.offset(facing.rotateY()));
        }
        if (tickable.getTicks() % 40 == 0) {
            output.update();
        }
        ComponentFluidHandler tank = getComponent(ComponentType.FluidHandler);
        ComponentElectrodynamic electro = getComponent(ComponentType.Electrodynamic);
        if (burnTime <= 0) {
            boolean shouldSend = !running;
            running = false;
            FluidStack stack = tank.getStackFromFluid(DeferredRegisters.fluidEthanol);
            if (stack.getAmount() > 0) {
                stack.setAmount(stack.getAmount() - 1);
                running = true;
                burnTime = 20; // TICKS_PER_MILIBUCKET
                shouldSend = true;
            }
            if (shouldSend) {
                this.<ComponentPacketHandler>getComponent(ComponentType.PacketHandler).sendGuiPacketToTracking();
            }
        } else {
            running = true;
            burnTime--;
        }
        if (running && burnTime > 0 && output.valid()) {
            ElectricityUtilities.receivePower(output.getSafe(), facing.rotateY().getOpposite(),
                    TransferPack.joulesVoltage(80, electro.getVoltage()), false); // JOULES_PER_TICK = 80
        }
    }

}