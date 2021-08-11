package com.teammoeg.frostedheart.mixin.electrodynamics;

import electrodynamics.DeferredRegisters;
import electrodynamics.common.block.BlockGenericMachine;
import electrodynamics.common.block.BlockMachine;
import electrodynamics.common.block.subtype.SubtypeMachine;
import electrodynamics.common.network.ElectricityUtilities;
import electrodynamics.common.settings.Constants;
import electrodynamics.common.tile.TileCoalGenerator;
import electrodynamics.prefab.tile.GenericTileTicking;
import electrodynamics.prefab.tile.components.ComponentType;
import electrodynamics.prefab.tile.components.type.ComponentDirection;
import electrodynamics.prefab.tile.components.type.ComponentInventory;
import electrodynamics.prefab.tile.components.type.ComponentTickable;
import electrodynamics.prefab.utilities.object.CachedTileOutput;
import electrodynamics.prefab.utilities.object.TargetValue;
import electrodynamics.prefab.utilities.object.TransferPack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileCoalGenerator.class)
public class TileCoalGeneratorMixin extends GenericTileTicking {
    @Shadow(remap = false)
    protected int burnTime;
    @Shadow(remap = false)
    protected CachedTileOutput output;
    @Shadow(remap = false)
    protected TargetValue heat;
    @Shadow(remap = false)
    protected TransferPack currentOutput;

    protected TileCoalGeneratorMixin(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    /**
     * @author yuesha-yc
     * @reason change burn time
     */
    @Overwrite(remap = false)
    protected void tickServer(ComponentTickable tickable) {
        ComponentDirection direction = getComponent(ComponentType.Direction);
        if (output == null) {
            output = new CachedTileOutput(world, new BlockPos(pos).offset(direction.getDirection().getOpposite()));
        }
        if (tickable.getTicks() % 20 == 0) {
            output.update();
        }
        ComponentInventory inv = getComponent(ComponentType.Inventory);
        if (burnTime <= 0 && !inv.getStackInSlot(0).isEmpty()) {
            burnTime = inv.getStackInSlot(0).getItem() == Items.COAL_BLOCK ? 400 * 9 : 400; // COAL_BURN_TIME = 400
            inv.decrStackSize(0, 1);
        }
        BlockMachine machine = (BlockMachine) getBlockState().getBlock();
        if (machine != null && world != null) {
            boolean update = false;
            if (machine.machine == SubtypeMachine.coalgenerator) {
                if (burnTime > 0) {
                    update = true;
                }
            } else {
                if (burnTime <= 0) {
                    update = true;
                }
            }
            if (update) {
                world.setBlockState(pos,
                        DeferredRegisters.SUBTYPEBLOCK_MAPPINGS.get(burnTime > 0 ? SubtypeMachine.coalgeneratorrunning : SubtypeMachine.coalgenerator)
                                .getDefaultState().with(BlockGenericMachine.FACING, getBlockState().get(BlockGenericMachine.FACING)),
                        3);
            }
        }
        if (heat.get() > 27 && output.valid()) {
            ElectricityUtilities.receivePower(output.getSafe(), direction.getDirection(), currentOutput, false);
        }
        heat.rangeParameterize(27, 3000, burnTime > 0 ? 3000 : 27, heat.get(), 600).flush();
        currentOutput = TransferPack.ampsVoltage(Constants.COALGENERATOR_MAX_OUTPUT.getAmps() * ((heat.get() - 27.0) / (3000.0 - 27.0)),
                Constants.COALGENERATOR_MAX_OUTPUT.getVoltage());
    }
}
