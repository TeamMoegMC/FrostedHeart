package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.content.curiosities.toolbox.ToolboxInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ToolboxInventory.class)
public class MixinToolboxInventory extends ItemStackHandler {
    ResourceLocation forbid = new ResourceLocation("immersiveengineering:forbidden_in_crates");

    @Inject(at = @At("HEAD"), method = "isItemValid", cancellable = true, remap = false)
    public void FH$AvoidForbid(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cbi) {
        if (stack.getItem().getTags().contains(forbid))
            cbi.setReturnValue(false);
    }
}
