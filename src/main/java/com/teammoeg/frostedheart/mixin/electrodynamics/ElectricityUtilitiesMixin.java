package com.teammoeg.frostedheart.mixin.electrodynamics;

import electrodynamics.api.electricity.CapabilityElectrodynamic;
import electrodynamics.api.electricity.IElectrodynamic;
import electrodynamics.common.network.ElectricityUtilities;
import electrodynamics.prefab.utilities.object.TransferPack;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
