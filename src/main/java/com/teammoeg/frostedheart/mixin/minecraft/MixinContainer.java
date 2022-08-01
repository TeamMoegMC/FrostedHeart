package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

//fix a dupe bug made by careless bugjang developers
//Hope this would work
@Mixin(Container.class)
public class MixinContainer {
    @Shadow
    private List<Slot> inventorySlots;

    @Inject(at = @At("HEAD"), method = "func_241440_b_", cancellable = true, require = 1)
    private void fh$slotSwapPending(int p_241440_1_, int p_241440_2_, ClickType p_241440_3_, PlayerEntity p_241440_4_, CallbackInfoReturnable<ItemStack> cbi) {
        if (p_241440_3_ == ClickType.SWAP) {
            Slot slot2 = null;
            for (Slot slot : this.inventorySlots) {
                if (slot.getSlotIndex() == p_241440_2_ && slot.inventory instanceof PlayerInventory) {
                    slot2 = slot;
                    break;
                }
            }
            if (slot2 == null) return;
            if (!slot2.canTakeStack(p_241440_4_))
                cbi.setReturnValue(ItemStack.EMPTY);
        }
    }
}
