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

import electrodynamics.common.network.ElectricityUtilities;
import org.spongepowered.asm.mixin.Mixin;

@Deprecated
@Mixin(ElectricityUtilities.class)
public class ElectricityUtilitiesMixin {
//    @Inject(method = "receivePower", at = @At(value = "INVOKE", target = "Lelectrodynamics/common/network/ElectricityUtilities;isElectricReceiver(Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/util/Direction;)Z"), cancellable = true, remap = false)
//    private static void inject$forgeEnergyCompat(TileEntity tile, Direction direction, TransferPack transfer, boolean debug, CallbackInfoReturnable<TransferPack> cir) {
//        if (ElectricityUtilities.isElectricReceiver(tile, direction)) {
//            LazyOptional<IElectrodynamic> cap = tile.getCapability(CapabilityElectrodynamic.ELECTRODYNAMIC, direction);
//            if (cap.isPresent()) {
//                IElectrodynamic handler = cap.resolve().get();
//                cir.setReturnValue(handler.receivePower(transfer, debug));
//            }
//            LazyOptional<IEnergyStorage> cap2 = tile.getCapability(CapabilityEnergy.ENERGY, direction);
//            if (cap2.isPresent()) {
//                IEnergyStorage handler = cap2.resolve().get();
//                double forgeEnergy = transfer.getJoules() / 4f; // Reduce Ele joule by a factor of 4. It is too much for our modpack.
//                TransferPack returner = TransferPack.joulesVoltage(handler.receiveEnergy((int) Math.min(Integer.MAX_VALUE, forgeEnergy), debug), transfer.getVoltage());
//                World world = tile.getWorld();
//                BlockPos pos = tile.getPos();
//                if (transfer.getVoltage() > CapabilityElectrodynamic.DEFAULT_VOLTAGE && world != null) {
//                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
//                    world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(),
//                            (float) Math.log10(10 + transfer.getVoltage() / CapabilityElectrodynamic.DEFAULT_VOLTAGE), Explosion.Mode.DESTROY);
//                }
//                cir.setReturnValue(returner);
//            }
//        }
//        cir.setReturnValue(TransferPack.EMPTY);
//    }
}
