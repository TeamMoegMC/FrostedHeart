package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Target the inner class using $ notation
@Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
public class ComposterBlockOutputContainerMixin {

    // Replace the check for bone meal with a check for biomass
    @Redirect(
            method = "canTakeItemThroughFace",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"
            )
    )
    private boolean redirectItemCheck(ItemStack stack, Item item) {
        // Instead of checking if the item is bone meal, check if it's biomass
        return stack.is(FHItems.BIOMASS.asItem());
    }
}