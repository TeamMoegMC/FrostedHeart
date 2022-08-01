package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.MixerRecipe;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity.MultiblockProcessInMachine;
import blusunrize.immersiveengineering.common.blocks.metal.MixerTileEntity;
import blusunrize.immersiveengineering.common.blocks.metal.MixerTileEntity.MultiblockProcessMixer;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MultiblockProcessMixer.class)
public class MultiblockProcessMixerMixin extends MultiblockProcessInMachine<MixerRecipe> {

    public MultiblockProcessMixerMixin(MixerRecipe recipe, int... inputSlots) {
        super(recipe, inputSlots);
    }

    /**
     * @author khjxiaogu
     * @reason fix ie mixer issue
     */
    @Overwrite(remap = false)
    public void doProcessTick(PoweredMultiblockTileEntity<?, MixerRecipe> multiblock) {
        int timerStep = Math.max(this.maxTicks / this.recipe.fluidAmount, 1);
        if (timerStep != 0 && this.processTick % timerStep == 0) {
            int outamount = this.recipe.fluidAmount / maxTicks;
            int outleftover = this.recipe.fluidAmount % maxTicks;
            int inamount = this.recipe.fluidInput.getAmount() / maxTicks;
            int inleftover = this.recipe.fluidInput.getAmount() % maxTicks;
            if (outleftover > 0) {
                double distBetweenExtra = maxTicks / (double) outleftover;
                if (Math.floor(processTick / distBetweenExtra) != Math.floor((processTick - 1) / distBetweenExtra))
                    outamount++;
            }
            if (inleftover > 0) {
                double distBetweenExtra = maxTicks / (double) inleftover;
                if (Math.floor(processTick / distBetweenExtra) != Math.floor((processTick - 1) / distBetweenExtra))
                    inamount++;
            }
            MixerTileEntity mixer = (MixerTileEntity) multiblock;
            FluidTagInput drain = recipe.fluidInput.withAmount(inamount);
            if (mixer.tank.drain(drain, FluidAction.SIMULATE).getAmount() >= inamount) {
                FluidStack drained = mixer.tank.drain(drain, FluidAction.EXECUTE);
                NonNullList<ItemStack> components = NonNullList.withSize(this.inputSlots.length, ItemStack.EMPTY);
                for (int i = 0; i < components.size(); i++)
                    components.set(i, multiblock.getInventory().get(this.inputSlots[i]));
                FluidStack output = this.recipe.getFluidOutput(drained, components);
                FluidStack fs = Utils.copyFluidStackWithAmount(output, outamount, false);
                ((MixerTileEntity) multiblock).tank.fill(fs, FluidAction.EXECUTE);
            }
        }
        super.doProcessTick(multiblock);
    }
}
